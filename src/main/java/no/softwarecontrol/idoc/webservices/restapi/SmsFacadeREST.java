/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Sms;
import no.softwarecontrol.idoc.data.entityobject.User;
import no.softwarecontrol.idoc.keysms.KeySmsController;
import no.softwarecontrol.idoc.webservices.exception.UnsupportedMediaException;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
import no.softwarecontrol.idoc.webservices.restapi.ratelimit.RateLimit;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.ArrayList;
import java.util.Optional;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.sms")
@RolesAllowed({"ApplicationRole"})
@RateLimit(requests = 8, seconds = 600)
public class SmsFacadeREST {


    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public void sendSafe(Sms sms) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // 1) Hent SA-JSON fra Secrets Manager
            String regionName = get("COGNITO_REGION", null);
            String apiKey = "0d40949004a3eb9c95b4db930f2cc0fa";
            Optional<String> secretJson = loadApiKeyFromSecretsManager(regionName);
            if (secretJson.isPresent()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(secretJson.get());
                    if (node.has("keysms_apikey")) {
                        apiKey = node.get("keysms_apikey").asText();
                    }
                } catch (Exception e) {
                    System.out.println("Feil ved parsing av secret JSON: " + e.getMessage());
                }
            }
            String senderId = sms.getSenderId();
            User user = UserFacadeREST.getInstance().find(senderId);
            if (user != null) {
                KeySmsController keySmsController = new KeySmsController(apiKey);
                //String[] receivers = {"41793713"};
                String[] receivers = new String[sms.getNumberList().size()];
                boolean isValidNumbers = true;
                for (int i = 0; i < receivers.length; i++) {
                    receivers[i] = sms.getNumberList().get(i);
                    Query queryCount = em.createNativeQuery(
                                    "SELECT count(*) FROM user u\n " +
                                            "  where u.mobile = ?1")
                            .setParameter(1, receivers[i]);
                    Number counter = (Number) queryCount.getSingleResult();
                    int intCounter = Integer.parseInt(counter.toString());
                    if (intCounter == 0) {
                        isValidNumbers = false;
                    }
                }
                if (isValidNumbers) {
                    String message = sms.getMessage();
                    try {
                        keySmsController.sendMessage(message, receivers);
                    }  catch (Exception e) {
                        throw new UnsupportedMediaException("Server denied access to send SMS");
                    }
                } else {
                    throw new UnsupportedMediaException("Number is not valid");
                }
            }
        } catch (Exception e) {
            throw new UnsupportedMediaException("Failed to send SMS: " +  e.getMessage());
        }
    }

    public static Optional<String> loadApiKeyFromSecretsManager(String region) {
        String secretName = get("KEYSMS_APIKEY_NAME", null);
        if (secretName == null || secretName.isBlank()) {
            // Ingen secret konfigurert - bruk public client
            System.out.println("KEYSMS_APIKEY_NAME ikke satt - bruker public client (ingen secret)");
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

}
