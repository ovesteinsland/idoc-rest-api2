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
import no.softwarecontrol.idoc.data.entityobject.Conversation;
import no.softwarecontrol.idoc.data.entityobject.Message;
import no.softwarecontrol.idoc.data.entityobject.User;

import java.util.Date;
import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.message")
@RolesAllowed({"ApplicationRole"})
public class MessageFacadeREST extends AbstractFacade<Message> {

    public MessageFacadeREST() {
        super(Message.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Message.findAll";
    }

    @PUT
    @Path("linkToConversation/{conversationId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToConversation(@PathParam("conversationId") String conversationId, Message entity) {
        ConversationFacadeREST conversationFacadeREST = new ConversationFacadeREST();
        Message message = this.find(entity.getMessageId());
        Conversation conversation = conversationFacadeREST.find(conversationId);
        if (message != null && conversation != null) {
            message.setConversation(conversation);
            conversation.getMessageList().add(message);
            this.edit(message);
            conversationFacadeREST.edit(conversation);
        }
    }
    
    @PUT
    @Path("linkToUser/{userId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Message entity) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        Message message = this.find(entity.getMessageId());
        User user = userFacadeREST.find(userId);
        if (message != null && user != null) {
            /*message.setSender(user);
            user.getMessageList().add(message);*/
            this.edit(message);
            userFacadeREST.edit(user);
        }
    }
    
    @PUT
    @Path("createWithConversation/{conversationId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void createWithConversation(@PathParam("conversationId") String conversationId, Message message) {
        ConversationFacadeREST conversationFacadeREST = new ConversationFacadeREST();
        Conversation conversation = conversationFacadeREST.find(conversationId);
        if (conversation != null && message != null) {
            conversation.getMessageList().add(message);
            conversation.setModifiedDate(message.getCreatedDate());
            message.setConversation(conversation);
            this.create(message);
            conversationFacadeREST.edit(conversation);
        }
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(Message entity) {
        entity.setCreatedDate(new Date());
        super.create(entity);
    }
    


    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Message entity) {
        Message message = find(id);
        message.setCreatedDate(new Date());
        message.setRead(entity.isRead());
        super.edit(message);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public Message find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Message> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Message> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
    
}
