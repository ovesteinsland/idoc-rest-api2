package no.softwarecontrol.idoc.webservices.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class UnsupportedMediaException extends WebApplicationException {
    public UnsupportedMediaException(String message) {
        super(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity(message).type(MediaType.TEXT_PLAIN).build());
    }
}

