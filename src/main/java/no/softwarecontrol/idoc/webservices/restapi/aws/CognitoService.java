package no.softwarecontrol.idoc.webservices.restapi.aws;

import no.softwarecontrol.idoc.filter.AuthConfig;
import no.softwarecontrol.idoc.webservices.data.cognito.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CognitoService {
    private static CognitoService INSTANCE;

    private final AuthConfig config;
    private final CognitoIdentityProviderClient cognitoClient;

    private CognitoService() {
        this.config = AuthConfig.load();
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(config.region))
                .build();
    }

    public static synchronized CognitoService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CognitoService();
        }
        return INSTANCE;
    }

    // ==================== LOGIN ====================
    public LoginResponse login(CognitoLoginRequest request) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", request.username);
            authParams.put("PASSWORD", request.password);

            // Legg til SECRET_HASH hvis app har client secret
            if (config.appClientSecret.isPresent()) {
                String secretHash = calculateSecretHash(request.username, config.appClientId, config.appClientSecret.get());
                authParams.put("SECRET_HASH", secretHash);
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(config.appClientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            AuthenticationResultType result = authResponse.authenticationResult();
            if (result == null) {
                throw new RuntimeException("Authentication failed: No result returned");
            }

            return new LoginResponse(
                    result.accessToken(),
                    result.idToken(),
                    result.refreshToken(),
                    result.expiresIn()
            );

        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Ugyldig brukernavn eller passord", e);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("Bruker ikke funnet", e);
        } catch (Exception e) {
            throw new RuntimeException("Login feilet: " + e.getMessage(), e);
        }
    }

    // ==================== REGISTER ====================
    public RegisterResponse register(CognitoRegisterRequest request) {
        try {
            List<AttributeType> attributes = new ArrayList<>();
            attributes.add(AttributeType.builder().name("preferred_username").value(request.username).build());
            attributes.add(AttributeType.builder().name("email").value(request.email).build());

            if (request.firstname != null && !request.firstname.isBlank()) {
                attributes.add(AttributeType.builder().name("given_name").value(request.firstname).build());
            }
            if (request.firstname != null && !request.firstname.isBlank()) {
                attributes.add(AttributeType.builder().name("middle_name").value("X").build());
            }
            if (request.lastname != null && !request.lastname.isBlank()) {
                attributes.add(AttributeType.builder().name("family_name").value(request.lastname).build());
            }
            if (request.mobile != null && !request.mobile.isBlank()) {
                attributes.add(AttributeType.builder().name("phone_number").value(request.mobile).build());
            }
            if (request.mobile != null && !request.mobile.isBlank()) {
                attributes.add(AttributeType.builder().name("locale").value(request.locale).build());
            }


            // Custom attributes
            if (request.customAttributes != null) {
                request.customAttributes.forEach((key, value) ->
                        attributes.add(AttributeType.builder().name(key).value(value).build())
                );
            }

            SignUpRequest.Builder signUpBuilder = SignUpRequest.builder()
                    .clientId(config.appClientId)
                    .username(request.username)
                    .password(request.password)
                    .userAttributes(attributes);

            // Legg til SECRET_HASH hvis nødvendig
            if (config.appClientSecret.isPresent()) {
                String secretHash = calculateSecretHash(request.username, config.appClientId, config.appClientSecret.get());
                signUpBuilder.secretHash(secretHash);
            }

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpBuilder.build());

            RegisterResponse response = new RegisterResponse(
                    signUpResponse.userSub(),
                    signUpResponse.userConfirmed(),
                    "Bruker opprettet. Sjekk e-post for verifiseringskode."
            );

            if (signUpResponse.codeDeliveryDetails() != null) {
                response.codeDeliveryDestination = signUpResponse.codeDeliveryDetails().destination();
            }

            return response;

        } catch (UsernameExistsException e) {
            throw new RuntimeException("Brukernavn eksisterer allerede", e);
        } catch (InvalidPasswordException e) {
            throw new RuntimeException("Passord oppfyller ikke kravene", e);
        } catch (Exception e) {
            throw new RuntimeException("Registrering feilet: " + e.getMessage(), e);
        }
    }

    // ==================== LOGOUT ====================
    public void logout(LogoutRequest request) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(request.accessToken)
                    .build();

            cognitoClient.globalSignOut(signOutRequest);

        } catch (Exception e) {
            throw new RuntimeException("Logout feilet: " + e.getMessage(), e);
        }
    }


    // ==================== CHANGE PASSWORD ====================
    public void changePassword(no.softwarecontrol.idoc.webservices.data.cognito.ChangePasswordRequest request) {
        try {
            software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest cognitoRequest =
                    software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest.builder()
                            .accessToken(request.accessToken)
                            .previousPassword(request.previousPassword)
                            .proposedPassword(request.proposedPassword)
                            .build();

            cognitoClient.changePassword(cognitoRequest);

        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Ugyldig gammelt passord", e);
        } catch (InvalidPasswordException e) {
            throw new RuntimeException("Nytt passord oppfyller ikke kravene", e);
        } catch (Exception e) {
            throw new RuntimeException("Passordendring feilet: " + e.getMessage(), e);
        }
    }

    // ==================== UPDATE EMAIL ====================
    public void updateEmail(UpdateEmailRequest request) {
        try {
            List<AttributeType> attributes = new ArrayList<>();
            attributes.add(AttributeType.builder().name("email").value(request.newEmail).build());

            UpdateUserAttributesRequest updateRequest = UpdateUserAttributesRequest.builder()
                    .accessToken(request.accessToken)
                    .userAttributes(attributes)
                    .build();

            cognitoClient.updateUserAttributes(updateRequest);

        } catch (Exception e) {
            throw new RuntimeException("E-postoppdatering feilet: " + e.getMessage(), e);
        }
    }

    // ==================== SEND VERIFICATION CODE ====================
    public GetUserAttributeVerificationCodeResponse sendVerificationCode(String accessToken, String attributeName) {
        try {
            GetUserAttributeVerificationCodeRequest verificationRequest =
                    GetUserAttributeVerificationCodeRequest.builder()
                            .accessToken(accessToken)
                            .attributeName(attributeName)
                            .build();

           return cognitoClient.getUserAttributeVerificationCode(verificationRequest);

        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke sende verifikasjonskode: " + e.getMessage(), e);
        }
    }

    // ==================== CONFIRM SIGNUP ====================
    public ConfirmSignUpResponse confirmSignUp(VerificationRequest request) {
        try {
            ConfirmSignUpRequest.Builder confirmBuilder = ConfirmSignUpRequest.builder()
                    .clientId(config.appClientId)
                    .username(request.username)
                    .confirmationCode(request.code);

            // Legg til SECRET_HASH hvis nødvendig
            if (config.appClientSecret.isPresent()) {
                String secretHash = calculateSecretHash(request.username, config.appClientId, config.appClientSecret.get());
                confirmBuilder.secretHash(secretHash);
            }
            return cognitoClient.confirmSignUp(confirmBuilder.build());

        } catch (CodeMismatchException e) {
            throw new RuntimeException("Ugyldig verifikasjonskode", e);
        } catch (ExpiredCodeException e) {
            throw new RuntimeException("Verifikasjonskode utløpt", e);
        } catch (Exception e) {
            throw new RuntimeException("E-postverifisering feilet: " + e.getMessage(), e);
        }
    }

    // ==================== REFRESH TOKENS ====================
    public TokenRefreshResponse refreshTokens(TokenRefreshRequest request) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", request.refreshToken);

            // SECRET_HASH: Hvis app har client secret OG username er gitt
            if (config.appClientSecret.isPresent() && request.username != null && !request.username.isBlank()) {
                String secretHash = calculateSecretHash(request.username, config.appClientId, config.appClientSecret.get());
                authParams.put("SECRET_HASH", secretHash);
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(config.appClientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            AuthenticationResultType result = authResponse.authenticationResult();
            if (result == null) {
                throw new RuntimeException("Token refresh failed: No result returned");
            }

            return new TokenRefreshResponse(
                    result.accessToken(),
                    result.idToken(),
                    result.expiresIn()
            );

        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Ugyldig refresh token", e);
        } catch (Exception e) {
            throw new RuntimeException("Token refresh feilet: " + e.getMessage(), e);
        }
    }


    // ==================== GET USER INFO ====================
    public UserInfoResponse getUserInfo(String accessToken) {
        try {
            GetUserRequest getUserRequest = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse getUserResponse = cognitoClient.getUser(getUserRequest);

            UserInfoResponse response = new UserInfoResponse();
            response.username = getUserResponse.username();

            // Parse attributes
            Map<String, String> customAttrs = new HashMap<>();
            for (AttributeType attr : getUserResponse.userAttributes()) {
                switch (attr.name()) {
                    case "sub":
                        response.sub = attr.value();
                        break;
                    case "email":
                        response.email = attr.value();
                        break;
                    case "email_verified":
                        response.emailVerified = Boolean.parseBoolean(attr.value());
                        break;
                    case "phone_number":
                        response.phoneNumber = attr.value();
                        break;
                    case "phone_number_verified":
                        response.phoneNumberVerified = Boolean.parseBoolean(attr.value());
                        break;
                    case "given_name":
                        response.givenName = attr.value();
                        break;
                    case "family_name":
                        response.familyName = attr.value();
                        break;
                    default:
                        if (attr.name().startsWith("custom:")) {
                            customAttrs.put(attr.name(), attr.value());
                        }
                        break;
                }
            }

            response.customAttributes = customAttrs;

            // TODO: Hent grupper hvis nødvendig (krever admin-kall)
            response.groups = new ArrayList<>();

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke hente brukerinfo: " + e.getMessage(), e);
        }
    }

    // ==================== HELPER: Calculate SECRET_HASH ====================
    private String calculateSecretHash(String username, String clientId, String clientSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] raw = mac.doFinal((username + clientId).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke beregne SECRET_HASH", e);
        }
    }

    // Cleanup
    public void close() {
        if (cognitoClient != null) {
            cognitoClient.close();
        }
    }
}