package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Report;
import no.softwarecontrol.idoc.data.entityobject.ReportParameter;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.reportparameter")
@RolesAllowed({"ApplicationRole"})
public class ReportParameterFacadeREST extends AbstractFacade<ReportParameter>{

    @EJB
    private ReportFacadeREST reportFacadeREST = new ReportFacadeREST();

    public ReportParameterFacadeREST() {
        super(ReportParameter.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "ReportParameter.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(ReportParameter entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, ReportParameter entity) {
        ReportParameter reportParameter = find(entity.getReportParameterId());
        reportParameter.setParameterName(entity.getParameterName());
        reportParameter.setParameterValue(entity.getParameterValue());
        reportParameter.setParameterType(entity.getParameterType());
        super.edit(reportParameter);
    }

    @PUT
    @Path("linkToReport/{reportId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToReport(@PathParam("reportId") String reportId, ReportParameter entity) {
        ReportParameter reportParameter = this.find(entity.getReportParameterId());
        Report report = reportFacadeREST.find(reportId);
        if (reportParameter != null && report != null) {
            if (!report.getReportParameterList().contains(reportParameter)) {
                report.getReportParameterList().add(reportParameter);
            }
            reportParameter.setReport(report);
            super.edit(reportParameter);
            reportFacadeREST.edit(report);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public ReportParameter find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<ReportParameter> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<ReportParameter> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
}
