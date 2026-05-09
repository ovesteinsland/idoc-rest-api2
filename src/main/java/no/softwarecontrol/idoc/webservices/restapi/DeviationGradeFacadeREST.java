package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Deviation;
import no.softwarecontrol.idoc.data.entityobject.DeviationGrade;
import no.softwarecontrol.idoc.data.entityobject.User;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.deviationgrade")
@RolesAllowed({"ApplicationRole"})
public class DeviationGradeFacadeREST extends AbstractFacade<DeviationGrade> {

    public DeviationGradeFacadeREST() {
        super(DeviationGrade.class);
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public DeviationGrade find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Path("loadByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<DeviationGrade> loadByDisipline(@PathParam("disiplineId") String disiplineId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery("""
                SELECT DISTINCT dg.*
                FROM deviation_grade dg
                JOIN disipline_has_deviation_grade dhdg ON dhdg.deviation_grade_deviation_grade_id = dg.deviation_grade_id
                WHERE dhdg.disipline_disipline_id = ?1
                """,
                            DeviationGrade.class)
                    .setParameter(1, disiplineId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByDisipline for Disipline ID: " + disiplineId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    protected String getSelectAllQuery() {
        return "SELECT * FROM deviation";
    }
}
