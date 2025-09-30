package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Company;
import no.softwarecontrol.idoc.data.entityobject.Invoice;
import no.softwarecontrol.idoc.data.entityobject.Observation;
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.data.requestparams.ProjectRequestParameters;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
import org.checkerframework.checker.units.qual.A;

import java.util.*;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.invoice")
@RolesAllowed({"ApplicationRole"})
public class InvoiceFacadeREST extends AbstractFacade<Invoice> {
    public InvoiceFacadeREST() {
        super(Invoice.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "Invoice.findAll";
    }

    @GET
    @Path("loadByCompany/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Invoice> loadByCompany(@PathParam("companyId") String companyId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Invoice> resultList = (List<Invoice>) em.createNativeQuery("SELECT "
                                + "* FROM invoice i\n"
                                + "WHERE i.company = ?1\n",
                        Invoice.class)
                .setParameter(1, companyId)
                .getResultList();
        em.close();
        return resultList;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(Invoice entity) {
        super.create(entity);
    }


    @POST
    @Path("createWithCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, Invoice invoice) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company company = companyFacadeREST.find(companyId);
        if (company != null) {
            invoice.setCompany(company);
            super.create(invoice);
            company.getInvoiceList().add(invoice);
            companyFacadeREST.editInternal(company);
        }
    }

    private Invoice createMemory(Date startDate, Company company, Invoice previousInvoice)   {
        Invoice invoice =new  Invoice();
        invoice.setInvoiceId(UUID.randomUUID().toString());
        invoice.setStartDate(startDate);
        if (previousInvoice.getInvoiceType() != null) {
            invoice.setPointLimit(previousInvoice.getInvoiceType().getPointLimit());
            invoice.setPointPrice(previousInvoice.getInvoiceType().getPointPrice());
        } else {
            invoice.setPointLimit(previousInvoice.getPointLimit());
            invoice.setPointPrice(previousInvoice.getPointPrice());
        }

        invoice.setPointDiscount(previousInvoice.getPointDiscount());
        invoice.setDescription(previousInvoice.getDescription());
        invoice.setInvoiceType(previousInvoice.getInvoiceType());
        invoice.setInvoiced(false);

        invoice.setCompany(company);
        return invoice;
    }

    @GET
    @Path("renew/{previousInvoiceId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Invoice renew(@PathParam("previousInvoiceId") String previousInvoiceId){
        Invoice previousInvoice = find(previousInvoiceId);

        ProjectRequestParameters projectRequestParameters = new ProjectRequestParameters();
        projectRequestParameters.authorityId = previousInvoice.getCompany().getCompanyId();
        //projectRequestParameters.userIds = parameter.getUserIds();
        projectRequestParameters.states.add(0);
        projectRequestParameters.states.add(1);
        projectRequestParameters.states.add(5);
        projectRequestParameters.states.add(8);
        projectRequestParameters.states.add(9);

        projectRequestParameters.fromDate = previousInvoice.getStartDate();
        projectRequestParameters.toDate = new Date();
        projectRequestParameters.dateField = ProjectRequestParameters.DateField.CREATED_DATE;
        projectRequestParameters.parentEntity = "company";
        projectRequestParameters.entityIds.add(previousInvoice.getCompany().getCompanyId());
        projectRequestParameters.batchOffset = 0;
        projectRequestParameters.batchSize = 9999;
        projectRequestParameters.excludeUpcoming = true;
        projectRequestParameters.isDeleted = false;

        ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
        //List<Project> projects2 = projectFacadeREST.loadProjects(projectRequestParameters);
        List<Project> projects = projectFacadeREST.loadInvoiceProjects(projectRequestParameters);
        // Sorter stigende dato
        Collections.sort(projects, (Project o2, Project o1) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()));

        double pointCounter = previousInvoice.getPointTransfered();
        boolean isEnded = false;
        Date endDate = null;
        double transferPoints = 0.0;
        for (Project project: projects) {
            if (!isEnded) {
                double projectPoints = project.calculatePoints();
                pointCounter = pointCounter + projectPoints;
                //System.out.println("pointCounter = " + pointCounter );
            }
            if (pointCounter >= previousInvoice.getPointLimit()) {
                if (!isEnded){
                    isEnded = true;
                    endDate = project.getCreatedDate();
                    transferPoints = pointCounter - previousInvoice.getPointLimit();
                }
            }
        }
        // Let's find the end date
        if (endDate != null) {
            endDate.setTime(endDate.getTime() + 1000); // add 1000 milliseconds
            Invoice invoice = createMemory(endDate, previousInvoice.getCompany(), previousInvoice);
            previousInvoice.setEndDate(endDate);
            previousInvoice.setInvoiced(true);
            invoice.setPointTransfered(transferPoints);

            // forrige Invoice må lagres før ny opprettes
            edit(previousInvoice.getInvoiceId(),previousInvoice);

            create(invoice);
            return invoice;
        } else {
            return null;
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Invoice entity) {
        Invoice invoice = this.find(id);
        if(invoice != null) {
            invoice.setDescription(entity.getDescription());
            invoice.setInvoiced(entity.getInvoiced());
            invoice.setEndDate(entity.getEndDate());
            invoice.setPointDiscount(entity.getPointDiscount());
            invoice.setPointLimit(entity.getPointLimit());
            invoice.setInvoiceType(entity.getInvoiceType());
            invoice.setPointPrice(entity.getPointPrice());
            invoice.setPointTransfered(entity.getPointTransfered());
            invoice.setStartDate(entity.getStartDate());
            super.edit(invoice);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        Invoice invoice = super.find(id);
        super.remove(invoice);
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public Invoice find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Invoice> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
}
