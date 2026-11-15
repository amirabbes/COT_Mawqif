package tn.supcom.cot.api.boundaries;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Autorise les PWA à appeler l'API. Liste modifiable : cors.allowed.origins (variable CORS_ALLOWED_ORIGINS).
 * "*" autorise toute origine : acceptable en réseau local de développement car l'API utilise des jetons Bearer
 * (pas de cookies) ; à remplacer par la liste exacte hors développement.
 * cors.enabled=false (variable CORS_ENABLED) désactive ce filtre quand WildFly ajoute déjà les en-têtes CORS
 * (scripts .cli de l'enseignant en HTTPS) : sinon l'en-tête serait en double et les navigateurs le refuseraient.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private final boolean enabled = ConfigProvider.getConfig()
            .getOptionalValue("cors.enabled", Boolean.class).orElse(true);

    private final Set<String> allowed = Arrays.stream(
                    ConfigProvider.getConfig()
                            .getOptionalValue("cors.allowed.origins", String.class)
                            .orElse("http://localhost:5173,http://localhost:5174")
                            .split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (!enabled) return;
        var origin = request.getHeaderString("Origin");
        if (origin == null || !(allowed.contains("*") || allowed.contains(origin))) return;
        var h = response.getHeaders();
        h.putSingle("Access-Control-Allow-Origin", origin);
        h.putSingle("Vary", "Origin");
        h.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.putSingle("Access-Control-Allow-Headers", "Content-Type, Authorization, If-Match");
        h.putSingle("Access-Control-Expose-Headers", "ETag, Location");
    }
}
