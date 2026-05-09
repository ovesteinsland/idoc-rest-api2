package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.CheckListSection;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklistsection")
@RolesAllowed({"ApplicationRole"})
public class CheckListSectionFacadeREST extends AbstractFacade<CheckListSection> {

    private static CheckListSectionFacadeREST instance;

    public CheckListSectionFacadeREST() {
        super(CheckListSection.class);
        instance = this;
    }

    public static CheckListSectionFacadeREST getInstance() {
        if (instance == null) {
            instance = new CheckListSectionFacadeREST();
        }
        return instance;
    }


    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(CheckListSection entity) {
        super.create(entity);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Path("createWithCheckList/{checkListId}")
    public void createWithCheckList(@PathParam("checkListId") String checkListSectionId, CheckListSection entity) {

        CheckList checkList = CheckListFacadeREST.getInstance().find(checkListSectionId);
        if(checkList != null) {
            entity.setCheckList(checkList);
            super.create(entity);
        }

    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckListSection entity) {
        CheckListSection existing = this.find(entity.getCheckListSectionId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setSortIndex(entity.getSortIndex());
            existing.setDescription(entity.getDescription());
        }
        super.edit(existing);
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckListSection find(@PathParam("id") String id) {
        return super.find(id);
    }
}
