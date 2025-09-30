package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.KeyFault;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.keyfault")
@RolesAllowed({"ApplicationRole"})
public class KeyFaultFacadeREST extends AbstractFacade<KeyFault>  {

    public KeyFaultFacadeREST() {
        super(KeyFault.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "KeyFault.findAll";
    }

    @GET
    @Path("loadByQuickChoiceGroup/{quickChoiceGroupId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<KeyFault> findByQuickChoiceGroup(@PathParam("quickChoiceGroupId") String quickChoiceGroupId) {
        return super.findAll();
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<KeyFault> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public KeyFault find(@PathParam("id") String id) {
        return super.find(id);
    }

}
