package tn.supcom.cot.api.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tn.supcom.cot.api.boundaries.Dtos.AlertDto;
import tn.supcom.cot.api.controllers.managers.AlertManager;

import java.util.List;

@Path("/alerts")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    private AlertManager alertManager;

    @GET
    public List<AlertDto> list(@QueryParam("parkingId") String parkingId,
                               @QueryParam("status") String status,
                               @QueryParam("limit") @DefaultValue("50") int limit) {
        return alertManager.list(parkingId, status, limit).stream().map(AlertDto::from).toList();
    }

    @POST
    @Path("/{alertId}/acknowledge")
    public AlertDto acknowledge(@PathParam("alertId") String alertId) {
        return AlertDto.from(alertManager.acknowledge(alertId));
    }
}
