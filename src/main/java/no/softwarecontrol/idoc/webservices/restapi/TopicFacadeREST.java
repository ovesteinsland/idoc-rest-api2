/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Conversation;
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.data.entityobject.Topic;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.topic")
@RolesAllowed({"ApplicationRole"})
public class TopicFacadeREST extends AbstractFacade<Topic> {
    @EJB
    ConversationFacadeREST conversationFacadeREST = new ConversationFacadeREST();
    @EJB
    ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();

    public TopicFacadeREST() {
        super(Topic.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Topic.findAll";
    }

    @PUT
    @Path("linkToConversation/{conversationId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToConversation(@PathParam("conversationId") String conversationId, Topic entity) {
        Topic topic = find(entity.getTopicId());
        Conversation conversation = conversationFacadeREST.find(conversationId);
        if (conversation != null && topic != null) {
            conversation.getTopicList().add(topic);
            topic.getConversationList().add(conversation);
            this.edit(topic);
            conversationFacadeREST.edit(conversation);
        }
    }

    @PUT
    @Path("linkToProject/{projectId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToProject(@PathParam("projectId") String projectId, Topic entity) {
        Topic topic = find(entity.getTopicId());
        Project project = projectFacadeREST.find(projectId);
        if (project != null && topic != null) {
            //project.getTopicList().add(topic);
            //topic.setProject(project);
            this.edit(topic);
            projectFacadeREST.editProjectOnly(project.getProjectId(),project);
        }
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(Topic entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Topic entity) {
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
    public Topic find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Topic> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Topic> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


}
