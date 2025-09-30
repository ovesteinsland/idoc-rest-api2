package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.KeyComponent;
import no.softwarecontrol.idoc.data.entityobject.KeyComponentLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.keycomponentlanguage")
@RolesAllowed({"ApplicationRole"})
public class KeyComponentLanguageFacadeREST extends AbstractFacade<KeyComponentLanguage> {

    public KeyComponentLanguageFacadeREST() {
        super(KeyComponentLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "KeyComponentLanguage.findAll";
    }

    @POST
    @Path("create/{keyComponentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("keyComponentId") String keyComponentId, KeyComponentLanguage entity) {
        KeyComponentFacadeREST keyComponentFacadeREST = new KeyComponentFacadeREST();
        KeyComponent keyComponent = keyComponentFacadeREST.find(keyComponentId);
        entity.setKeyComponent(keyComponent);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, KeyComponentLanguage entity) {
        KeyComponentLanguage existing = this.find(entity.getKeyComponentLanguageId());
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
    public KeyComponentLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
