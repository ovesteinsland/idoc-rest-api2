package no.softwarecontrol.idoc.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enkel JWKS-provider med cache og kid-rotasjon.
 */
final class CognitoJwksProvider {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String jwksUrl;
    private volatile Map<String, RSAPublicKey> cache = new ConcurrentHashMap<>();
    private volatile long lastRefreshEpochMs = 0L;
    private static final long REFRESH_INTERVAL_MS = Duration.ofMinutes(15).toMillis();

    CognitoJwksProvider(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }

    RSAPublicKey getKey(String kid) throws Exception {
        RSAPublicKey k = cache.get(kid);
        if (k != null) return k;

        // Prøv refresh ved ukjent kid
        refresh();
        k = cache.get(kid);
        if (k != null) return k;

        // Force-refetch hvis fortsatt ikke funnet
        forceRefresh();
        k = cache.get(kid);
        if (k != null) return k;

        throw new IllegalStateException("JWKS: ukjent kid: " + kid);
    }

    synchronized void refresh() throws Exception {
        long now = System.currentTimeMillis();
        if (now - lastRefreshEpochMs < REFRESH_INTERVAL_MS && !cache.isEmpty()) return;
        fetchAndPopulate();
    }

    synchronized void forceRefresh() throws Exception {
        fetchAndPopulate();
    }

    private void fetchAndPopulate() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(jwksUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Kunne ikke hente JWKS: " + resp.statusCode());
        }
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray keys = obj.getAsJsonArray("keys");
        Map<String, RSAPublicKey> newMap = new ConcurrentHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            JsonObject k = keys.get(i).getAsJsonObject();
            if (!"RSA".equals(k.get("kty").getAsString())) continue;
            String kid = k.get("kid").getAsString();
            String n = k.get("n").getAsString();
            String e = k.get("e").getAsString();
            RSAPublicKey pk = toPublicKey(n, e);
            newMap.put(kid, pk);
        }
        cache = newMap;
        lastRefreshEpochMs = System.currentTimeMillis();
    }

    private static RSAPublicKey toPublicKey(String nB64u, String eB64u) throws Exception {
        Base64.Decoder urlDecoder = Base64.getUrlDecoder();
        BigInteger n = new BigInteger(1, urlDecoder.decode(nB64u));
        BigInteger e = new BigInteger(1, urlDecoder.decode(eB64u));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(spec);
        return (RSAPublicKey) key;
    }
}

