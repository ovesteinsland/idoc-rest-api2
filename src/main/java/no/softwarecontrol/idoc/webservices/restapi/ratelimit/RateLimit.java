package no.softwarecontrol.idoc.webservices.restapi.ratelimit;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})

public @interface RateLimit {
    int requests() default 10;           // antall tillatte forespørsler
    int seconds() default 60;
}
