// Java
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import com.google.cloud.storage.HttpMethod;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Path("google.cloud.storage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@PermitAll
@RolesAllowed({"ApplicationRole"})
public class GcsSignedUrlResource {

    // Tillatte prefiks i objektstien
    private static final Set<String> ALLOWED_PREFIXES = Set.of("software-control/");
    private static final int DEFAULT_EXPIRES_MIN = 15;
    private static final int MAX_EXPIRES_MIN = 60;

    // Miljøvariabler
    private static final String ENV_SECRET_NAME = "GCP_SA_SECRET_NAME";
    private static final String ENV_AWS_REGION  = "COGNITO_REGION";

    // Cache av Storage-klient
    private volatile Storage storage;

    @POST
    @Path("/signed-url")
    public Response createSignedUrl(SignedUrlRequest req) {
        List<String> errors = validate(req);
        if (!errors.isEmpty()) {
            return badRequest(String.join("; ", errors));
        }

        try {
            Storage storage = getOrCreateStorage();

            String bucket = req.bucket.trim();
            String objectName = req.objectName.trim();
            HttpMethod httpMethod = HttpMethod.valueOf(req.method.trim().toUpperCase(Locale.ROOT));

            int expires = (req.expiresMinutes == null) ? DEFAULT_EXPIRES_MIN : req.expiresMinutes;
            expires = Math.min(Math.max(1, expires), MAX_EXPIRES_MIN);

            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectName)).build();

            List<Storage.SignUrlOption> opts = new ArrayList<>();
            opts.add(Storage.SignUrlOption.httpMethod(httpMethod));
            opts.add(Storage.SignUrlOption.withV4Signature());

            if (httpMethod == HttpMethod.PUT) {
                String contentType = req.contentType.trim();
                opts.add(Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType)));
            }

            URL url = storage.signUrl(blobInfo, expires, TimeUnit.MINUTES, opts.toArray(new Storage.SignUrlOption[0]));
            System.out.println("signing url: " + url);
            return Response.ok(new SignedUrlResponse(url.toString())).build();

        } catch (StorageException se) {
            return badRequest("GCS-feil: " + safeMsg(se));
        } catch (IllegalArgumentException iae) {
            return badRequest("Ugyldig forespørsel: " + safeMsg(iae));
        } catch (Exception e) {
            return serverError("Kunne ikke generere signed URL: " + safeMsg(e));
        }
    }

    private List<String> validate(SignedUrlRequest req) {
        List<String> errors = new ArrayList<>();
        if (req == null) {
            errors.add("Body kan ikke være tom.");
            return errors;
        }
        if (isBlank(req.bucket)) errors.add("bucket er påkrevd.");
        if (isBlank(req.objectName)) errors.add("objectName er påkrevd.");
        if (isBlank(req.method)) errors.add("method er påkrevd (PUT eller GET).");

        if (!isBlank(req.method)) {
            String m = req.method.trim().toUpperCase(Locale.ROOT);
            if (!m.equals("PUT") && !m.equals("GET")) {
                errors.add("method må være PUT eller GET.");
            }
            if (m.equals("PUT") && isBlank(req.contentType)) {
                errors.add("contentType er påkrevd for PUT.");
            }
        }

        if (!isBlank(req.objectName) && !hasAllowedPrefix(req.objectName)) {
            errors.add("objectName må starte med et tillatt prefiks: " + ALLOWED_PREFIXES);
        }

        if (req.expiresMinutes != null) {
            if (req.expiresMinutes < 1 || req.expiresMinutes > MAX_EXPIRES_MIN) {
                errors.add("expiresMinutes må være mellom 1 og " + MAX_EXPIRES_MIN + ".");
            }
        }
        return errors;
    }

    private boolean hasAllowedPrefix(String name) {
        for (String p : ALLOWED_PREFIXES) {
            if (name.startsWith(p)) return true;
        }
        return false;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", msg)).build();
    }

    private Response serverError(String msg) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", msg)).build();
    }

    private String safeMsg(Throwable t) {
        return (t.getMessage() == null || t.getMessage().isBlank()) ? t.getClass().getSimpleName() : t.getMessage();
    }

    // Lazy-init av Storage-klient med SA-JSON fra AWS Secrets Manager
    private Storage getOrCreateStorage() throws Exception {
        if (storage != null) return storage;
        synchronized (this) {
            if (storage != null) return storage;

            String secretName = System.getProperty(ENV_SECRET_NAME);
            if (secretName == null || secretName.isBlank()) secretName = System.getenv(ENV_SECRET_NAME);
            if (isBlank(secretName)) {
                throw new IllegalStateException("Miljøvariabel " + ENV_SECRET_NAME + " mangler.");
            }
            String regionEnv = System.getProperty(ENV_AWS_REGION);
            if (regionEnv == null || regionEnv.isBlank()) regionEnv = System.getenv(ENV_AWS_REGION);
            if (isBlank(regionEnv)) {
                throw new IllegalStateException("Miljøvariabel " + ENV_AWS_REGION + " mangler.");
            }

            Region region = Region.of(regionEnv);

            SecretsManagerClient sm = SecretsManagerClient.builder()
                    .region(region)
                    .build();

            GetSecretValueResponse resp = sm.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretName).build()
            );
            String saJson = resp.secretString();

            ServiceAccountCredentials creds = ServiceAccountCredentials.fromStream(
                    new ByteArrayInputStream(saJson.getBytes(StandardCharsets.UTF_8))
            );
            storage = StorageOptions.newBuilder()
                    .setCredentials(creds)
                    .build()
                    .getService();

            return storage;
        }
    }

    // DTOer
    public static class SignedUrlRequest {
        public String bucket;
        public String objectName;
        public String method;          // "PUT" eller "GET"
        public String contentType;     // påkrevd for PUT
        public Integer expiresMinutes; // valgfri, default 15, maks 60
    }

    public static class SignedUrlResponse {
        public String signedUrl;
        public SignedUrlResponse(String signedUrl) { this.signedUrl = signedUrl; }
    }
}