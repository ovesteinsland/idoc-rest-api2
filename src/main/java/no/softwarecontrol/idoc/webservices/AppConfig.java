package no.softwarecontrol.idoc.webservices;
import jakarta.ws.rs.ApplicationPath;
import no.softwarecontrol.idoc.filter.CorsFilter;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Set;

@ApplicationPath("/iDocWebServices/webresources")
public class AppConfig extends ResourceConfig {

    public AppConfig() {
        // Register filter and resource packages
        register(CorsFilter.class);
        register(no.softwarecontrol.idoc.filter.CognitoJwtAuthFilter.class);
        packages("no.softwarecontrol.idoc.webservices");
    }


}