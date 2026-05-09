package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WebSocket endpoint for Wheel of Fortune game
 * Handles real-time communication between two players
 */
@ServerEndpoint(
    value = "/wheeloffortune",
    configurator = WheelOfFortuneConfigurator.class
)
public class WheelOfFortuneWebSocket {
    
    private static final Logger LOGGER = Logger.getLogger(WheelOfFortuneWebSocket.class.getName());
    private final WheelOfFortuneSessionManager sessionManager = WheelOfFortuneSessionManager.getInstance();
    private final Jsonb jsonb = JsonbBuilder.create();

    static {
        LOGGER.info("=== WheelOfFortuneWebSocket class loaded ===");
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        try {
            // Extract validated parameters from configurator
            String playerId = (String) config.getUserProperties().get("playerId");
            String partnerId = (String) config.getUserProperties().get("partnerId");
            
            if (playerId == null || partnerId == null) {
                LOGGER.severe("Missing required parameters after validation");
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid session parameters"));
                return;
            }
            
            LOGGER.info("WebSocket connection opened for player: " + playerId + " with partner: " + partnerId);
            
            // Store player information in session for later use
            session.getUserProperties().put("playerId", playerId);
            session.getUserProperties().put("partnerId", partnerId);
            
            // Connect player to game session
            WheelOfFortuneSession gameSession = sessionManager.connectPlayer(playerId, partnerId, session);
            
            if (gameSession != null) {
                // Notify about partner connection status
                sendPartnerStatusUpdate(session, gameSession, playerId);
                
                // If partner is already connected, notify them about this connection
                Session partnerSession = gameSession.getPartnerSession(playerId);
                if (partnerSession != null && partnerSession.isOpen()) {
                    sendPartnerStatusUpdate(partnerSession, gameSession, partnerId);
                }
                
                LOGGER.info("Player " + playerId + " successfully connected to session: " + gameSession.getSessionId());
            } else {
                LOGGER.warning("Failed to connect player " + playerId + " to session");
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Failed to create session"));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in onOpen", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "Error closing session after onOpen failure", ioException);
            }
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            String playerId = (String) session.getUserProperties().get("playerId");
            if (playerId == null) {
                LOGGER.warning("Received message from session without playerId");
                return;
            }
            
            // Update player activity
            sessionManager.updatePlayerActivity(playerId);
            
            // Get game session
            WheelOfFortuneSession gameSession = sessionManager.getSessionForPlayer(playerId);
            if (gameSession == null) {
                sendErrorMessage(session, "No active session found");
                return;
            }
            
            // Parse incoming message
            WheelOfFortuneMessage incomingMessage;
            try {
                incomingMessage = jsonb.fromJson(message, WheelOfFortuneMessage.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse incoming message: " + message, e);
                sendErrorMessage(session, "Invalid message format");
                return;
            }
            
            // Forward message to partner
            Session partnerSession = gameSession.getPartnerSession(playerId);
            if (partnerSession != null && partnerSession.isOpen()) {
                // Create forwarded message with current timestamp
                WheelOfFortuneMessage forwardedMessage = new WheelOfFortuneMessage(
                    incomingMessage.getType(),
                    incomingMessage.getPayload()
                );
                
                String jsonMessage = jsonb.toJson(forwardedMessage);
                //this.session.getAsyncRemote().sendObject(message);
                //partnerSession.getAsyncRemote().sendText(jsonMessage);
                partnerSession.getBasicRemote().sendText(jsonMessage);
            } else {
                LOGGER.warning("Partner not available to receive message from " + playerId);
                sendErrorMessage(session, "Partner not connected");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in onMessage", e);
            sendErrorMessage(session, "Server error processing message");
        }
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        try {
            String playerId = (String) session.getUserProperties().get("playerId");
            if (playerId == null) {
                LOGGER.warning("Session closed without playerId information");
                return;
            }
            
            LOGGER.info("WebSocket connection closed for player: " + playerId + 
                       ", reason: " + closeReason.getReasonPhrase());
            
            // Get game session before disconnecting
            WheelOfFortuneSession gameSession = sessionManager.getSessionForPlayer(playerId);
            
            // Disconnect player
            sessionManager.disconnectPlayer(playerId);
            
            // Notify partner about disconnection
            if (gameSession != null) {
                Session partnerSession = gameSession.getPartnerSession(playerId);
                if (partnerSession != null && partnerSession.isOpen()) {
                    sendPartnerStatusUpdate(partnerSession, gameSession, 
                                          playerId.equals(gameSession.getPlayerId()) ? gameSession.getPartnerId() : gameSession.getPlayerId());
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in onClose", e);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            String playerId = (String) session.getUserProperties().get("playerId");
            LOGGER.log(Level.SEVERE, "WebSocket error for player: " + playerId, throwable);
            
            // Send error message to client if session is still open
            if (session.isOpen()) {
                sendErrorMessage(session, "Connection error occurred");
            }
            
            // Clean up session
            if (playerId != null) {
                sessionManager.disconnectPlayer(playerId);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in error handler", e);
        }
    }
    
    /**
     * Sends partner connection status update to a player
     */
    private void sendPartnerStatusUpdate(Session session, WheelOfFortuneSession gameSession, String currentPlayerId) {
        try {
            if (session == null || !session.isOpen()) {
                return;
            }
            
            boolean partnerConnected = gameSession.isBothPlayersConnected();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("partnerConnected", partnerConnected);
            
            WheelOfFortuneMessage statusMessage = new WheelOfFortuneMessage(
                WheelOfFortuneMessage.MessageType.STATUS,
                payload
            );
            
            String jsonMessage = jsonb.toJson(statusMessage);
            session.getAsyncRemote().sendText(jsonMessage);
            
            LOGGER.info("Sent partner status update to " + currentPlayerId + ": partnerConnected=" + partnerConnected);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send partner status update", e);
        }
    }
    
    /**
     * Sends error message to a player
     */
    private void sendErrorMessage(Session session, String errorMessage) {
        try {
            if (session == null || !session.isOpen()) {
                return;
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", errorMessage);
            
            WheelOfFortuneMessage errorMsg = new WheelOfFortuneMessage(
                WheelOfFortuneMessage.MessageType.ERROR,
                payload
            );
            
            String jsonMessage = jsonb.toJson(errorMsg);
            session.getAsyncRemote().sendText(jsonMessage);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send error message", e);
        }
    }
}