package tn.supcom.cot.iam.boundaries;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tn.supcom.cot.iam.boundaries.IamDtos.Problem;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Toutes les erreurs sortent en application/problem+json (RFC 9457), comme l'API. */
@Provider
public class IamExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(IamExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        int status;
        String title;
        if (e instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            title = Response.Status.fromStatusCode(status) != null
                    ? Response.Status.fromStatusCode(status).getReasonPhrase() : "Error";
        } else if (e instanceof IllegalArgumentException) {
            status = 400;
            title = "Bad Request";
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
