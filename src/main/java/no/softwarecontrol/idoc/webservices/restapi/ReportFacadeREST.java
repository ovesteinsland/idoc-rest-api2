/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.report")
@RolesAllowed({"ApplicationRole"})
public class ReportFacadeREST extends AbstractFacade<Report> {

    @EJB
    private ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
    @EJB
    private CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
    @EJB
    private DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();

    public ReportFacadeREST() {
        super(Report.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Report.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Report entity) {
        super.create(entity);
    }

    @POST
    @Path("createWithProject/{projectId}/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithProject(@PathParam("projectId") String projectId, @PathParam("disiplineId") String disiplineId, Report entity) {
        createList(entity);
        super.create(entity);
        linkToProject(projectId, entity);
        linkToDisipline(disiplineId, entity);
    }

    @POST
    @Path("createWithCompany/{companyId}/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, @PathParam("disiplineId") String disiplineId, Report entity) {
        createList(entity);
        super.create(entity);
        linkToCompany(companyId, entity);
        linkToDisipline(disiplineId, entity);
    }

    @POST
    @Path("createWithDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithDisipline(@PathParam("disiplineId") String disiplineId, Report entity) {
        createList(entity);
        super.create(entity);
        //linkToProject(disiplineId, entity);
    }

    private void createList(Report entity) {
        for (ReportSection reportSection : entity.getReportSectionList()) {
            reportSection.setReport(entity);
        }
        for (ReportParameter reportParameter : entity.getReportParameterList()) {
            reportParameter.setReport(entity);
        }
    }

    @PUT
    @Path("linkToProject/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToProject(@PathParam("projectId") String projectId, Report entity) {
        Report report = this.find(entity.getReportId());
        Project project = projectFacadeREST.find(projectId);
        if (project != null && report != null) {
            if (!project.getReportList().contains(report)) {
                project.getReportList().add(report);
            }
            if (!report.getProjectList().contains(project)) {
                report.getProjectList().add(project);

            }
            projectFacadeREST.edit(project);
            super.edit(report);
        }
    }

    @PUT
    @Path("linkToCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId, Report entity) {
        Report report = this.find(entity.getReportId());
        Company company = companyFacadeREST.find(companyId);
        if (company != null && report != null) {
            if (!company.getReportList().contains(report)) {
                company.getReportList().add(report);
            }
            if (!report.getCompanyList().contains(company)) {
                report.getCompanyList().add(company);

            }
            companyFacadeREST.edit(company);
            super.edit(report);
        }
    }

    @PUT
    @Path("linkToDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToDisipline(@PathParam("disiplineId") String disiplineId, Report entity) {
        Report report = this.find(entity.getReportId());
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        if (disipline != null && report != null) {
            if (!disipline.getReportList().contains(report)) {
                disipline.getReportList().add(report);
            }
            report.setDisipline(disipline);
            disiplineFacadeREST.edit(disipline);
            super.edit(report);
        }
    }

    @PUT
    @Path("removeSections/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void removeSections(@PathParam("id") String id, Report entity) {
        Report report = this.find(entity.getReportId());
        ReportSectionFacadeREST reportSectionFacadeREST = new ReportSectionFacadeREST();
        List<ReportSection> reportSections = new ArrayList(report.getReportSectionList());
        for (int i = 0; i < reportSections.size(); i++) {
            String sectionId = reportSections.get(i).getReportSectionId();
            reportSectionFacadeREST.remove(sectionId);
        }
        report.getReportSectionList().clear();
        super.edit(report);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Report entity) {
        for (ReportSection section : entity.getReportSectionList()) {
            section.setReport(entity);
        }

        Report report = this.find(entity.getReportId());
        report.setTitle(entity.getTitle());
        report.setSubtitle(entity.getSubtitle());
        report.setGeneratedDate(entity.getGeneratedDate());

        for (ReportSection reportSection : entity.getReportSectionList()) {
            if (!report.getReportSectionList().contains(reportSection)) {
                report.getReportSectionList().add(reportSection);
            }
        }

        //report.setReportSectionList(entity.getReportSectionList());
        super.edit(report);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("findByCompany/{companyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findByCompany(@PathParam("companyId") String companyId) {
        List<Report> result = new ArrayList<>();
        Company company = companyFacadeREST.find(companyId);
        if (company != null) {
            for (Report report : company.getReportList()) {
                result.add(report);
            }
        }
        return result;
    }

    @GET
    @Path("findByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findByProject(@PathParam("projectId") String projectId) {
        List<Report> result = new ArrayList<>();
        Project project = projectFacadeREST.find(projectId);
        if (project != null) {
            for (Report report : project.getReportList()) {
                result.add(report);
            }
        }
        return result;
    }

    @GET
    @Path("findRootByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findRootByDisipline(@PathParam("disiplineId") String disiplineId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        List<Report> resultList = (List<Report>) em.createNativeQuery("SELECT *\n"
                        + "FROM report \n"
                        + "WHERE disipline = ?1 AND is_master = ?2",
                Report.class)
                .setParameter(1, disiplineId)
                .setParameter(2, true)
                .getResultList();

        em.close();
        return resultList;
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Report find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
}
