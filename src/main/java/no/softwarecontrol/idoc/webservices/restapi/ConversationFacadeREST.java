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
import no.softwarecontrol.idoc.data.entityobject.User;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.conversation")
@RolesAllowed({"ApplicationRole"})
public class ConversationFacadeREST extends AbstractFacade<Conversation> {

    public ConversationFacadeREST() {
        super(Conversation.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Conversation.findAll";
    }

    @PUT
    @Path("linkToUser/{userId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Conversation entity) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        Conversation conversation = find(entity.getConversationId());
        User user = userFacadeREST.find(userId);
        if(conversation != null && user != null){
            conversation.getUserList().add(user);
            user.getConversationList().add(conversation);
            userFacadeREST.edit(user);
            this.edit(conversation);
        }
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(Conversation entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Conversation entity) {
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
    public Conversation find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Conversation> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Conversation> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
    
}
