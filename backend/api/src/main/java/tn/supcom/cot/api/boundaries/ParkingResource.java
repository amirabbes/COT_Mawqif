package tn.supcom.cot.api.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.paho.mqttv5.common.MqttException;
import tn.supcom.cot.api.boundaries.Dtos.CommandDto;
import tn.supcom.cot.api.boundaries.Dtos.OpenBarrierRequest;
import tn.supcom.cot.api.boundaries.Dtos.ParkingDto;
import tn.supcom.cot.api.boundaries.Dtos.SlotDto;
import tn.supcom.cot.api.controllers.managers.CommandManager;
import tn.supcom.cot.api.controllers.managers.ParkingManager;

import java.util.List;

@Path("/parkings")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ParkingResource {

    @Inject
    private ParkingManager parkingManager;
    @Inject
    private CommandManager commandManager;

    @GET
    public List<ParkingDto> list() {
        return parkingManager.list().stream().map(ParkingDto::from).toList();
    }

    @GET
    @Path("/{parkingId}")
    public Response get(@PathParam("parkingId") String parkingId) {
        var parking = parkingManager.find(parkingId);
        return Response.ok(ParkingDto.from(parking)).tag(parking.tag()).build();
    }

    @GET
    @Path("/{parkingId}/slots")
    public List<SlotDto> slots(@PathParam("parkingId") String parkingId, @QueryParam("state") String state) {
        return parkingManager.slotsOf(parkingId, state).stream().map(SlotDto::from).toList();
    }

    /** Ouverture manuelle : réponse 202, le résultat arrive par WebSocket (command_ack). */
    @POST
    @Path("/{parkingId}/barrier/open")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response openBarrier(@PathParam("parkingId") String parkingId, OpenBarrierRequest request)
            throws MqttException {
        if (request == null || request.reason() == null || request.reason().trim().length() < 3
                || request.reason().length() > 200) {
            throw new BadRequestException("Le champ 'reason' est obligatoire (3 à 200 caractères).");
        }
        var command = commandManager.openBarrier(parkingId, request.reason().trim());
        return Response.accepted(CommandDto.from(command)).build();
    }
}
