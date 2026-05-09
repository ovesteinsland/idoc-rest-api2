package no.softwarecontrol.idoc.webservices.data.cognito;

public class TokenRefreshResponse {
    public String accessToken;
    public String idToken;
    public Integer expiresIn;
    public String tokenType;

    public TokenRefreshResponse() {}

    public TokenRefreshResponse(String accessToken, String idToken, Integer expiresIn) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.expiresIn = expiresIn;
        this.tokenType = "Bearer";
    }
}
