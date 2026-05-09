package no.softwarecontrol.idoc.webservices.data.cognito;

public class RegisterResponse {
    public String userSub;
    public boolean userConfirmed;
    public String message;
    public String codeDeliveryDestination; // Hvor verifikasjonskode ble sendt

    public RegisterResponse() {}

    public RegisterResponse(String userSub, boolean userConfirmed, String message) {
        this.userSub = userSub;
        this.userConfirmed = userConfirmed;
        this.message = message;
    }
}