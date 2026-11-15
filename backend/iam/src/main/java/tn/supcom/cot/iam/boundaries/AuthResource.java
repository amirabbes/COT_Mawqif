package tn.supcom.cot.iam.boundaries;

import jakarta.enterprise.context.RequestScoped;
import tn.supcom.cot.core.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.iam.boundaries.IamDtos.RegisterRequest;
import tn.supcom.cot.iam.boundaries.IamDtos.TokenRequest;
import tn.supcom.cot.iam.boundaries.IamDtos.TokenResponse;
import tn.supcom.cot.iam.controllers.managers.IamManager;
import tn.supcom.cot.core.security.Role;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Connexion et inscription. /token et /register sont publics ; /me exige un jeton. */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private IamManager iam;

    @POST
    @Path("/token")
    public Response token(TokenRequest request) {
        if (request == null || request.grantType() == null) throw new BadRequestException("grant_type obligatoire.");
        var result = switch (request.grantType()) {
            case "password" -> iam.passwordGrant(request.clientId(), request.clientSecret(),
                    request.username(), request.password(), request.scope());
            case "refresh_token" -> iam.refreshGrant(request.clientId(), request.clientSecret(), request.refreshToken());
            default -> throw new BadRequestException("unsupported_grant_type");
        };
        // RFC 6749 §5.1 : une réponse de jeton ne doit jamais être mise en cache.
        return Response.ok(TokenResponse.from(result)).header("Cache-Control", "no-store").header("Pragma", "no-cache").build();
    }

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        if (request == null) throw new BadRequestException("Corps obligatoire.");
        var identity = iam.register(request.username(), request.password());
        return Response.created(URI.create("/api/v1/auth/me")).entity(IamDtos.IdentityDto.from(identity)).build();
    }

    @GET
    @Path("/me")
    @Secured
    public Map<String, Object> me(@Context HttpHeaders headers) {
        // AuthFilter a déjà validé le jeton (ou l'IAM est désactivée) ; on relit ses claims.
        var claims = iam.fromAuthorizationHeader(headers.getHeaderString(HttpHeaders.AUTHORIZATION)).orElse(null);
        if (claims == null) return Map.of("authentication", "disabled");
        return Map.of("id", claims.subject(), "username", claims.username(),
                "roles", Role.fromMask(claims.roles()).stream().map(Enum::name).toList(),
                "scope", claims.scope(), "client_id", claims.clientId(), "expires_at", claims.expiresAt().toString());
    }
}
