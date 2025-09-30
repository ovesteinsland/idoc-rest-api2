package no.softwarecontrol.idoc.filter;
import java.util.Optional;

final class AuthConfig {
    final String region;
    final String userPoolId;
    final String appClientId;
    final boolean enableJwt;
    final boolean enableBasic;
    final Optional<String> requiredScope;
    final String roleGroup;

    private AuthConfig(String region, String userPoolId, String appClientId, boolean enableJwt, boolean enableBasic,
                       Optional<String> requiredScope, String roleGroup) {
        this.region = region;
        this.userPoolId = userPoolId;
        this.appClientId = appClientId;
        this.enableJwt = enableJwt;
        this.enableBasic = enableBasic;
        this.requiredScope = requiredScope;
        this.roleGroup = roleGroup;
    }

    static AuthConfig load() {
        String region = get("COGNITO_REGION", null);
        String userPoolId = get("COGNITO_USER_POOL_ID", null);
        String appClientId = get("COGNITO_APP_CLIENT_ID", null);

        boolean enableJwt = Boolean.parseBoolean(get("AUTH_ENABLE_JWT", "true"));
        boolean enableBasic = Boolean.parseBoolean(get("AUTH_ENABLE_BASIC", "false"));
        Optional<String> requiredScope = Optional.ofNullable(get("COGNITO_REQUIRED_SCOPE", null));
        String roleGroup = get("COGNITO_ROLE_GROUP", "ApplicationRole");

        System.out.println("=============== JWT Configuration ================");
        System.out.println("COGNITO_REGION:         " + region);
        System.out.println("COGNITO_USER_POOL_ID:   " + userPoolId);
        System.out.println("COGNITO_APP_CLIENT_ID:  " + appClientId);
        System.out.println("COGNITO_REQUIRED_SCOPE: " + requiredScope);
        System.out.println("COGNITO_ROLE_GROUP:     " + roleGroup);
        System.out.println("--------------- JWT Configuration ----------------");
        return new AuthConfig(region, userPoolId, appClientId, enableJwt, enableBasic, requiredScope, roleGroup);
    }

    private static String get(String key, String def) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        if (v == null || v.isBlank()) v = def;
        return v;
    }

    String issuer() {
        return "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
    }

    String jwksUrl() {
        return issuer() + "/.well-known/jwks.json";
    }

}
