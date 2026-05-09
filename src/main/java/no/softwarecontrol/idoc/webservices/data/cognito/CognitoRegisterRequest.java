package no.softwarecontrol.idoc.webservices.data.cognito;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;


public class CognitoRegisterRequest {
    public String username;
    public String password;
    public String email;
    public String mobile;  // Valgfritt, format: +4712345678
    public String firstname;    // Fornavn
    public String lastname;
    public String locale;
    public Map<String, String> customAttributes; // Valgfritt, custom attributter

    public CognitoRegisterRequest() {}
}
