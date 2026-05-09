package no.softwarecontrol.idoc.webservices.data.cognito;

public class CognitoLoginResponse {
    public boolean success;
    public TokenData tokens;
    public String message;

    public CognitoLoginResponse() {}

    public CognitoLoginResponse(boolean success, TokenData tokens, String message) {
        this.success = success;
        this.tokens = tokens;
        this.message = message;
    }

    public static CognitoLoginResponse success(LoginResponse loginResponse) {
        TokenData tokens = new TokenData(
                loginResponse.accessToken,
                loginResponse.idToken,
                loginResponse.refreshToken, // kan være null ved token refresh
                loginResponse.expiresIn,
                loginResponse.tokenType
        );
        return new CognitoLoginResponse(true, tokens, null);
    }

    public static CognitoLoginResponse error(String message) {
        return new CognitoLoginResponse(false, null, message);
    }

    public static class TokenData {
        public String accessToken;
        public String idToken;
        public String refreshToken; // null ved token refresh
        public Integer expiresIn;
        public String tokenType;

        public TokenData() {}

        public TokenData(String accessToken, String idToken, String refreshToken, Integer expiresIn, String tokenType) {
            this.accessToken = accessToken;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
        }
    }
}