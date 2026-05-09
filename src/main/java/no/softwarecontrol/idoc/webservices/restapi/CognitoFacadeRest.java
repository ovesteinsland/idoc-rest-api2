package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.softwarecontrol.idoc.webservices.data.cognito.*;
import no.softwarecontrol.idoc.webservices.restapi.aws.CognitoService;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeResponse;

@Path("cognito")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CognitoFacadeRest {

    private final CognitoService cognitoService;

    public CognitoFacadeRest() {
        this.cognitoService = CognitoService.getInstance();
    }

    // ====================
    // LOGIN - Public
    // ====================
    @POST
    @Path("login")
    @PermitAll
    public Response login(CognitoLoginRequest request) {
        try {
            if (request.username == null || request.username.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoLoginResponse.error("Username er påkrevd"))
                        .build();
            }
            if (request.password == null || request.password.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoLoginResponse.error("Password er påkrevd"))
                        .build();
            }

            LoginResponse loginResponse = cognitoService.login(request);
            return Response.ok(CognitoLoginResponse.success(loginResponse)).build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(CognitoLoginResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(CognitoLoginResponse.error("En uventet feil oppstod"))
                    .build();
        }
    }

    // ====================
    // REGISTER - Public
    // ====================
    @POST
    @Path("register")
    @PermitAll
    public Response register(CognitoRegisterRequest request) {
        try {
            if (request.username == null || request.username.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoRegisterResponse.error("Username er påkrevd"))
                        .build();
            }
            if (request.password == null || request.password.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoRegisterResponse.error("Password er påkrevd"))
                        .build();
            }
            if (request.email == null || request.email.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoRegisterResponse.error("Email er påkrevd"))
                        .build();
            }

            RegisterResponse registerResponse = cognitoService.register(request);
            return Response.status(Response.Status.CREATED)
                    .entity(CognitoRegisterResponse.success(registerResponse))
                    .build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(CognitoRegisterResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(CognitoRegisterResponse.error("En uventet feil oppstod"))
                    .build();
        }
    }

    // ====================
    // CONFIRM EMAIL - Public (brukes etter registrering)
    // ====================
    @POST
    @Path("confirm-signup")
    @PermitAll
    public Response confirmSignup(VerificationRequest request) {
        try {
            if (request.username == null || request.username.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Username er påkrevd"))
                        .build();
            }
            if (request.code == null || request.code.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Verification code er påkrevd"))
                        .build();
            }

            cognitoService.confirmSignUp(request);
            return Response.ok()
                    .entity(new StandardResponse(true, "E-post bekreftet"))
                    .build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StandardResponse(false, e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "En uventet feil oppstod"))
                    .build();
        }
    }


    // ====================
    // CONFIRM EMAIL - Public (brukes etter registrering)
    // ====================
    @POST
    @Path("confirm-email")
    @PermitAll
    public Response confirmEmail(VerificationRequest request) {
        try {
            if (request.username == null || request.username.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Username er påkrevd"))
                        .build();
            }
            if (request.code == null || request.code.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Verification code er påkrevd"))
                        .build();
            }

            cognitoService.confirmSignUp(request);
            return Response.ok()
                    .entity(new StandardResponse(true, "E-post bekreftet"))
                    .build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StandardResponse(false, e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "En uventet feil oppstod"))
                    .build();
        }
    }

    // ====================
    // REFRESH TOKENS - Public
    // ====================
    @POST
    @Path("tokens")
    @PermitAll
    public Response refreshTokensPost(TokenRefreshRequest request) {
        try {
            if (request.refreshToken == null || request.refreshToken.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoLoginResponse.error("Refresh token er påkrevd"))
                        .build();
            }

            TokenRefreshResponse refreshResponse = cognitoService.refreshTokens(request);

            // Konverter til samme format som login
            LoginResponse loginResponse = new LoginResponse(
                    refreshResponse.accessToken,
                    refreshResponse.idToken,
                    request.refreshToken, // Bruk original refresh token
                    refreshResponse.expiresIn
            );

            return Response.ok(CognitoLoginResponse.success(loginResponse)).build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(CognitoLoginResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(CognitoLoginResponse.error("En uventet feil oppstod"))
                    .build();
        }
    }

    @POST
    @Path("refresh-token")
    @PermitAll
    public Response refreshToken(RefreshTokenOnlyRequest request) {
        try {
            if (request.refreshToken == null || request.refreshToken.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(CognitoLoginResponse.error("Refresh token er påkrevd"))
                        .build();
            }

            // Konverter til TokenRefreshRequest (uten username for public client)
            TokenRefreshRequest tokenRequest = new TokenRefreshRequest(request.refreshToken);
            TokenRefreshResponse refreshResponse = cognitoService.refreshTokens(tokenRequest);

            // Konverter til samme format som login
            // OBS: Cognito returnerer IKKE ny refreshToken ved refresh, så vi setter den til null
            LoginResponse loginResponse = new LoginResponse(
                    refreshResponse.accessToken,
                    refreshResponse.idToken,
                    null, // Cognito gir ikke ny refresh token
                    refreshResponse.expiresIn
            );

            return Response.ok(CognitoLoginResponse.success(loginResponse)).build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(CognitoLoginResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(CognitoLoginResponse.error("Token refresh feilet"))
                    .build();
        }
    }


    // ====================
    // GET USER INFO - Requires authentication
    // ====================
    @GET
    @Path("user-info")
    @RolesAllowed({"ApplicationRole"})
    public Response getUserInfo(@HeaderParam("Authorization") String authHeader) {
        try {
            String accessToken = extractBearerToken(authHeader);
            if (accessToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Authorization header er påkrevd"))
                        .build();
            }

            UserInfoResponse userInfo = cognitoService.getUserInfo(accessToken);
            return Response.ok(new StandardDataResponse<>(true, userInfo, null)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "Kunne ikke hente brukerinfo: " + e.getMessage()))
                    .build();
        }
    }

    // ====================
    // LOGOUT - Requires authentication
    // ====================
    @POST
    @Path("logout")
    @RolesAllowed({"ApplicationRole"})
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        try {
            String accessToken = extractBearerToken(authHeader);
            if (accessToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Access token er påkrevd"))
                        .build();
            }

            LogoutRequest request = new LogoutRequest();
            request.accessToken = accessToken;
            cognitoService.logout(request);
            return Response.ok()
                    .entity(new StandardResponse(true, "Logout vellykket"))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "Logout feilet: " + e.getMessage()))
                    .build();
        }
    }

    // ====================
    // CHANGE PASSWORD - Requires authentication
    // ====================
    @POST
    @Path("change-password")
    @RolesAllowed({"ApplicationRole"})
    public Response changePassword(ChangePasswordRequest request, @HeaderParam("Authorization") String authHeader) {
        try {
            String accessToken = extractBearerToken(authHeader);
            if (accessToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Access token er påkrevd"))
                        .build();
            }
            request.accessToken = accessToken;

            if (request.previousPassword == null || request.previousPassword.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Gammelt passord er påkrevd"))
                        .build();
            }
            if (request.proposedPassword == null || request.proposedPassword.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Nytt passord er påkrevd"))
                        .build();
            }

            cognitoService.changePassword(request);
            return Response.ok()
                    .entity(new StandardResponse(true, "Passord endret"))
                    .build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StandardResponse(false, e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "En uventet feil oppstod"))
                    .build();
        }
    }

    // ====================
    // UPDATE EMAIL - Requires authentication
    // ====================
    @POST
    @Path("update-email")
    @RolesAllowed({"ApplicationRole"})
    public Response updateEmail(UpdateEmailRequest request, @HeaderParam("Authorization") String authHeader) {
        try {
            String accessToken = extractBearerToken(authHeader);
            if (accessToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Access token er påkrevd"))
                        .build();
            }
            request.accessToken = accessToken;

            if (request.newEmail == null || request.newEmail.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Ny e-post er påkrevd"))
                        .build();
            }

            cognitoService.updateEmail(request);
            return Response.ok()
                    .entity(new StandardResponse(true, "E-post oppdatert. Sjekk e-post for verifiseringskode."))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "E-postoppdatering feilet: " + e.getMessage()))
                    .build();
        }
    }

    // ====================
    // SEND VERIFICATION CODE - Requires authentication
    // ====================
    @POST
    @Path("send-verification-code")
    @RolesAllowed({"ApplicationRole"})
    public Response sendVerificationCode(SendVerificationCodeRequest request, @HeaderParam("Authorization") String authHeader) {
        try {
            String accessToken = extractBearerToken(authHeader);
            if (accessToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Access token er påkrevd"))
                        .build();
            }
            request.accessToken = accessToken;

            if (request.attributeName == null || request.attributeName.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new StandardResponse(false, "Attribute name er påkrevd (email eller phone_number)"))
                        .build();
            }

            GetUserAttributeVerificationCodeResponse userAttributeVerificationCodeResponse = cognitoService.sendVerificationCode(request.accessToken, request.attributeName);
            return Response.ok()
                    .entity(new StandardResponse(true, "Verifikasjonskode sendt"))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new StandardResponse(false, "Kunne ikke sende verifikasjonskode: " + e.getMessage()))
                    .build();
        }
    }

    // ====================
    // Helper method to extract Bearer token
    // ====================
    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    // ====================
    // Standard response wrapper
    // ====================
    public static class StandardResponse {
        public boolean success;
        public String message;

        public StandardResponse() {}

        public StandardResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // ====================
    // Standard response wrapper with data
    // ====================
    public static class StandardDataResponse<T> {
        public boolean success;
        public T data;
        public String message;

        public StandardDataResponse() {}

        public StandardDataResponse(boolean success, T data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }
    }

    // ====================
    // Helper DTO for send verification code
    // ====================
    public static class SendVerificationCodeRequest {
        public String accessToken;
        public String attributeName; // "email" eller "phone_number"

        public SendVerificationCodeRequest() {}
    }
}