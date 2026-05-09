package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckListAnswer;
import no.softwarecontrol.idoc.data.entityobject.CheckListAnswerLanguage;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestion;
import no.softwarecontrol.idoc.data.entityobject.CheckListQuestionLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistanswerlanguage")
@RolesAllowed({"ApplicationRole"})
public class CheckListAnswerLanguageFacadeREST extends AbstractFacade<CheckListAnswerLanguage> {

    private static CheckListAnswerLanguageFacadeREST instance;


    public CheckListAnswerLanguageFacadeREST() {
        super(CheckListAnswerLanguage.class);
        instance = this;
    }

    public static CheckListAnswerLanguageFacadeREST getInstance() {
        if (instance == null) {
            instance = new CheckListAnswerLanguageFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery(){
        return "CheckListAnswerLanguage.findAll";
    }

    @POST
    @Path("create/{checkListAnswerId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListAnswerId") String checkListAnswerId, CheckListAnswerLanguage entity) {
        CheckListAnswer checkListAnswer = CheckListAnswerFacadeREST.getInstance().find(checkListAnswerId);
        entity.setCheckListAnswer(checkListAnswer);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListAnswerLanguage entity) {
        CheckListAnswerLanguage existing = this.find(entity.getCheckListAnswerLanguageId());
        if(existing != null) {
            existing.setAnswer(entity.getAnswer());
            existing.setAnswerAuxiliary(entity.getAnswerAuxiliary());
            existing.setDescription(entity.getDescription());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListAnswerLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }

}
