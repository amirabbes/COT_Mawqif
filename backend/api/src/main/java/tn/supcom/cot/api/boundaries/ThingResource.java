package tn.supcom.cot.api.boundaries;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.boundaries.Dtos.ThingDto;
import tn.supcom.cot.api.controllers.managers.ThingManager;

import java.util.List;

@Path("/things")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ThingResource {

    @Inject
    private ThingManager thingManager;

    @GET
    public List<ThingDto> list(@QueryParam("parkingId") String parkingId, @QueryParam("state") String state) {
        return thingManager.list(parkingId, state).stream().map(ThingDto::from).toList();
    }

    @GET
    @Path("/{thingId}")
    public Response get(@PathParam("thingId") String thingId) {
        var thing = thingManager.find(thingId);
        return Response.ok(ThingDto.from(thing)).tag(thing.tag()).build();
    }
}
