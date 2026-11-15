package tn.supcom.cot.iam.boundaries;

import jakarta.json.bind.annotation.JsonbProperty;
import tn.supcom.cot.iam.controllers.managers.IamManager.TokenResult;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.core.security.Role;

import java.time.Instant;
import java.util.List;

/** DTO de l'IAM. Les champs du jeton suivent les noms OAuth 2.0 (snake_case, RFC 6749). */
public final class IamDtos {
    private IamDtos() {}

    /** Erreur application/problem+json (RFC 9457). */
    public record Problem(String type, String title, int status, String detail) {
        public static Problem of(int status, String title, String detail) {
            return new Problem("about:blank", title, status, detail);
        }
    }

    public record TokenRequest(
            @JsonbProperty("grant_type") String grantType,
            @JsonbProperty("client_id") String clientId,
            @JsonbProperty("client_secret") String clientSecret,
            String username,
            String password,
            @JsonbProperty("refresh_token") String refreshToken,
            String scope) {}

    public record TokenResponse(
            @JsonbProperty("access_token") String accessToken,
            @JsonbProperty("token_type") String tokenType,
            @JsonbProperty("expires_in") long expiresIn,
            @JsonbProperty("refresh_token") String refreshToken,
            String scope) {
        public static TokenResponse from(TokenResult r) {
            return new TokenResponse(r.accessToken(), "Bearer", r.expiresIn(), r.refreshToken(), r.scope());
        }
    }

    public record RegisterRequest(String username, String password) {}

    public record CreateIdentityRequest(String username, String password, List<String> roles) {}

    public record IdentityDto(String id, String username, List<String> roles, String scope, Instant createdAt) {
        public static IdentityDto from(Identity i) {
            return new IdentityDto(i.getId(), i.getUsername(),
                    Role.fromMask(i.getRoles()).stream().map(Enum::name).toList(), i.getProvidedScopes(), i.getCreatedAt());
        }
    }
}
