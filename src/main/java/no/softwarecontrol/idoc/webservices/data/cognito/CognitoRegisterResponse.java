package no.softwarecontrol.idoc.webservices.data.cognito;

public class CognitoRegisterResponse {
    public boolean success;
    public RegistrationData data;
    public String message;

    public CognitoRegisterResponse() {}

    public CognitoRegisterResponse(boolean success, RegistrationData data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static CognitoRegisterResponse success(RegisterResponse registerResponse) {
        RegistrationData data = new RegistrationData(
                registerResponse.userSub,
                registerResponse.userConfirmed,
                registerResponse.userConfirmed ? "COMPLETE" : "CONFIRM_EMAIL",
                registerResponse.codeDeliveryDestination
        );
        return new CognitoRegisterResponse(true, data, registerResponse.message);
    }

    public static CognitoRegisterResponse error(String message) {
        return new CognitoRegisterResponse(false, null, message);
    }

    public static class RegistrationData {
        public String userSub;
        public boolean isComplete;
        public String nextStep; // "COMPLETE" eller "CONFIRM_EMAIL"
        public String codeDeliveryDestination;

        public RegistrationData() {}

        public RegistrationData(String userSub, boolean isComplete, String nextStep, String codeDeliveryDestination) {
            this.userSub = userSub;
            this.isComplete = isComplete;
            this.nextStep = nextStep;
            this.codeDeliveryDestination = codeDeliveryDestination;
        }
    }
}