package tn.supcom.cot.api.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.boundaries.Dtos.Problem;

/**
 * Routes du contrat déjà publiées, logique pas encore écrite : 501 jusqu'à la semaine de la réservation.
 * Les PWA peuvent déjà coder contre ces URL.
 */
@Path("/reservations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    private static Response notImplemented() {
        return Response.status(501).type("application/problem+json")
                .entity(Problem.of(501, "Not Implemented", "Réservations : prévu après la semaine 3.")).build();
    }

    @GET
    public Response list() {
        return notImplemented();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(String body) {
        return notImplemented();
    }

    @DELETE
    @Path("/{reservationId}")
    public Response cancel(@PathParam("reservationId") String reservationId) {
        return notImplemented();
    }
}
