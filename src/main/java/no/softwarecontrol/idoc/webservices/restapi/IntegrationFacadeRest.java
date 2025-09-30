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
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_integration \n " +
                            " WHERE integration_integration_id = ?1 AND company_company_id = ?2")
                    .setParameter(1, integrationId)
                    .setParameter(2, companyId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO company_has_integration (integration_integration_id, company_company_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, integrationId)
                        .setParameter(2, companyId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: equipment_has_measurement already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into equipment_has_measurement: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }
}
