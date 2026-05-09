package no.softwarecontrol.idoc.webservices.persistence;// ... eksisterende imports ...
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.annotation.Resource; // Ny import
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnit; // Ny import
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence; // Beholdes hvis du fortsatt vil ha den fallback-muligheten
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import javax.naming.InitialContext; // Ny import
import javax.sql.DataSource; // Ny import
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
// ...

@WebListener
public class LocalEntityManagerFactory implements ServletContextListener {

    private static EntityManagerFactory emf;

    // Du kan også injisere DataSource direkte hvis du bruker CDI/EJB
    // @Resource(name = "jdbc/iDocDatabase")
    // private DataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            // Slå opp DataSource fra JNDI
            InitialContext initialContext = new InitialContext();
            DataSource dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/iDocDatabase");

            // Nå oppretter vi EntityManagerFactory ved å gi DataSource-objektet
            // Dette er hvordan du forteller JPA å bruke den eksterne DataSource-en
            // Properties fra persistence.xml vil fortsatt gjelde, men JDBC-detaljene kommer fra DataSource
            Map<String, Object> properties = new HashMap<>();
            properties.put("jakarta.persistence.jtaDataSource", dataSource); // Fortell EclipseLink å bruke denne JTA DataSource
            // properties.put("jakarta.persistence.nonJtaDataSource", dataSource); // Hvis du også bruker non-JTA

            // Hvis du har andre EclipseLink-spesifikke egenskaper, kan de også legges til her
            // f.eks. properties.put("eclipselink.logging.level", "FINE");

            emf = Persistence.createEntityManagerFactory("iDocDatabasePU", properties);

        } catch (Exception e) {
            // Logg feilen nøye! Dette er kritisk for å forstå hvorfor EF ikke ble initialisert.
            event.getServletContext().log("Failed to initialize EntityManagerFactory: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize EntityManagerFactory", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }

        // Stopp MySQL cleanup-tråden
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception e) {
            event.getServletContext().log("Failed to shutdown MySQL cleanup thread: " + e.getMessage(), e);
        }

        // Avregistrer JDBC-drivere
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                event.getServletContext().log("Deregistered JDBC driver: " + driver);
            } catch (SQLException e) {
                event.getServletContext().log("Failed to deregister JDBC driver: " + driver, e);
            }
        }

    }

    public static EntityManager createEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("Context is not initialized yet. EntityManagerFactory is null.");
        }
        return emf.createEntityManager();
    }
}