/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Company;
import no.softwarecontrol.idoc.data.entityobject.Contract;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.contract")
@RolesAllowed({"ApplicationRole"})
public class ContractFacadeREST extends AbstractFacade<Contract> {


    public ContractFacadeREST() {
        super(Contract.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Contract.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Contract entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Contract entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        Contract contract = super.find(id);
        if (contract != null) {
            CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
            Company company = contract.getCompany();
            if (company.getContractList().contains(contract)) {
                company.getContractList().remove(contract);
                companyFacadeREST.editInternal(company);
            }
            Company partner = contract.getPartner();
            if (partner.getContractList().contains(contract)) {
                partner.getContractList().remove(contract);
                companyFacadeREST.editInternal(partner);
            }
            super.remove(contract);
        }
    }

    @GET
    @Path("loadByCompany/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> loadByCompany(@PathParam("companyId") String companyId) {
        List<Contract> contracts = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        contracts = (List<Contract>) em.createNativeQuery("SELECT "
                        + "* FROM contract c\n"
                        + "WHERE "
                        + "c.company = ?1",
                Contract.class)
                .setParameter(1, companyId)
                .getResultList();

        em.close();
        return contracts;
    }

    @GET
    @Path("loadByCompanyAndPartner/{companyId}/{partnerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> loadByCompanyAndPartner(@PathParam("companyId") String companyId, @PathParam("partnerId") String partnerId) {
        List<Contract> contracts = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        contracts = (List<Contract>) em.createNativeQuery("SELECT "
                        + "* FROM contract c\n"
                        + "WHERE "
                        + "c.company = ?1 AND c.partner = ?2",
                Contract.class)
                .setParameter(1, companyId)
                .setParameter(2, partnerId)
                .getResultList();

        em.close();
        return contracts;
    }


    @GET
    @Path("loadByPartner/{authorityId}/{partnerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> loadByPartner(@PathParam("authorityId") String authorityId, @PathParam("partnerId") String partnerId) {
        List<Contract> contracts = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        contracts = (List<Contract>) em.createNativeQuery("SELECT "
                        + "* FROM contract c\n"
                        + "WHERE "
                        + "c.company = ?1 AND c.partner = ?2",
                Contract.class)
                .setParameter(1, authorityId)
                .setParameter(2, partnerId)
                .getResultList();

        em.close();
        return contracts;
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Contract find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Contract> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
    
    /*@PUT
    @Path("createwithcompany")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void createWithCompany(CompanyContractReference reference) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company partner = companyFacadeREST.find(reference.getPartnerId());
        Company company = companyFacadeREST.find(reference.getCompanyId());
        if(partner != null && company != null){
            Contract contract = reference.getContract();
            contract.setCompany(company);
            contract.setPartner(partner);
            company.getContractList().add(contract);
            partner.getPartnerContracts().add(contract);
            super.create(contract);
            companyFacadeREST.edit(company);
            companyFacadeREST.edit(partner);
        }
    }*/

}
