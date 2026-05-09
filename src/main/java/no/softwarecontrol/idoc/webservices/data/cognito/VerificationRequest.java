package no.softwarecontrol.idoc.webservices.data.cognito;

public class VerificationRequest {
    public String username;
    public String code;
    public String attributeName; // "email" eller "phone_number"

    public VerificationRequest() {}

    public VerificationRequest(String username, String code, String attributeName) {
        this.username = username;
        this.code = code;
        this.attributeName = attributeName;
    }
}