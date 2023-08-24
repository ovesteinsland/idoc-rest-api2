package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.InvoiceType;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.invoicetype")
@RolesAllowed({"ApplicationRole"})
public class InvoiceTypeFacadeREST extends AbstractFacade<InvoiceType> {

    public InvoiceTypeFacadeREST() {
        super(InvoiceType.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "InvoiceType.findAll";
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(InvoiceType entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, InvoiceType entity) {
        InvoiceType invoiceType = this.find(id);
        /*address.setAddress1(entity.getAddress1());
        address.setAddress2(entity.getAddress2());
        address.setAddressType(entity.getAddressType());
        address.setCity(entity.getCity());
        address.setCountry(entity.getCountry());
        address.setZipCode(entity.getZipCode());*/
        super.edit(invoiceType);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public InvoiceType find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<InvoiceType> findAll() {
        return super.findAll();
    }
    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<InvoiceType> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
}
