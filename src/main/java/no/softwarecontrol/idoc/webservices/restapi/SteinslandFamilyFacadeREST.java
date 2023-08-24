package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.testentities.Daughter;
import no.softwarecontrol.idoc.data.testentities.Father;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ovesteinsland on 08/06/2017.
 */

@Stateless
@Path("steinsland")
public class SteinslandFamilyFacadeREST {

    @GET
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Daughter> showDaughters() {
        Father father = new Father();
        father.setId("0");
        father.setName("Pappa");

        List<Daughter> daughters = new ArrayList<>();

        Daughter solfrid = new Daughter();
        solfrid.setDaughterId("3");
        solfrid.setName("Solfrid");
        solfrid.setFather(father);
        father.getDaughters().add(solfrid);

        Daughter marta = new Daughter();
        marta.setDaughterId("2");
        marta.setName("Marta");
        marta.setFather(father);
        father.getDaughters().add(marta);

        Daughter kristin = new Daughter();
        kristin.setDaughterId("1");
        kristin.setName("Kristin");
        kristin.setFather(father);
        father.getDaughters().add(kristin);

        daughters.add(kristin);
        daughters.add(marta);
        daughters.add(solfrid);


        return daughters;
    }

}
