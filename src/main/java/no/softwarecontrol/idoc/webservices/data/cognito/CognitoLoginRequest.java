package no.softwarecontrol.idoc.webservices.data.cognito;

public class CognitoLoginRequest {
    public String username;
    public String password;

    public CognitoLoginRequest() {}

    public CognitoLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
