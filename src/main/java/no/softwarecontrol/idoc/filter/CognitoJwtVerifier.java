package no.softwarecontrol.idoc.filter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

final class CognitoJwtVerifier {

    static final class Result {
        final String subject;
        final Set<String> groups;
        final Set<String> scopes;
        final Map<String, Object> claims;

        Result(String subject, Set<String> groups, Set<String> scopes, Map<String, Object> claims) {
            this.subject = subject;
            this.groups = groups;
            this.scopes = scopes;
            this.claims = claims;
        }
    }

    private final AuthConfig cfg;
    private final CognitoJwksProvider jwks;

    CognitoJwtVerifier(AuthConfig cfg) {
        this.cfg = cfg;
        this.jwks = new CognitoJwksProvider(cfg.jwksUrl());
    }

    Result verifyAccessToken(String bearerToken) throws Exception {
        String token = stripBearer(bearerToken);
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Ugyldig JWT-format");

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);

        JsonObject header = JsonParser.parseString(headerJson).getAsJsonObject();
        JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();

        String alg = header.get("alg").getAsString();
        if (!"RS256".equals(alg)) throw new SecurityException("Ugyldig alg: " + alg);

        String kid = header.get("kid").getAsString();
        RSAPublicKey key = jwks.getKey(kid);

        // Verifiser signatur
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(key);
        sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        if (!sig.verify(signature)) {
            // Prøv en force refresh ved falsk, i tilfelle key-rotasjon
            jwks.forceRefresh();
            key = jwks.getKey(kid);
            sig.initVerify(key);
            sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!sig.verify(signature)) {
                throw new SecurityException("Signatur verifikasjon feilet");
            }
        }

        // Standard-claims
        long now = Instant.now().getEpochSecond();
        long exp = payload.get("exp").getAsLong();
        if (now >= exp) throw new SecurityException("Token utløpt");
        if (payload.has("nbf")) {
            long nbf = payload.get("nbf").getAsLong();
            if (now < nbf) throw new SecurityException("Token ikke gyldig ennå");
        }

        String iss = payload.get("iss").getAsString();
        if (!cfg.issuer().equals(iss)) throw new SecurityException("Ugyldig issuer");

        String tokenUse = payload.has("token_use") ? payload.get("token_use").getAsString() : null;
        if (!"access".equals(tokenUse)) throw new SecurityException("Token må være access token");

        // client_id (for access token). For ID token ville 'aud' gjelde.
        if (payload.has("client_id")) {
            String clientId = payload.get("client_id").getAsString();
            if (!cfg.appClientId.equals(clientId)) throw new SecurityException("Ugyldig client_id");
        } else {
            throw new SecurityException("Mangler client_id i access token");
        }

        // scopes (mellomrom-separert)
        Set<String> scopes = new HashSet<>();
        if (payload.has("scope")) {
            String scopeStr = payload.get("scope").getAsString();
            scopes.addAll(Arrays.asList(scopeStr.split("\\s+")));
        }

        // grupper (cognito:groups)
        Set<String> groups = new HashSet<>();
        if (payload.has("cognito:groups")) {
            payload.getAsJsonArray("cognito:groups").forEach(e -> groups.add(e.getAsString()));
        }

        // Påkrevd scope (valgfri config)
        if (cfg.requiredScope.isPresent() && !scopes.contains(cfg.requiredScope.get())) {
            throw new MissingScopeException("Mangler påkrevd scope: " + cfg.requiredScope.get());
        }

        String sub = payload.has("sub") ? payload.get("sub").getAsString() : "unknown";
        Map<String, Object> claims = new HashMap<>();
        payload.asMap().forEach((k,v)-> claims.put(k, v.getAsString()));

        return new Result(sub, groups, scopes, claims);
    }

    private static String stripBearer(String header) {
        if (header == null) throw new IllegalArgumentException("Mangler Authorization header");
        if (header.startsWith("Bearer ")) return header.substring("Bearer ".length()).trim();
        return header.trim();
    }

    static class MissingScopeException extends SecurityException {
        MissingScopeException(String msg) { super(msg); }
    }
}

