package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.data.entityobject.DisiplineLanguage;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroup;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroupLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.quickchoicegrouplanguage")
@RolesAllowed({"ApplicationRole"})
public class QuickChoiceGroupLanguageFacadeREST extends AbstractFacade<QuickChoiceGroupLanguage> {

    public QuickChoiceGroupLanguageFacadeREST() {
        super(QuickChoiceGroupLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "QuickChoiceGroupLanguage.findAll";
    }

    @POST
    @Path("create/{quickChoiceGroupId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("quickChoiceGroupId") String quickChoiceGroupId, QuickChoiceGroupLanguage entity) {
        QuickChoiceGroupFacadeREST quickChoiceGroupFacadeREST = new QuickChoiceGroupFacadeREST();
        QuickChoiceGroup quickChoiceGroup = quickChoiceGroupFacadeREST.find(quickChoiceGroupId);
        entity.setQuickChoiceGroup(quickChoiceGroup);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, QuickChoiceGroupLanguage entity) {
        QuickChoiceGroupLanguage existing = this.find(entity.getQuickChoiceGroupLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            existing.setLanguageCode(entity.getLanguageCode());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public QuickChoiceGroupLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
