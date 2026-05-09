package no.softwarecontrol.idoc.webservices.data.cognito;

public class ErrorResponse {
    public String error;
    public String message;
    public String errorCode;

    public ErrorResponse() {}

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(String error, String message, String errorCode) {
        this.error = error;
        this.message = message;
        this.errorCode = errorCode;
    }
}
