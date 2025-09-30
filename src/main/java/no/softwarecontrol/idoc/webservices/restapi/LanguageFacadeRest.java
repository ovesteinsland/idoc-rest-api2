package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.KeyFault;
import no.softwarecontrol.idoc.data.entityobject.Language;
import no.softwarecontrol.idoc.data.entityobject.Observation;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
import no.softwarecontrol.idoc.webservices.restapi.ratelimit.RateLimit;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.language")
@RolesAllowed({"ApplicationRole"})
public class LanguageFacadeRest extends AbstractFacade<Language> {

    public LanguageFacadeRest() {
        super(Language.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Language.findAll";
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    @PermitAll
    @RateLimit(requests = 10, seconds = 60)
    public List<Language> findAll() {
        return super.findAll();
    }

    public void linkToCompany(String companyId, String languageId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_language \n " +
                            " WHERE company_company_id = ?1 AND language_language_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, languageId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO company_has_language (company_company_id, language_language_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, companyId)
                        .setParameter(2, languageId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: observation_has_measurement already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into observation_has_measurement: " + exp.getMessage());
        } finally {
            em.close();
        }
    }
}
