package no.softwarecontrol.idoc.filter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

public final class AuthConfig {
    public final String region;
    final String userPoolId;
    public final String appClientId;
    final boolean enableJwt;
    final boolean enableBasic;
    final Optional<String> requiredScope;
    final String roleGroup;
    public final Optional<String> appClientSecret; // NY LINJE

    private AuthConfig(String region, String userPoolId, String appClientId, boolean enableJwt, boolean enableBasic,
                       Optional<String> requiredScope, String roleGroup, Optional<String> appClientSecret) { // OPPDATERT
        this.region = region;
        this.userPoolId = userPoolId;
        this.appClientId = appClientId;
        this.enableJwt = enableJwt;
        this.enableBasic = enableBasic;
        this.requiredScope = requiredScope;
        this.roleGroup = roleGroup;
        this.appClientSecret = appClientSecret; // NY LINJE
    }


    public static AuthConfig load() {
        String region = get("COGNITO_REGION", null);
        String userPoolId = get("COGNITO_USER_POOL_ID", null);
        String appClientId = get("COGNITO_APP_CLIENT_ID", null);

        boolean enableJwt = Boolean.parseBoolean(get("AUTH_ENABLE_JWT", "true"));
        boolean enableBasic = Boolean.parseBoolean(get("AUTH_ENABLE_BASIC", "false"));
        Optional<String> requiredScope = Optional.ofNullable(get("COGNITO_REQUIRED_SCOPE", null));
        String roleGroup = get("COGNITO_ROLE_GROUP", "ApplicationRole");
        Optional<String> appClientSecret = loadClientSecretFromSecretsManager(region);

        System.out.println("=============== JWT Configuration ================");
        System.out.println("COGNITO_REGION:         " + region);
        System.out.println("COGNITO_USER_POOL_ID:   " + userPoolId);
        System.out.println("COGNITO_APP_CLIENT_ID:  " + appClientId);
        System.out.println("COGNITO_REQUIRED_SCOPE: " + requiredScope);
        System.out.println("COGNITO_ROLE_GROUP:     " + roleGroup);
        System.out.println("AUTH_ENABLE_BASIC:      " + enableBasic);
        System.out.println("CLIENT_SECRET_LOADED:   " + appClientSecret.isPresent());
        System.out.println("--------------- JWT Configuration ----------------");
        return new AuthConfig(region, userPoolId, appClientId, enableJwt, enableBasic, requiredScope, roleGroup, appClientSecret);
    }

    private static Optional<String> loadClientSecretFromSecretsManager(String region) {
        String secretName = get("COGNITO_CLIENT_SECRET_NAME", null);
        if (secretName == null || secretName.isBlank()) {
            // Ingen secret konfigurert - bruk public client
            System.out.println("COGNITO_CLIENT_SECRET_NAME ikke satt - bruker public client (ingen secret)");
            return Optional.empty();
        }

        try {
            SecretsManagerClient sm = SecretsManagerClient.builder()
                    .region(Region.of(region))
                    .build();

            GetSecretValueResponse resp = sm.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretName).build()
            );
            String secret = resp.secretString();
            System.out.println("Client secret hentet fra AWS Secrets Manager: " + secretName);
            return Optional.of(secret);
        } catch (Exception e) {
            System.out.println("FEIL ved henting av client secret fra Secrets Manager: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
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
