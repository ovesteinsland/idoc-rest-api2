package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Report;
import no.softwarecontrol.idoc.data.entityobject.ReportLanguage;
import no.softwarecontrol.idoc.data.entityobject.ReportSection;
import no.softwarecontrol.idoc.data.entityobject.ReportSectionLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.reportsectionlanguage")
@RolesAllowed({"ApplicationRole"})
public class ReportSectionLanguageFacadeREST extends AbstractFacade<ReportSectionLanguage> {

    public ReportSectionLanguageFacadeREST() {
        super(ReportSectionLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "ReportSectionLanguage.findAll";
    }

    @POST
    @Path("create/{reportSectionId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("reportSectionId") String reportSectionId, ReportSectionLanguage entity) {
        ReportSectionFacadeREST reportSectionFacadeREST = new ReportSectionFacadeREST();
        ReportSection reportSection = reportSectionFacadeREST.find(reportSectionId);
        entity.setReportSection(reportSection);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, ReportSectionLanguage entity) {
        ReportSectionLanguage existing = this.find(entity.getReportSectionLanguageId());
        if(existing != null) {
            existing.setTitle(entity.getTitle());
            existing.setBody(entity.getBody());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public ReportSectionLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }



}
