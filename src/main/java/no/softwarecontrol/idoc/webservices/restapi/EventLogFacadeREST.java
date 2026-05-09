package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.eventlog")
@RolesAllowed({"ApplicationRole"})
public class EventLogFacadeREST extends AbstractFacade<EventLog> {


    //private final UserFacadeREST userFacadeREST = UserFacadeREST.getInstance();

    public EventLogFacadeREST() {
        super(EventLog.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "EventLog.findAll";
    }

    @POST
    @Path("{projectId}/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(
            @PathParam("projectId") String projectId,
            @PathParam("userId") String userId,
            EventLog entity) {
        User user = UserFacadeREST.getInstance().find(userId);
        entity.setUser(user);
        super.create(entity);
        linkToProject(entity.getEventLogId(),projectId);
    }

    private void linkToProject(String eventLogId, String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // Sjekk om linken allerede eksisterer
            Number counter = (Number) em.createNativeQuery("""
                SELECT COUNT(*) 
                FROM project_has_event_log
                WHERE project_project_id = ?1 
                  AND event_log_event_log_id = ?2
                """)
                    .setParameter(1, projectId)
                    .setParameter(2, eventLogId)
                    .getSingleResult();

            if (counter.intValue() == 0) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    em.createNativeQuery("""
                        INSERT INTO project_has_event_log (project_project_id, event_log_event_log_id)
                        VALUES (?, ?)
                        """)
                            .setParameter(1, projectId)
                            .setParameter(2, eventLogId)
                            .executeUpdate();
                    tx.commit();
                } catch (Exception e) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting into project_has_event_log");
                    System.out.println("Project ID: " + projectId + ", Event Log ID: " + eventLogId);
                    System.out.println("Error: " + e.getMessage());
                    //throw new RuntimeException("Failed to link event log to project", e);
                }
            }
            // Link eksisterer allerede - ingen handling nødvendig
        } // EntityManager lukkes automatisk her
    }
}
