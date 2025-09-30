package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.CheckListLanguage;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.data.entityobject.DisiplineLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.disiplinelanguage")
@RolesAllowed({"ApplicationRole"})
public class DisiplineLanguageClientFacadeREST extends AbstractFacade<DisiplineLanguage> {
    public DisiplineLanguageClientFacadeREST() {
        super(DisiplineLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "CheckListLanguage.findAll";
    }

    @POST
    @Path("create/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("disiplineId") String disiplineId, DisiplineLanguage entity) {
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        entity.setDisipline(disipline);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, DisiplineLanguage entity) {
        DisiplineLanguage existing = this.find(entity.getDisiplineLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            existing.setGroup(entity.getGroup());
            existing.setLanguageCode(entity.getLanguageCode());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public DisiplineLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
