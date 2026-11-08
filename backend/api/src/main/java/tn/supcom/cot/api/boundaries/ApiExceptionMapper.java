package tn.supcom.cot.api.boundaries;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.paho.mqttv5.common.MqttException;
import tn.supcom.cot.api.boundaries.Dtos.Problem;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Toutes les erreurs sortent en application/problem+json (RFC 9457), comme dans openapi.yaml. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        int status;
        String title;
        if (e instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            title = Response.Status.fromStatusCode(status) != null
                    ? Response.Status.fromStatusCode(status).getReasonPhrase() : "Error";
        } else if (e instanceof EntityNotFoundException) {
            status = 404;
            title = "Not Found";
        } else if (e instanceof IllegalArgumentException) {
            status = 400;
            title = "Bad Request";
        } else if (e instanceof MqttException) {
            status = 503;
            title = "Service Unavailable";
        } else {
            LOG.log(Level.SEVERE, "Erreur non prévue", e);
            return problem(500, "Internal Server Error", "Erreur interne.");
        }
        return problem(status, title, e.getMessage());
    }

    private static Response problem(int status, String title, String detail) {
        return Response.status(status).type("application/problem+json")
                .entity(Problem.of(status, title, detail)).build();
    }
}
