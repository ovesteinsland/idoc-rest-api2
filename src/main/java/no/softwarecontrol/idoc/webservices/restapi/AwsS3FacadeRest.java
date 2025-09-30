package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Path("aws.s3.storage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ApplicationRole"})
public class AwsS3FacadeRest {

    //private static final String BUCKET_NAME = "api-thermidoc-storage";

    private DefaultAwsRegionProviderChain regionProvider = DefaultAwsRegionProviderChain.builder().build();

    private S3Presigner presigner = S3Presigner.builder()
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .region(resolveRegion())
            .build();

    private Region resolveRegion() {
        Region region = regionProvider.getRegion();
        if (region == null) {
            // Som fallback kan du hardkode region her om ønskelig:
            // return Region.EU_NORTH_1;
            throw new IllegalStateException("AWS region kunne ikke bestemmes fra DefaultAwsRegionProviderChain");
        }
        return region;
    }

    // ==========================
    // Upload (PUT) presigned URL
    // ==========================
    @POST
    @Path("upload")
    public Response presignUpload(UploadRequest req) {
        String key = required(req.key, "key");
        long expires = req.expiresInSeconds != null ? req.expiresInSeconds : 600L; // default 10 min
        if (expires <= 0 || expires > 3600) { // SDK v2 anbefaler <= 1 time
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("expiresInSeconds må være mellom 1 og 3600")).build();
        }

        PutObjectRequest.Builder put = PutObjectRequest.builder()
                .bucket(req.bucket)
                .key(key);

        if (req.contentType != null && !req.contentType.isBlank()) {
            put = put.contentType(req.contentType);
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .putObjectRequest(put.build())
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        // Merk: signedHeaders() er Map<String, List<String>>
        Map<String, List<String>> headers = presigned.signedHeaders();
        PresignResponse resp = new PresignResponse(presigned.url().toString(), headers, expires);
        return Response.ok(resp).build();
    }

    // ============================
    // Download (GET) presigned URL
    // ============================
    @POST
    @Path("download")
    public Response presignDownload(DownloadRequest req) {
        String key = required(req.key, "key");
        long expires = req.expiresInSeconds != null ? req.expiresInSeconds : 600L; // default 10 min
        if (expires <= 0 || expires > 604800) { // S3 GET kan være lengre; holder oss til <= 7 dager
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("expiresInSeconds må være mellom 1 og 604800")).build();
        }

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(req.bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

        // Merk: signedHeaders() er Map<String, List<String>>
        Map<String, List<String>> headers = presigned.signedHeaders();
        PresignResponse resp = new PresignResponse(presigned.url().toString(), headers, expires);
        return Response.ok(resp).build();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " er påkrevd");
        }
        return value;
    }

    private static Map<String, String> error(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("error", msg);
        return m;
    }

    // ====== DTO-er ======
    public static class UploadRequest {
        public String bucket = "api-thermidoc-storage";
        public String key;                // Objekt-nøkkel i S3 (f.eks. company123/reports/file.pdf)
        public String contentType;        // Valgfritt: f.eks. application/pdf eller image/png
        public Long expiresInSeconds;     // Valgfritt: default 600 (10 min)
    }

    public static class DownloadRequest {
        public String bucket = "api-thermidoc-storage";
        public String key;                // Objekt-nøkkel i S3
        public Long expiresInSeconds;     // Valgfritt: default 600 (10 min)
    }

    public static class PresignResponse {
        public String url;
        public Map<String, List<String>> headers;
        public long expiresInSeconds;

        public PresignResponse() {}

        public PresignResponse(String url, Map<String, List<String>> headers, long expiresInSeconds) {
            this.url = url;
            this.headers = headers;
            this.expiresInSeconds = expiresInSeconds;
        }
    }
}
