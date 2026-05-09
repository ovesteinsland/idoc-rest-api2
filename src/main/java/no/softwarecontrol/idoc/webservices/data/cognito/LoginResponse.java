package no.softwarecontrol.idoc.webservices.data.cognito;

public class LoginResponse {
    public String accessToken;
    public String idToken;
    public String refreshToken;
    public Integer expiresIn;
    public String tokenType;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String idToken, String refreshToken, Integer expiresIn) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = "Bearer";
    }
}