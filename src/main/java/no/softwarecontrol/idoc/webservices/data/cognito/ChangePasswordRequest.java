package no.softwarecontrol.idoc.webservices.data.cognito;

public class ChangePasswordRequest {
    public String accessToken;
    public String previousPassword;
    public String proposedPassword;

    public ChangePasswordRequest() {}

    public ChangePasswordRequest(String accessToken, String previousPassword, String proposedPassword) {
        this.accessToken = accessToken;
        this.previousPassword = previousPassword;
        this.proposedPassword = proposedPassword;
    }
}
