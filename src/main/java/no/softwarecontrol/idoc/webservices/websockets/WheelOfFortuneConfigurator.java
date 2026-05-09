package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WebSocket configurator for Wheel of Fortune game
 * Validates API key before allowing WebSocket connections
 */
public class WheelOfFortuneConfigurator extends ServerEndpointConfig.Configurator {
    
    private static final Logger LOGGER = Logger.getLogger(WheelOfFortuneConfigurator.class.getName());
    
    // Hardcoded API key - in production this should come from configuration
    private static final String VALID_API_KEY = "wheeloffortune-secret-key-2026";
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        LOGGER.info("Processing WebSocket handshake request");
        
        try {
            // Extract query parameters
            String queryString = request.getQueryString();
            LOGGER.info("Query string: " + queryString);
            
            if (queryString == null || queryString.isEmpty()) {
                LOGGER.warning("No query parameters provided");
                throw new SecurityException("Missing required parameters");
            }
            
            // Parse query parameters
            String[] params = queryString.split("&");
            String playerId = null;
            String partnerId = null;
            String apiKey = null;
            
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    switch (key) {
                        case "playerId":
                            playerId = decodeValue(value);
                            break;
                        case "partnerId":
                            partnerId = decodeValue(value);
                            break;
                        case "apiKey":
                            apiKey = decodeValue(value);
                            break;
                    }
                }
            }
            
            // Validate required parameters
            if (playerId == null || playerId.trim().isEmpty()) {
                LOGGER.warning("Missing or empty playerId");
                throw new SecurityException("Missing playerId parameter");
            }
            
            if (partnerId == null || partnerId.trim().isEmpty()) {
                LOGGER.warning("Missing or empty partnerId");
                throw new SecurityException("Missing partnerId parameter");
            }
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOGGER.warning("Missing or empty API key");
                throw new SecurityException("Missing API key");
            }
            
            // Validate API key
            if (!VALID_API_KEY.equals(apiKey.trim())) {
                LOGGER.warning("Invalid API key provided: " + apiKey);
                throw new SecurityException("Invalid API key");
            }
            
            // Validate that playerId and partnerId are different
            if (playerId.equals(partnerId)) {
                LOGGER.warning("PlayerId and partnerId cannot be the same: " + playerId);
                throw new SecurityException("PlayerId and partnerId must be different");
            }
            
            // Store validated parameters in user properties for use in WebSocket endpoint
            config.getUserProperties().put("playerId", playerId);
            config.getUserProperties().put("partnerId", partnerId);
            config.getUserProperties().put("apiKey", apiKey);
            
            LOGGER.info("Handshake validated successfully for player: " + playerId + " with partner: " + partnerId);
            
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Security validation failed: " + e.getMessage());
            // Add error header to response
            response.getHeaders().put("X-Error", List.of(e.getMessage()));
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during handshake validation", e);
            response.getHeaders().put("X-Error", List.of("Internal server error"));
            throw new SecurityException("Handshake validation failed");
        }
    }
    
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        // In production, implement proper origin validation
        // For now, allow all origins for development
        LOGGER.info("Origin check: " + originHeaderValue);
        return true;
    }
    
    @Override
    public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
        // No specific subprotocol requirements for this implementation
        return null;
    }
    
    @Override
    public List<jakarta.websocket.Extension> getNegotiatedExtensions(List<jakarta.websocket.Extension> installed, 
                                                                    List<jakarta.websocket.Extension> requested) {
        // No specific extensions required
        return new java.util.ArrayList<>();

    }
    
    /**
     * Simple URL decode for parameter values
     * In production, use proper URL decoding library
     */
    private String decodeValue(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            // Basic URL decoding - replace common encoded characters
            return value.replace("%20", " ")
                       .replace("%3D", "=")
                       .replace("%26", "&")
                       .replace("%2B", "+")
                       .replace("%2F", "/");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error decoding parameter value: " + value, e);
            return value;
        }
    }
    
    /**
     * Get the hardcoded API key (for testing purposes)
     */
    public static String getValidApiKey() {
        return VALID_API_KEY;
    }
}