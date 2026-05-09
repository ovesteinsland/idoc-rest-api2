package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket server configuration
 * Explicitly registers WebSocket endpoints for scanning
 */
public class WheelOfFortuneServerConfig implements ServerApplicationConfig {

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        // No programmatic endpoints
        return new HashSet<>();
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        // Return annotated endpoint classes to be deployed
        Set<Class<?>> results = new HashSet<>();
        results.add(WheelOfFortuneWebSocket.class);
        return results;
    }
}