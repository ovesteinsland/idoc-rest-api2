/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Notification;
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.data.entityobject.User;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.notification")
@RolesAllowed({"ApplicationRole"})
public class NotificationFacadeREST extends AbstractFacade<Notification> {

    public NotificationFacadeREST() {
        super(Notification.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Notification.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(Notification entity) {
        super.create(entity);
    }

    @POST
    @Path("createWithUsers/{fromUser}/{toUser}/{projectId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void createWithUsers(@PathParam("fromUser") String fromUsername, @PathParam("toUser") String toUsername, @PathParam("projectId") String projectId, Notification entity) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        User fromUser = userFacadeREST.find(fromUsername);
        User toUser = userFacadeREST.find(toUsername);
        Project project = projectFacadeREST.find(projectId);

        entity.setFromUser(fromUser);
        entity.getUserList().add(toUser);
        if (project != null) {
            entity.setProject(project);
            project.getNotificationList().add(entity);
            this.create(entity);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Notification entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public Notification find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Notification> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Notification> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

}
