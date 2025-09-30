package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.CheckListLanguage;
import org.checkerframework.checker.units.qual.C;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistlanguage")
@RolesAllowed({"ApplicationRole"})
public class CheckListLanguageFacadeREST extends AbstractFacade<CheckListLanguage> {

    public CheckListLanguageFacadeREST() {
        super(CheckListLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "CheckListLanguage.findAll";
    }

    @POST
    @Path("create/{checkListId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListId") String checkListId, CheckListLanguage entity) {
        CheckListFacadeREST checkListFacadeREST = new CheckListFacadeREST();
        CheckList checkList = checkListFacadeREST.find(checkListId);
        entity.setCheckList(checkList);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListLanguage entity) {
        CheckListLanguage existing = this.find(entity.getCheckListLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
