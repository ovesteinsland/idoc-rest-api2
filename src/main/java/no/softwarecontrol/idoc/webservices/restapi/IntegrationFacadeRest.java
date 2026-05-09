package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Equipment;
import no.softwarecontrol.idoc.data.entityobject.Integration;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.integration")
@RolesAllowed({"ApplicationRole"})
public class IntegrationFacadeRest extends AbstractFacade<Integration> {

    public IntegrationFacadeRest() {
        super(Integration.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Integration.findAll";
    }

    @POST
    @Path("createWithCompany/{authorityId}/{customerId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Integration entity, @PathParam("authorityId") String authorityId, @PathParam("customerId") String customerId) {
        super.create(entity);
        linkCompany(entity.getIntegrationId(),authorityId);
        linkCompany(entity.getIntegrationId(),customerId);
    }


    public void linkCompany(String integrationId, String companyId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM company_has_integration
                            WHERE integration_integration_id = ?1 AND company_company_id = ?2
                            """)
                    .setParameter(1, integrationId)
                    .setParameter(2, companyId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                    INSERT INTO company_has_integration (integration_integration_id, company_company_id)
                                    VALUES (?, ?);
                                    """)
                            .setParameter(1, integrationId)
                            .setParameter(2, companyId)
                            .executeUpdate();
                    tx.commit();
                } catch (Exception exp) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting into company_has_integration: " + exp.getMessage());
                    exp.printStackTrace(System.err);
                    throw new RuntimeException("Failed to link company with integration", exp);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while checking/linking company: " + e.getMessage());
            e.printStackTrace(System.err);
            //throw new RuntimeException("Failed to link company", e);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }
}
