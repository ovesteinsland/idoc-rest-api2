package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.Map;

/**
 * DTO for JSON messages in Wheel of Fortune WebSocket communication
 * Supports message types: GAME_DATA, STATUS, ERROR
 */
public class WheelOfFortuneMessage {
    
    public enum MessageType {
        GAME_DATA,
        STATUS,
        HEARTBEAT,
        ERROR
    }
    
    @JsonbProperty("type")
    private MessageType type;
    
    @JsonbProperty("payload")
    private Map<String, Object> payload;
    
    @JsonbProperty("timestamp")
    private String timestamp;
    
    public WheelOfFortuneMessage() {
        this.timestamp = Instant.now().toString();
    }
    
    public WheelOfFortuneMessage(MessageType type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now().toString();
    }
    
    // Getters and setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}