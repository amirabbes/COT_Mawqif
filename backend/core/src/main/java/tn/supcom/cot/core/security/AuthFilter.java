package tn.supcom.cot.core.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import tn.supcom.cot.core.security.JwtService.Claims;

import java.security.Principal;

/** Valide le jeton Bearer des ressources annotées @Secured. Les erreurs partent en problem+json via l'ExceptionMapper du WAR. */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;
    @Inject
    private TokenVerifier verifier;

    @Override
    public void filter(ContainerRequestContext request) {
        // Les pré-requêtes CORS n'ont pas d'en-tête Authorization.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !verifier.enabled()) return;

        var secured = resourceInfo.getResourceMethod().getAnnotation(Secured.class);
        if (secured == null) secured = resourceInfo.getResourceClass().getAnnotation(Secured.class);

        var header = request.getHeaderString("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new NotAuthorizedException("Jeton Bearer manquant.", "Bearer");
        }
        Claims claims = verifier.authenticate(header.substring(7).trim())
                .orElseThrow(() -> new NotAuthorizedException("Jeton invalide ou expiré.", "Bearer"));

        if (secured != null) {
            if (!claims.hasAnyRole(secured.roles())) throw new ForbiddenException("Rôle insuffisant.");
            for (var scope : secured.scopes()) {
                if (!claims.hasScope(scope)) throw new ForbiddenException("Scope manquant : " + scope);
            }
        }

        var secure = request.getSecurityContext().isSecure();
        request.setSecurityContext(new SecurityContext() {
            @Override public Principal getUserPrincipal() { return claims::username; }
            @Override public boolean isUserInRole(String role) {
                try { return claims.hasAnyRole(Role.valueOf(role)); } catch (IllegalArgumentException e) { return false; }
            }
            @Override public boolean isSecure() { return secure; }
            @Override public String getAuthenticationScheme() { return "Bearer"; }
        });
    }
}
