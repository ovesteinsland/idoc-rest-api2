package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.websocket.Session;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a Wheel of Fortune game session between two players
 * Holds both WebSocket sessions and activity timestamps for timeout management
 */
public class WheelOfFortuneSession {
    
    private final String sessionId;
    private final String playerId;
    private final String partnerId;
    
    private Session playerSession;
    private Session partnerSession;
    
    private Instant lastPlayerActivity;
    private Instant lastPartnerActivity;
    private final Instant createdAt;
    
    public WheelOfFortuneSession(String playerId, String partnerId) {
        this.sessionId = UUID.randomUUID().toString();
        this.playerId = playerId;
        this.partnerId = partnerId;
        this.createdAt = Instant.now();
        this.lastPlayerActivity = Instant.now();
        this.lastPartnerActivity = Instant.now();
    }
    
    /**
     * Sets the WebSocket session for the player
     */
    public void setPlayerSession(Session session) {
        this.playerSession = session;
        this.lastPlayerActivity = Instant.now();
    }
    
    /**
     * Sets the WebSocket session for the partner
     */
    public void setPartnerSession(Session session) {
        this.partnerSession = session;
        this.lastPartnerActivity = Instant.now();
    }
    
    /**
     * Updates activity timestamp for a specific user
     */
    public void updateActivity(String userId) {
        if (playerId.equals(userId)) {
            this.lastPlayerActivity = Instant.now();
        } else if (partnerId.equals(userId)) {
            this.lastPartnerActivity = Instant.now();
        }
    }
    
    /**
     * Gets the session for the other player (partner)
     */
    public Session getPartnerSession(String currentUserId) {
        if (playerId.equals(currentUserId)) {
            return partnerSession;
        } else if (partnerId.equals(currentUserId)) {
            return playerSession;
        }
        return null;
    }
    
    /**
     * Checks if both players are connected
     */
    public boolean isBothPlayersConnected() {
        return playerSession != null && playerSession.isOpen() && 
               partnerSession != null && partnerSession.isOpen();
    }
    
    /**
     * Checks if the session is inactive based on timeout
     */
    public boolean isInactive(long timeoutMinutes) {
        Instant cutoff = Instant.now().minusSeconds(timeoutMinutes * 60);
        return lastPlayerActivity.isBefore(cutoff) && lastPartnerActivity.isBefore(cutoff);
    }
    
    /**
     * Removes a player's session (on disconnect)
     */
    public void removePlayerSession(String userId) {
        if (playerId.equals(userId)) {
            this.playerSession = null;
        } else if (partnerId.equals(userId)) {
            this.partnerSession = null;
        }
    }
    
    /**
     * Checks if a user belongs to this session
     */
    public boolean containsUser(String userId) {
        return playerId.equals(userId) || partnerId.equals(userId);
    }
    
    // Getters
    public String getSessionId() {
        return sessionId;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public Session getPlayerSession() {
        return playerSession;
    }
    
    public Session getPartnerSession() {
        return partnerSession;
    }
    
    public Instant getLastPlayerActivity() {
        return lastPlayerActivity;
    }
    
    public Instant getLastPartnerActivity() {
        return lastPartnerActivity;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}