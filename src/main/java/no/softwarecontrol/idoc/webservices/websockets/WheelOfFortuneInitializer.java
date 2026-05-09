package no.softwarecontrol.idoc.webservices.websockets;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class WheelOfFortuneInitializer implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(WheelOfFortuneInitializer.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("=== WheelOfFortuneInitializer: Starting WebSocket registration ===");

        try {
            ServerContainer serverContainer = (ServerContainer) sce.getServletContext()
                    .getAttribute("jakarta.websocket.server.ServerContainer");

            if (serverContainer == null) {
                LOGGER.severe("ServerContainer not found! WebSocket support may not be enabled.");
                return;
            }

            serverContainer.addEndpoint(WheelOfFortuneWebSocket.class);
            LOGGER.info("=== WheelOfFortuneWebSocket endpoint registered successfully ===");

        } catch (DeploymentException e) {
            LOGGER.log(Level.SEVERE, "Failed to register WebSocket endpoint", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during WebSocket registration", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("=== WheelOfFortuneInitializer: Context destroyed ===");
        WheelOfFortuneSessionManager.getInstance().shutdown();
    }
}