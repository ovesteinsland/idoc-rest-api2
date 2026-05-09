package no.softwarecontrol.idoc.webservices.data.cognito;

import java.util.List;
import java.util.Map;

public class UserInfoResponse {
    public String username;
    public String sub;
    public String email;
    public boolean emailVerified;
    public String phoneNumber;
    public boolean phoneNumberVerified;
    public String givenName;
    public String familyName;
    public List<String> groups;
    public Map<String, String> customAttributes;

    public UserInfoResponse() {}
}