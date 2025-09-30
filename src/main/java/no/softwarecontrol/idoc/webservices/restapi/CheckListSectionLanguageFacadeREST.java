package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.CheckListLanguage;
import no.softwarecontrol.idoc.data.entityobject.CheckListSection;
import no.softwarecontrol.idoc.data.entityobject.CheckListSectionLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistsectionlanguage")
@RolesAllowed({"ApplicationRole"})
public class CheckListSectionLanguageFacadeREST extends AbstractFacade<CheckListSectionLanguage>{
    public CheckListSectionLanguageFacadeREST() {
        super(CheckListSectionLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "CheckListSectionLanguage.findAll";
    }

    @POST
    @Path("create/{checkListSectionId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("checkListSectionId") String checkListSectionId, CheckListSectionLanguage entity) {
        CheckListSectionFacadeREST checkListSectionFacadeREST = new CheckListSectionFacadeREST();
        CheckListSection checkListSection = checkListSectionFacadeREST.find(checkListSectionId);
        entity.setCheckListSection(checkListSection);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListSectionLanguage entity) {
        CheckListSectionLanguage existing = this.find(entity.getCheckListSectionLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            super.edit(existing);
        }

    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListSectionLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
