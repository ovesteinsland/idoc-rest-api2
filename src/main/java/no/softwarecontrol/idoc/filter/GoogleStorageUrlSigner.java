package no.softwarecontrol.idoc.filter;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GoogleStorageUrlSigner {

    private final Storage storage;

    public GoogleStorageUrlSigner(String secretName, Region region) throws Exception {
        // 1) Hent SA-JSON fra Secrets Manager
        SecretsManagerClient sm = SecretsManagerClient.builder()
                .region(region)
                .build();
        GetSecretValueResponse resp = sm.getSecretValue(
                GetSecretValueRequest.builder().secretId(secretName).build()
        );
        String saJson = resp.secretString();

        // 2) Lag GCP Storage-klient med SA-JSON
        ServiceAccountCredentials creds = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(saJson.getBytes(StandardCharsets.UTF_8))
        );
        this.storage = StorageOptions.newBuilder()
                .setCredentials(creds)
                .build()
                .getService();
    }

    // Enkel signeringsmetode (PUT/GET). For PUT kreves contentType.
    public String signV4(String bucket, String objectName, String method, String contentType, int expiresMinutes) {
        if (!objectName.startsWith("software-control/")) {
            throw new IllegalArgumentException("objectName må starte med software-control/");
        }
        int exp = Math.min(60, expiresMinutes <= 0 ? 15 : expiresMinutes);
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));

        BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, objectName)).build();
        List<Storage.SignUrlOption> opts = new ArrayList<>();
        opts.add(Storage.SignUrlOption.httpMethod(httpMethod));
        opts.add(Storage.SignUrlOption.withV4Signature());

        if (httpMethod == HttpMethod.PUT) {
            if (contentType == null || contentType.isBlank()) {
                throw new IllegalArgumentException("contentType er påkrevd for PUT");
            }
            opts.add(Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType)));
        }

        URL url = storage.signUrl(info, exp, TimeUnit.MINUTES, opts.toArray(new Storage.SignUrlOption[0]));
        return url.toString();
    }


    // Use this code snippet in your app.
// If you need more information about configurations or implementing the sample
// code, visit the AWS docs:
// https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html

// Make sure to import the following packages in your code
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
// import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
// import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

//    public static void getSecret() {
//
//        String secretName = "gcp/service-account/storage-signer";
//        Region region = Region.of("eu-central-1");
//
//        // Create a Secrets Manager client
//        SecretsManagerClient client = SecretsManagerClient.builder()
//                .region(region)
//                .build();
//
//        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
//                .secretId(secretName)
//                .build();
//
//        GetSecretValueResponse getSecretValueResponse;
//
//        try {
//            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
//        } catch (Exception e) {
//            // For a list of exceptions thrown, see
//            // https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
//            throw e;
//        }
//
//        String secret = getSecretValueResponse.secretString();
//
//        // Your code goes here.
//    }
}
