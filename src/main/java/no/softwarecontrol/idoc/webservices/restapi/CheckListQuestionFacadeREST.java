package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestion;
import no.softwarecontrol.idoc.data.entityobject.CheckListSection;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistquestion")
@RolesAllowed({"ApplicationRole"})
public class CheckListQuestionFacadeREST extends AbstractFacade<CheckListQuestion> {

    public CheckListQuestionFacadeREST() {
        super(CheckListQuestion.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(CheckListQuestion entity) {
        super.create(entity);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Path("createWithCheckListSection/{checkListSectionId}")
    public void createWithCheckListSection(@PathParam("checkListSectionId") String checkListSectionId, CheckListQuestion entity) {
        CheckListSectionFacadeREST checkListSectionFacadeREST = new CheckListSectionFacadeREST();
        CheckListSection checkListSection = checkListSectionFacadeREST.find(checkListSectionId);
        if(checkListSection != null) {
            entity.setCheckListSection(checkListSection);
            super.create(entity);
        }

    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListQuestion entity) {
        CheckListQuestion existing = this.find(entity.getCheckListQuestionId());
        if(existing != null) {
            existing.setQuestion(entity.getQuestion());
            existing.setSortIndex(entity.getSortIndex());
            existing.setDescription(entity.getDescription());
            existing.setLayoutType(entity.getLayoutType());
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
    public CheckListQuestion find(@PathParam("id") String id) {
        return super.find(id);
    }
}
