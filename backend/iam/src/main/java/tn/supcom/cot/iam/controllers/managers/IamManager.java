package tn.supcom.cot.iam.controllers.managers;

import com.mongodb.MongoWriteException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.ConfigProvider;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;
import tn.supcom.cot.core.security.JwtService;
import tn.supcom.cot.core.security.JwtService.Claims;
import tn.supcom.cot.core.security.PasswordHasher;
import tn.supcom.cot.core.security.Role;
import tn.supcom.cot.core.security.Scopes;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Gestion des identités et émission des jetons (équivalent simplifié d'un serveur OAuth 2.0).
 * Flux supportés : grant_type=password et grant_type=refresh_token.
 */
@ApplicationScoped
public class IamManager {
    private static final Logger LOG = Logger.getLogger(IamManager.class.getName());
    private static final String DEFAULT_SECRET = JwtService.DEV_SECRET;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._@-]{3,50}");

    public record TokenResult(String accessToken, String refreshToken, long expiresIn, String scope) {}

    @Inject
    private IdentityRepository identities;
    @Inject
    private TenantRepository tenants;
    @Inject
    private GrantRepository grants;

    private JwtService jwt;
    private boolean enabled;
    /** Hash factice pour que "utilisateur inconnu" et "mauvais mot de passe" prennent le même temps. */
    private String dummyHash;

    @PostConstruct
    void init() {
        var config = ConfigProvider.getConfig();
        enabled = config.getOptionalValue("iam.enabled", Boolean.class).orElse(true);
        var secret = config.getOptionalValue("iam.jwt.secret", String.class).orElse(DEFAULT_SECRET);
        if (DEFAULT_SECRET.equals(secret)) {
            LOG.warning("IAM : clé JWT par défaut utilisée. Définir IAM_JWT_SECRET hors développement.");
        }
        jwt = new JwtService(secret,
                config.getOptionalValue("iam.jwt.issuer", String.class).orElse("mawqif"),
                Duration.ofSeconds(config.getOptionalValue("iam.access.ttl.seconds", Long.class).orElse(900L)),
                Duration.ofSeconds(config.getOptionalValue("iam.refresh.ttl.seconds", Long.class).orElse(604_800L)));
        dummyHash = PasswordHasher.hash("dummy-password");
    }

    /** Si false (iam.enabled=false), l'API reste ouverte comme avant l'ajout de l'IAM. */
    public boolean enabled() {
        return enabled;
    }

    // ---------- Jetons ----------

    public TokenResult passwordGrant(String clientId, String clientSecret, String username, String password, String requestedScope) {
        var tenant = authenticateClient(clientId, clientSecret);
        if (!tenant.supports("password")) throw new BadRequestException("unsupported_grant_type");
        var identity = username == null ? Optional.<Identity>empty() : identities.findByUsername(normalize(username));
        // Même travail de hachage que l'utilisateur existe ou non.
        boolean ok = PasswordHasher.verify(password == null ? "" : password, identity.map(Identity::getPasswordHash).orElse(dummyHash));
        if (identity.isEmpty() || !ok) throw new NotAuthorizedException("Identifiants invalides.", "Bearer");
        var who = identity.get();
        requireAllowedRole(tenant, who);

        var granted = intersect(requestedScope == null || requestedScope.isBlank() ? who.getProvidedScopes() : requestedScope,
                who.getProvidedScopes(), tenant.getAllowedScopes());
        if (granted.isEmpty()) throw new BadRequestException("invalid_scope");
        grants.save(Grant.of(tenant.getId(), who.getId(), Scopes.join(granted)));
        return issue(who, tenant, Scopes.join(granted));
    }

    public TokenResult refreshGrant(String clientId, String clientSecret, String refreshToken) {
        var tenant = authenticateClient(clientId, clientSecret);
        if (!tenant.supports("refresh_token")) throw new BadRequestException("unsupported_grant_type");
        var claims = jwt.parse(refreshToken, JwtService.REFRESH)
                .orElseThrow(() -> new NotAuthorizedException("Jeton de rafraîchissement invalide ou expiré.", "Bearer"));
        if (!tenant.getName().equals(claims.clientId())) {
            throw new NotAuthorizedException("Jeton émis pour un autre client.", "Bearer");
        }
        var who = identities.findById(claims.subject())
                .orElseThrow(() -> new NotAuthorizedException("Compte supprimé.", "Bearer"));
        requireAllowedRole(tenant, who);
        var granted = intersect(claims.scope(), who.getProvidedScopes(), tenant.getAllowedScopes());
        if (granted.isEmpty()) throw new BadRequestException("invalid_scope");
        return issue(who, tenant, Scopes.join(granted));
    }

    /** Vérifie un jeton d'accès (en-tête Authorization sans le préfixe "Bearer "). */
    public Optional<Claims> authenticate(String accessToken) {
        return jwt.parse(accessToken, JwtService.ACCESS);
    }

    /** Claims du jeton d'un en-tête "Authorization: Bearer ..." (vide si absent ou invalide). */
    public Optional<Claims> fromAuthorizationHeader(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        return authenticate(header.substring(7).trim());
    }

    private TokenResult issue(Identity who, Tenant tenant, String scope) {
        var access = jwt.issue(JwtService.ACCESS, who.getId(), who.getUsername(), who.getRoles(), scope, tenant.getName());
        var refresh = jwt.issue(JwtService.REFRESH, who.getId(), who.getUsername(), who.getRoles(), scope, tenant.getName());
        return new TokenResult(access, refresh, jwt.accessTtl().toSeconds(), scope);
    }

    private Tenant authenticateClient(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank()) throw new BadRequestException("client_id obligatoire.");
        var tenant = tenants.findByName(clientId).orElseThrow(() -> new NotAuthorizedException("Client inconnu.", "Bearer"));
        if (tenant.getSecretHash() != null && !PasswordHasher.verify(clientSecret, tenant.getSecretHash())) {
            throw new NotAuthorizedException("Secret du client invalide.", "Bearer");
        }
        return tenant;
    }

    private static void requireAllowedRole(Tenant tenant, Identity who) {
        if ((tenant.getAllowedRoles() & who.getRoles()) == 0) {
            throw new ForbiddenException("Ce compte n'a pas le droit d'utiliser cette application.");
        }
    }

    private static LinkedHashSet<String> intersect(String wanted, String provided, String allowed) {
        var set = new LinkedHashSet<>(Scopes.parse(wanted));
        set.retainAll(Scopes.parse(provided));
        set.retainAll(Scopes.parse(allowed));
        return set;
    }

    // ---------- Identités ----------

    /** Inscription libre : toujours le rôle DRIVER. */
    public Identity register(String username, String password) {
        return create(username, password, Role.maskOf(Role.DRIVER), Scopes.DRIVER_SCOPES);
    }

    /** Création par un administrateur : rôles par nom (DRIVER, OPERATOR, ADMIN). */
    public Identity createIdentity(String username, String password, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) throw new BadRequestException("Au moins un rôle est obligatoire.");
        long mask = 0;
        for (var name : roleNames) {
            try {
                mask |= Role.valueOf(name.trim().toUpperCase(Locale.ROOT)).mask();
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Rôle inconnu : " + name);
            }
        }
        return create(username, password, mask, scopesFor(mask));
    }

    public List<Identity> list() {
        return identities.findAll();
    }

    public static String scopesFor(long roles) {
        if (Role.any(roles, Role.ADMIN)) return Scopes.ADMIN_SCOPES;
        if (Role.any(roles, Role.OPERATOR)) return Scopes.OPERATOR_SCOPES;
        return Scopes.DRIVER_SCOPES;
    }

    /** Utilisé par IamSeed : ne fait rien si le compte existe déjà. */
    public void ensureIdentity(String username, String password, long roles) {
        if (identities.findByUsername(normalize(username)).isEmpty()) {
            create(username, password, roles, scopesFor(roles));
        }
    }

    public void ensureTenant(String name, String secret, String redirectUri, long allowedRoles, String allowedScopes, String grantTypes) {
        if (tenants.findByName(name).isEmpty()) {
            tenants.save(Tenant.of(name, secret == null ? null : PasswordHasher.hash(secret), redirectUri, allowedRoles, allowedScopes, grantTypes));
        }
    }

    public void ensureIndexes() {
        identities.ensureIndexes();
        tenants.ensureIndexes();
    }

    private Identity create(String username, String password, long roles, String scopes) {
        var name = normalize(username);
        if (!USERNAME.matcher(name).matches()) {
            throw new BadRequestException("Nom d'utilisateur : 3 à 50 caractères parmi a-z 0-9 . _ @ -");
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new BadRequestException("Le mot de passe doit contenir entre 8 et 128 caractères.");
        }
        if (identities.findByUsername(name).isPresent()) throw new ClientErrorException("Nom d'utilisateur déjà pris.", 409);
        try {
            return identities.save(Identity.of(name, PasswordHasher.hash(password), roles, scopes));
        } catch (MongoWriteException e) {
            throw new ClientErrorException("Nom d'utilisateur déjà pris.", 409);
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
