package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager for Wheel of Fortune game sessions
 * Handles player pairing, session management, and automatic timeout cleanup
 */
public class WheelOfFortuneSessionManager {
    
    private static final Logger LOGGER = Logger.getLogger(WheelOfFortuneSessionManager.class.getName());
    private static volatile WheelOfFortuneSessionManager instance;
    
    // Configuration
    private static final long DEFAULT_TIMEOUT_MINUTES = 5;
    private static final long CLEANUP_INTERVAL_MINUTES = 1;
    
    // Thread-safe collections
    private final ConcurrentHashMap<String, WheelOfFortuneSession> activeSessions;
    private final ConcurrentHashMap<String, String> userToSessionMap;
    
    // Timeout cleanup
    private final ScheduledExecutorService scheduler;
    private final long timeoutMinutes;
    
    private WheelOfFortuneSessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.userToSessionMap = new ConcurrentHashMap<>();
        this.timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Start periodic cleanup task
        startTimeoutCleanup();
        
        LOGGER.info("WheelOfFortuneSessionManager initialized with timeout: " + timeoutMinutes + " minutes");
    }
    
    public static WheelOfFortuneSessionManager getInstance() {
        if (instance == null) {
            synchronized (WheelOfFortuneSessionManager.class) {
                if (instance == null) {
                    instance = new WheelOfFortuneSessionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Connects a player to a session. Creates a new session or joins existing one.
     */
    public synchronized WheelOfFortuneSession connectPlayer(String playerId, String partnerId, Session session) {
        LOGGER.info("Connecting player: " + playerId + " to partner: " + partnerId);
        
        // Check if player is already in a session
        String existingSessionId = userToSessionMap.get(playerId);
        if (existingSessionId != null) {
            WheelOfFortuneSession existingSession = activeSessions.get(existingSessionId);
            if (existingSession != null) {
                // Update existing session
                updatePlayerSession(existingSession, playerId, session);
                return existingSession;
            }
        }
        
        // Look for existing session with this player pair
        WheelOfFortuneSession gameSession = findSessionForPlayerPair(playerId, partnerId);
        
        if (gameSession == null) {
            // Create new session
            gameSession = new WheelOfFortuneSession(playerId, partnerId);
            activeSessions.put(gameSession.getSessionId(), gameSession);
            LOGGER.info("Created new session: " + gameSession.getSessionId());
        }
        
        // Add player to session
        updatePlayerSession(gameSession, playerId, session);
        userToSessionMap.put(playerId, gameSession.getSessionId());
        
        LOGGER.info("Player " + playerId + " connected to session: " + gameSession.getSessionId());
        return gameSession;
    }
    
    /**
     * Disconnects a player from their session
     */
    public synchronized void disconnectPlayer(String playerId) {
        String sessionId = userToSessionMap.get(playerId);
        if (sessionId != null) {
            WheelOfFortuneSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.removePlayerSession(playerId);
                userToSessionMap.remove(playerId);
                
                LOGGER.info("Player " + playerId + " disconnected from session: " + sessionId);
                
                // Remove session if both players are gone
                if (session.getPlayerSession() == null && session.getPartnerSession() == null) {
                    activeSessions.remove(sessionId);
                    userToSessionMap.remove(session.getPlayerId());
                    userToSessionMap.remove(session.getPartnerId());
                    LOGGER.info("Session " + sessionId + " removed - no active players");
                }
            }
        }
    }
    
    /**
     * Gets the session for a specific player
     */
    public WheelOfFortuneSession getSessionForPlayer(String playerId) {
        String sessionId = userToSessionMap.get(playerId);
        if (sessionId != null) {
            return activeSessions.get(sessionId);
        }
        return null;
    }
    
    /**
     * Updates activity timestamp for a player
     */
    public void updatePlayerActivity(String playerId) {
        WheelOfFortuneSession session = getSessionForPlayer(playerId);
        if (session != null) {
            session.updateActivity(playerId);
        }
    }
    
    /**
     * Gets all active sessions (for monitoring/debugging)
     */
    public Collection<WheelOfFortuneSession> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Gets count of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    private WheelOfFortuneSession findSessionForPlayerPair(String playerId, String partnerId) {
        for (WheelOfFortuneSession session : activeSessions.values()) {
            if ((session.getPlayerId().equals(playerId) && session.getPartnerId().equals(partnerId)) ||
                (session.getPlayerId().equals(partnerId) && session.getPartnerId().equals(playerId))) {
                return session;
            }
        }
        return null;
    }
    
    private void updatePlayerSession(WheelOfFortuneSession gameSession, String playerId, Session session) {
        if (gameSession.getPlayerId().equals(playerId)) {
            gameSession.setPlayerSession(session);
        } else if (gameSession.getPartnerId().equals(playerId)) {
            gameSession.setPartnerSession(session);
        }
    }
    
    private void startTimeoutCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupInactiveSessions();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during session cleanup", e);
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    private void cleanupInactiveSessions() {
        List<String> sessionsToRemove = new ArrayList<>();
        
        for (Map.Entry<String, WheelOfFortuneSession> entry : activeSessions.entrySet()) {
            WheelOfFortuneSession session = entry.getValue();
            if (session.isInactive(timeoutMinutes)) {
                sessionsToRemove.add(entry.getKey());
                
                // Close any remaining WebSocket connections
                closeSessionConnections(session);
                
                LOGGER.info("Removing inactive session: " + session.getSessionId());
            }
        }
        
        // Remove inactive sessions
        for (String sessionId : sessionsToRemove) {
            WheelOfFortuneSession session = activeSessions.remove(sessionId);
            if (session != null) {
                userToSessionMap.remove(session.getPlayerId());
                userToSessionMap.remove(session.getPartnerId());
            }
        }
        
        if (!sessionsToRemove.isEmpty()) {
            LOGGER.info("Cleaned up " + sessionsToRemove.size() + " inactive sessions");
        }
    }
    
    private void closeSessionConnections(WheelOfFortuneSession session) {
        try {
            if (session.getPlayerSession() != null && session.getPlayerSession().isOpen()) {
                session.getPlayerSession().close();
            }
            if (session.getPartnerSession() != null && session.getPartnerSession().isOpen()) {
                session.getPartnerSession().close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing session connections", e);
        }
    }
    
    /**
     * Shutdown the session manager and cleanup resources
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close all active sessions
        for (WheelOfFortuneSession session : activeSessions.values()) {
            closeSessionConnections(session);
        }
        
        activeSessions.clear();
        userToSessionMap.clear();
        
        LOGGER.info("WheelOfFortuneSessionManager shutdown completed");
    }
}