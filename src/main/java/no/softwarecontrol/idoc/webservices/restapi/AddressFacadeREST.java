/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Address;
import no.softwarecontrol.idoc.data.entityobject.Company;

import java.util.List;

/**
 * @servicetag Address
 *
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.address")
@RolesAllowed({"ApplicationRole"})
public class AddressFacadeREST extends AbstractFacade<Address> {

    public AddressFacadeREST() {
        super(Address.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Address.findAll";
    }

    /**
     *
     * Create an address
     *
     * @param entity The address to be created.
     * @summary Create
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(Address entity) {
        super.create(entity);
    }

    /**
     *
     * Save an address.
     * @param id The id of the address.
     * @param entity The address object.
     * @summary Save
     */
    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Address entity) {
        Address address = this.find(id);
        if(address != null) {
            address.setAddress1(entity.getAddress1());
            address.setAddress2(entity.getAddress2());
            address.setAddressType(entity.getAddressType());
            address.setCity(entity.getCity());
            address.setCountry(entity.getCountry());
            address.setZipCode(entity.getZipCode());
            super.edit(address);
        }
    }

    /**
     * Link a company to an address.
     * @param companyId The company's uuid.
     * @param entity The address to be linked to the company.
     * @summary Link company
     */
    @PUT
    @Path("linkCompany/{companyId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId,Address entity) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Address address = this.find(entity.getAddressId());
        Company company = companyFacadeREST.find(companyId);
        if (address != null && company != null) {
            if (!address.getCompanyList().contains(company)) {
                address.getCompanyList().add(company);
                this.edit(address);
            }
            if (!company.getAddressList().contains(address)) {
                company.getAddressList().add(address);
                companyFacadeREST.edit(company);
            }
        }
    }

    /**
     * Delete an address.
     * @param id The uuid of the address.
     * @summary Delete
     */
    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    /**
     *
     * Load an address with it's uuid.
     * @param id The uuid of the address.
     * @return Address
     * @summary Load one
     */
    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public Address find(@PathParam("id") String id) {
        return super.find(id);
    }

    /**
     *
     * Load all addresses in the system. UPDATED 25.04.2017 TEST.
     * @return Array of addresses
     * @summary Load all
     */
    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Address> findAll() {
        return super.findAll();
    }

    /**
     * Load a list of addresses within a range.
     *
     * @param from
     * @param to
     * @return List of addresses
     * * @summary Load range
     */
    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Address> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

}
