package tn.supcom.cot.api.boundaries;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Autorise les PWA (Vite : 5173 et 5174) à appeler l'API en local. Liste modifiable : cors.allowed.origins. */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private final Set<String> allowed = Arrays.stream(
                    ConfigProvider.getConfig()
                            .getOptionalValue("cors.allowed.origins", String.class)
                            .orElse("http://localhost:5173,http://localhost:5174")
                            .split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        var origin = request.getHeaderString("Origin");
        if (origin == null || !allowed.contains(origin)) return;
        var h = response.getHeaders();
        h.putSingle("Access-Control-Allow-Origin", origin);
        h.putSingle("Vary", "Origin");
        h.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.putSingle("Access-Control-Allow-Headers", "Content-Type, Authorization, If-Match");
        h.putSingle("Access-Control-Expose-Headers", "ETag, Location");
    }
}
