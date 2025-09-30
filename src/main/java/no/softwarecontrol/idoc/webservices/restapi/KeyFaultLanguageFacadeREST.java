package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.KeyFault;
import no.softwarecontrol.idoc.data.entityobject.KeyFaultLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.keyfaultlanguage")
@RolesAllowed({"ApplicationRole"})
public class KeyFaultLanguageFacadeREST extends AbstractFacade<KeyFaultLanguage> {

    public KeyFaultLanguageFacadeREST() {
        super(KeyFaultLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "KeyFaultLanguage.findAll";
    }

    @POST
    @Path("create/{keyFaultId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("keyFaultId") String keyFaultId, KeyFaultLanguage entity) {
        KeyFaultFacadeREST keyFaultFacadeREST = new KeyFaultFacadeREST();
        KeyFault keyFault = keyFaultFacadeREST.find(keyFaultId);
        entity.setKeyFault(keyFault);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, KeyFaultLanguage entity) {
        KeyFaultLanguage existing = this.find(entity.getKeyFaultLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            existing.setLanguageCode(entity.getLanguageCode());
            existing.setSynonym(entity.getSynonym());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public KeyFaultLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
