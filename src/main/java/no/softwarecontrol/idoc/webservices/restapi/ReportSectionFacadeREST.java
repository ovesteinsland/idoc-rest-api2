/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.AssetType;
import no.softwarecontrol.idoc.data.entityobject.Report;
import no.softwarecontrol.idoc.data.entityobject.ReportSection;
import no.softwarecontrol.idoc.data.entityobject.ReportSectionLanguage;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.reportsection")
@RolesAllowed({"ApplicationRole"})
public class ReportSectionFacadeREST extends AbstractFacade<ReportSection> {
    private static ReportSectionFacadeREST instance;


    public ReportSectionFacadeREST() {
        super(ReportSection.class);
        instance = this;
    }

    public static ReportSectionFacadeREST getInstance() {
        if (instance == null) {
            instance = new ReportSectionFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return "ReportSection.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(ReportSection entity) {
        super.create(entity);
        System.out.println("Report Section Created");
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("createWithReport/{reportId}")
    public void createWithReport(@PathParam("reportId") String reportId, ReportSection entity) {
        Report report = ReportFacadeREST.getInstance().find(reportId);
        if (report != null) {
            entity.setReport(report);
            create(entity);
        }
        System.out.println("Report Section Created");
    }

    @PUT
    @Path("{languageCode}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("languageCode") String languageCode, ReportSection entity) {
        ReportSection reportSection = find(entity.getReportSectionId());
        reportSection.setTitle(entity.getTitle());
        reportSection.setBody(entity.getBody());
        reportSection.setIsHidden(entity.getIsHidden());
        reportSection.setIsIgnored(entity.getIsIgnored());
        reportSection.setSortIndex(entity.getSortIndex());
        reportSection.setMasterSection(entity.getMasterSection());

        List<ReportSectionLanguage> existingLanguages = reportSection.getReportSectionLanguageList().stream().filter(r -> r.getLanguageCode().equalsIgnoreCase(languageCode)).toList();
        if (!existingLanguages.isEmpty()) {
            existingLanguages.get(0).setTitle(reportSection.getTitle());
            existingLanguages.get(0).setBody(reportSection.getBody());
        }

        super.edit(reportSection);
    }

    @PUT
    @Path("linkToReport/{reportId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToReport(@PathParam("reportId") String reportId, ReportSection entity) {
        ReportSection reportSection = this.find(entity.getReportSectionId());
        Report report = ReportFacadeREST.getInstance().find(reportId);
        if (reportSection != null && report != null) {
            if (!report.getReportSectionList().contains(reportSection)) {
                report.getReportSectionList().add(reportSection);
            }
            reportSection.setReport(report);
            super.edit(reportSection);
            ReportFacadeREST.getInstance().edit(report);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public ReportSection find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<ReportSection> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ReportSection> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


}
