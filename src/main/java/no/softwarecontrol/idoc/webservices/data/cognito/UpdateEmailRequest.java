package no.softwarecontrol.idoc.webservices.data.cognito;

public class UpdateEmailRequest {
    public String accessToken;
    public String newEmail;

    public UpdateEmailRequest() {}

    public UpdateEmailRequest(String accessToken, String newEmail) {
        this.accessToken = accessToken;
        this.newEmail = newEmail;
    }
}