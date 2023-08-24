/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Sms;
import no.softwarecontrol.idoc.keysms.KeySmsController;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.sms")
@RolesAllowed({"ApplicationRole"})
public class SmsFacadeREST {

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public void send(Sms sms) {
        KeySmsController keySmsController = new KeySmsController();
        //String[] receivers = {"41793713"};
        String[] receivers = new String[sms.getNumberList().size()];
        for(int i=0; i< receivers.length; i++){
            receivers[i]=sms.getNumberList().get(i);
        }
        String message = sms.getMessage();
        keySmsController.sendMessage(message, receivers);
    }

}
