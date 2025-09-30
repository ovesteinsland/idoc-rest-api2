package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestion;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestionLanguage;
import no.softwarecontrol.idoc.data.entityobject.CheckListSection;
import no.softwarecontrol.idoc.data.entityobject.CheckListSectionLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistquestionlanguage")
@RolesAllowed({"ApplicationRole"})
public class CheckListQuestionLanguageFacadeREST extends AbstractFacade<CheckListQuestionLanguage> {

    public CheckListQuestionLanguageFacadeREST() {
        super(CheckListQuestionLanguage.class);
    }
    @Override
    protected String getSelectAllQuery(){
        return "CheckListQuestionLanguage.findAll";
    }

    @POST
    @Path("create/{checkListQuestionId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListQuestionId") String checkListQuestionId, CheckListQuestionLanguage entity) {
        CheckListQuestionFacadeREST checkListQuestionFacadeREST = new CheckListQuestionFacadeREST();
        CheckListQuestion checkListQuestion = checkListQuestionFacadeREST.find(checkListQuestionId);
        entity.setCheckListQuestion(checkListQuestion);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListQuestionLanguage entity) {
        CheckListQuestionLanguage existing = this.find(entity.getCheckListQuestionLanguageId());
        if(existing != null) {
            existing.setQuestion(entity.getQuestion());
            existing.setDescription(entity.getDescription());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListQuestionLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
