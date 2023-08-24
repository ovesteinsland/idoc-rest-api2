package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.KeyComponent;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.keycomponent")
@RolesAllowed({"ApplicationRole"})
public class KeyComponentFacadeREST extends AbstractFacade<KeyComponent>  {

    public KeyComponentFacadeREST() {
        super(KeyComponent.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "KeyComponent.findAll";
    }

    @GET
    @Path("loadByQuickChoiceGroup/{quickChoiceGroupId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<KeyComponent> findByQuickChoiceGroup(@PathParam("quickChoiceGroupId") String quickChoiceGroupId) {
        return super.findAll();
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<KeyComponent> find(@PathParam("id") String id) {
        return super.findAll();
    }
}
