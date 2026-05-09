package no.softwarecontrol.idoc.webservices.data.cognito;

public class LogoutRequest {
    public String accessToken;

    public LogoutRequest() {}

    public LogoutRequest(String accessToken) {
        this.accessToken = accessToken;
    }
}