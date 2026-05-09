/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.disipline")
@RolesAllowed({"ApplicationRole"})
public class DisiplineFacadeREST extends AbstractFacade<Disipline> {
    private static DisiplineFacadeREST instance;

    public DisiplineFacadeREST() {
        super(Disipline.class);
        instance = this;
    }

    public static DisiplineFacadeREST getInstance() {
        if (instance == null) {
            instance = new DisiplineFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery(){
        return "Disipline.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(Disipline entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Disipline entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public Disipline find(@PathParam("id") String id) {
        Disipline entity = super.find(id);
        return entity;
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Disipline> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Disipline> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    void linkToCompany(String companyId, String disiplineId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // Sjekk om linken allerede eksisterer
            Number counter = (Number) em.createNativeQuery("""
                SELECT COUNT(*) 
                FROM company_has_disipline
                WHERE company_company_id = ?1 
                  AND disipline_disipline_id = ?2
                """)
                    .setParameter(1, companyId)
                    .setParameter(2, disiplineId)
                    .getSingleResult();

            if (counter.intValue() == 0) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    em.createNativeQuery("""
                        INSERT INTO company_has_disipline (company_company_id, disipline_disipline_id)
                        VALUES (?, ?)
                        """)
                            .setParameter(1, companyId)
                            .setParameter(2, disiplineId)
                            .executeUpdate();
                    tx.commit();
                } catch (Exception e) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting into company_has_disipline");
                    System.out.println("Company ID: " + companyId + ", Disipline ID: " + disiplineId);
                    System.out.println("Error: " + e.getMessage());
                    //throw new RuntimeException("Failed to link disipline to company", e);
                }
            }
            // Link eksisterer allerede - ingen handling nødvendig
        } // EntityManager lukkes automatisk her
    }
    
}
