package tn.supcom.cot.iam.boundaries;

import jakarta.enterprise.context.RequestScoped;
import tn.supcom.cot.core.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.iam.boundaries.IamDtos.CreateIdentityRequest;
import tn.supcom.cot.iam.boundaries.IamDtos.IdentityDto;
import tn.supcom.cot.iam.controllers.managers.IamManager;
import tn.supcom.cot.core.security.Role;
import tn.supcom.cot.core.security.Scopes;

import java.util.List;

/** Administration des comptes (création d'opérateurs, liste). Réservé aux administrateurs. */
@Path("/identities")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Secured(roles = Role.ADMIN, scopes = Scopes.IAM_MANAGE)
public class IdentityResource {

    @Inject
    private IamManager iam;

    @GET
    public List<IdentityDto> list() {
        return iam.list().stream().map(IdentityDto::from).toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateIdentityRequest request) {
        if (request == null) throw new BadRequestException("Corps obligatoire.");
        var identity = iam.createIdentity(request.username(), request.password(), request.roles());
        return Response.status(Response.Status.CREATED).entity(IdentityDto.from(identity)).build();
    }
}
