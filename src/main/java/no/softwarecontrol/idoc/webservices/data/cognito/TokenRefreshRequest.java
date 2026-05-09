package no.softwarecontrol.idoc.webservices.data.cognito;

public class TokenRefreshRequest {
    public String refreshToken;
    public String username;

    public TokenRefreshRequest() {}

    public TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }


    public TokenRefreshRequest(String refreshToken, String username) {
        this.refreshToken = refreshToken;
        this.username = username;
    }

}