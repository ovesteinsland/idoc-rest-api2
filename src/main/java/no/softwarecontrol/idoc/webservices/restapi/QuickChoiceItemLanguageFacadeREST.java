package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroup;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroupLanguage;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceItem;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceItemLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.quickchoiceitemlanguage")
@RolesAllowed({"ApplicationRole"})
public class QuickChoiceItemLanguageFacadeREST extends AbstractFacade<QuickChoiceItemLanguage> {

    public QuickChoiceItemLanguageFacadeREST() {
        super(QuickChoiceItemLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "QuickChoiceItemLanguage.findAll";
    }

    @POST
    @Path("create/{quickChoiceItemId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("quickChoiceItemId") String quickChoiceItemId, QuickChoiceItemLanguage entity) {
        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find(quickChoiceItemId);
        entity.setQuickChoiceItem(quickChoiceItem);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, QuickChoiceItemLanguage entity) {
        QuickChoiceItemLanguage existing = this.find(entity.getQuickChoiceItemLanguageId());
        if(existing != null) {
            existing.setLanguageCode(entity.getLanguageCode());
            existing.setName(entity.getName());
            existing.setFullText(entity.getFullText());
            existing.setRegulationReference(entity.getRegulationReference());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public QuickChoiceItemLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
