package tn.supcom.cot.api.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.controllers.managers.ReservationManager;
import tn.supcom.cot.api.controllers.managers.ReservationManager.Caller;
import tn.supcom.cot.api.entities.Reservation;
import tn.supcom.cot.core.security.Role;
import tn.supcom.cot.core.security.TokenVerifier;
import tn.supcom.cot.core.security.Secured;
import tn.supcom.cot.core.security.Scopes;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** Réservations. Un automobiliste gère les siennes ; l'exploitant (OPERATOR/ADMIN) voit et annule toutes. */
@Path("/reservations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Secured(roles = {Role.DRIVER, Role.OPERATOR, Role.ADMIN}, scopes = Scopes.RESERVATION_WRITE)
public class ReservationResource {

    /** Corps de POST /reservations (voir CreateReservationRequest dans openapi.yaml). */
    public record CreateRequest(String parkingId, String plate, Instant from, Instant to) {}

    public record ReservationDto(String id, String parkingId, String plate, Instant from, Instant to, String status,
                                 String owner) {
        static ReservationDto from(Reservation r) {
            return new ReservationDto(r.getId(), r.getParkingId(), r.getPlate(), r.getFrom(), r.getTo(), r.getStatus(),
                    r.getOwnerName());
        }
    }

    @Inject
    private ReservationManager reservations;
    @Inject
    private TokenVerifier iam;

    @GET
    public List<ReservationDto> list(@Context HttpHeaders headers, @QueryParam("parkingId") String parkingId) {
        return reservations.list(caller(headers), parkingId).stream().map(ReservationDto::from).toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context HttpHeaders headers, CreateRequest request) {
        if (request == null) throw new BadRequestException("Corps obligatoire.");
        var created = reservations.create(caller(headers), request.parkingId(), request.plate(), request.from(), request.to());
        return Response.created(URI.create("/api/v1/reservations/" + created.getId()))
                .entity(ReservationDto.from(created)).build();
    }

    @DELETE
    @Path("/{reservationId}")
    public Response cancel(@Context HttpHeaders headers, @PathParam("reservationId") String reservationId) {
        reservations.cancel(caller(headers), reservationId);
        return Response.noContent().build();
    }

    private Caller caller(HttpHeaders headers) {
        var claims = iam.fromAuthorizationHeader(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (claims.isPresent()) {
            var c = claims.get();
            return new Caller(c.subject(), c.username(), c.hasAnyRole(Role.OPERATOR, Role.ADMIN));
        }
        if (!iam.enabled()) return new Caller("anonymous", "anonymous", true); // IAM désactivée (débogage)
        throw new NotAuthorizedException("Jeton Bearer manquant.", "Bearer");
    }
}
