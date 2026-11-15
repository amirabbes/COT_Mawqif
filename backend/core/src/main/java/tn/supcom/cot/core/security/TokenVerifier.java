package tn.supcom.cot.core.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;
import tn.supcom.cot.core.security.JwtService.Claims;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Vérifie les jetons émis par le service IAM (iam-1.0.war). Utilisé par chaque WAR qui protège des routes.
 * Les deux services partagent la clé de signature (iam.jwt.secret / variable IAM_JWT_SECRET) et l'émetteur (iam.jwt.issuer).
 * iam.enabled=false : plus aucune route n'exige de jeton (débogage).
 */
@ApplicationScoped
public class TokenVerifier {
    private static final Logger LOG = Logger.getLogger(TokenVerifier.class.getName());

    private JwtService jwt;
    private boolean enabled;

    @PostConstruct
    void init() {
        var config = ConfigProvider.getConfig();
        enabled = config.getOptionalValue("iam.enabled", Boolean.class).orElse(true);
        var secret = config.getOptionalValue("iam.jwt.secret", String.class).orElse(JwtService.DEV_SECRET);
        if (JwtService.DEV_SECRET.equals(secret)) {
            LOG.warning("Clé JWT par défaut utilisée. Définir IAM_JWT_SECRET (identique pour iam et api) hors développement.");
        }
        // Les durées ne servent qu'à l'émission, jamais à la vérification.
        jwt = new JwtService(secret, config.getOptionalValue("iam.jwt.issuer", String.class).orElse("mawqif"),
                Duration.ZERO, Duration.ZERO);
    }

    public boolean enabled() {
        return enabled;
    }

    /** Claims d'un jeton d'accès (sans le préfixe "Bearer "). */
    public Optional<Claims> authenticate(String accessToken) {
        return jwt.parse(accessToken, JwtService.ACCESS);
    }

    /** Claims du jeton d'un en-tête "Authorization: Bearer ..." (vide si absent ou invalide). */
    public Optional<Claims> fromAuthorizationHeader(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        return authenticate(header.substring(7).trim());
    }
}
