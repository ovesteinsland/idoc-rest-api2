package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckListAnswer;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestion;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistanswer")
@RolesAllowed({"ApplicationRole"})
public class CheckListAnswerFacadeREST extends AbstractFacade<CheckListAnswer> {

    public CheckListAnswerFacadeREST() {
        super(CheckListAnswer.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(CheckListAnswer entity) {
        super.create(entity);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Path("createWithCheckListQuestion/{checkListQuestionId}")
    public void createWithCheckListSection(@PathParam("checkListQuestionId") String checkListQuestionId, CheckListAnswer entity) {
        CheckListQuestionFacadeREST checkListQuestionFacadeREST = new CheckListQuestionFacadeREST();
        CheckListQuestion checkListQuestion = checkListQuestionFacadeREST.find(checkListQuestionId);
        if(checkListQuestion != null) {
            entity.setCheckListQuestion(checkListQuestion);
            super.create(entity);
        }

    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListAnswer entity) {
        CheckListAnswer existing = this.find(entity.getCheckListAnswerId());
        if(existing != null) {
            existing.setAnswer(entity.getAnswer());
            existing.setSortIndex(entity.getSortIndex());
            existing.setDescription(entity.getDescription());
            existing.setAnswerType(entity.getAnswerType());
            existing.setAnswerAuxiliary(entity.getAnswerAuxiliary());
            existing.setTemplateVariable(entity.getTemplateVariable());
            existing.setQuickChoiceItemList(entity.getQuickChoiceItemList());
        }
        super.edit(existing);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListAnswer find(@PathParam("id") String id) {
        return super.find(id);
    }
}
