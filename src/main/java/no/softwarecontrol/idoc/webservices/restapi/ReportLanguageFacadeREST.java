package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.EquipmentType;
import no.softwarecontrol.idoc.data.entityobject.EquipmentTypeLanguage;
import no.softwarecontrol.idoc.data.entityobject.Report;
import no.softwarecontrol.idoc.data.entityobject.ReportLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.reportlanguage")
@RolesAllowed({"ApplicationRole"})
public class ReportLanguageFacadeREST extends AbstractFacade<ReportLanguage> {

    public ReportLanguageFacadeREST() {
        super(ReportLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "ReportLanguage.findAll";
    }

    @POST
    @Path("create/{reportId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("reportId") String reportId, ReportLanguage entity) {
        ReportFacadeREST reportFacadeREST = new ReportFacadeREST();
        Report report = reportFacadeREST.find(reportId);
        entity.setReport(report);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, ReportLanguage entity) {
        ReportLanguage existing = this.find(entity.getReportLanguageId());
        if(existing != null) {
            existing.setTitle(entity.getTitle());
            existing.setBody(entity.getBody());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public ReportLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
