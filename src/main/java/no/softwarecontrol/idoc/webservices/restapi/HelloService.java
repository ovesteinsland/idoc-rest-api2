package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.softwarecontrol.idoc.data.entityobject.User;

import java.util.ArrayList;
import java.util.List;

@Path("hello")
public class HelloService {

//    @GET
//    @Path("{clientName}")
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response greetClient(@PathParam("clientName") String name) {
//        String output = "Hi " + name;
//        return Response.status(200).entity(output).build();
//    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("loadUsers")
    public List<User> loadUsers() {
        String output = "Hi OVE";
        User user = new User();
        user.setFirstname("Ove");
        user.setLastname("Steinsland");
        List<User> users =  new ArrayList<User>();
        users.add(user);
        return users;
    }

}
