package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Tool;
import no.softwarecontrol.idoc.data.entityobject.User;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.tool")
@RolesAllowed({"ApplicationRole"})
public class ToolFacadeREST extends AbstractFacade<Tool>{

    public ToolFacadeREST() {
        super(Tool.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Tool find(@PathParam("id") String id) {
        Tool tool = super.find(id);
        return tool;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(Tool entity) {
        super.create(entity);
    }


    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Tool entity) {
        Tool tool = this.find(id);
        if (tool != null) {
            tool.setName(entity.getName());
            tool.setToolType(entity.getToolType());
            tool.setManufacturer(entity.getManufacturer());
            tool.setSerialNo(entity.getSerialNo());
            tool.setCalibrationDate(entity.getCalibrationDate());
            tool.setDeleted(entity.getDeleted());
            tool.setDescription(entity.getDescription());
            super.edit(tool);
        }
    }

    @PUT
    @Path("linkToUser/{certificateId}/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("certificateId") String certificateId, @PathParam("userId") String userId) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        User user = userFacadeREST.find(userId);
        Tool tool = this.find(certificateId);
        if (user != null && tool != null) {
            if (!user.getToolList().contains(tool)) {
                user.getToolList().add(tool);
                userFacadeREST.edit(user);
            }
            if (!tool.getUserList().contains(user)) {
                tool.getUserList().add(user);
                this.edit(tool);
            }
        }
    }
}
