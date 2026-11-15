package tn.supcom.cot.core.security;

import jakarta.json.Json;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Émet et vérifie des JWT HS256 (access et refresh). Sans état : pas de révocation côté serveur. */
public final class JwtService {

    /** Clé de développement : à remplacer par IAM_JWT_SECRET (identique dans iam-1.0.war et api-1.0.war). */
    public static final String DEV_SECRET = "dev-only-change-me-iam-secret-0123456789";

    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    /** Contenu vérifié d'un jeton. */
    public record Claims(String subject, String username, long roles, String scope, String clientId,
                         String type, Instant issuedAt, Instant expiresAt) {
        public boolean hasAnyRole(Role... wanted) { return wanted.length == 0 || Role.any(roles, wanted); }
        public boolean hasScope(String s) { return Scopes.parse(scope).contains(s); }
    }

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final byte[] key;
    private final String issuer;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public Duration accessTtl() { return accessTtl; }

    public String issue(String type, String identityId, String username, long roles, String scope, String clientId) {
        var now = Instant.now();
        var ttl = ACCESS.equals(type) ? accessTtl : refreshTtl;
        var payload = Json.createObjectBuilder()
                .add("iss", issuer)
                .add("sub", identityId)
                .add("preferred_username", username)
                .add("roles", roles)
                .add("scope", scope)
                .add("client_id", clientId)
                .add("typ", type)
                .add("iat", now.getEpochSecond())
                .add("exp", now.plus(ttl).getEpochSecond())
                .add("jti", UUID.randomUUID().toString())
                .build().toString();
        var unsigned = HEADER + "." + b64(payload.getBytes(StandardCharsets.UTF_8));
        return unsigned + "." + b64(hmac(unsigned));
    }

    /** Retourne les claims si la signature, l'émetteur, le type et l'expiration sont valides. */
    public Optional<Claims> parse(String token, String expectedType) {
        try {
            if (token == null) return Optional.empty();
            var parts = token.split("\\.");
            // L'en-tête doit être exactement le nôtre : refuse alg=none et tout autre algorithme.
            if (parts.length != 3 || !HEADER.equals(parts[0])) return Optional.empty();
            var expected = hmac(parts[0] + "." + parts[1]);
            var given = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, given)) return Optional.empty();

            var json = Json.createReader(new StringReader(
                    new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8))).readObject();
            if (!issuer.equals(json.getString("iss")) || !expectedType.equals(json.getString("typ"))) return Optional.empty();
            var exp = Instant.ofEpochSecond(json.getJsonNumber("exp").longValue());
            if (!exp.isAfter(Instant.now())) return Optional.empty();
            return Optional.of(new Claims(json.getString("sub"), json.getString("preferred_username"),
                    json.getJsonNumber("roles").longValue(), json.getString("scope"), json.getString("client_id"),
                    json.getString("typ"), Instant.ofEpochSecond(json.getJsonNumber("iat").longValue()), exp));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }

    private static String b64(byte[] bytes) { return ENC.encodeToString(bytes); }
}
