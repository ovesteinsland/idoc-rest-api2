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
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
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

    private static ReportFacadeREST instance;


    public ReportFacadeREST() {
        super(Report.class);
        instance = this;
    }

    public static ReportFacadeREST getInstance() {
        if (instance == null) {
            instance = new ReportFacadeREST();
        }
        return instance;
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
        //linkToProject(projectId, entity);
        linkToProjectNative(projectId, entity.getReportId());
        linkToDisipline(disiplineId, entity);

    }

    @POST
    @Path("createWithCompany/{companyId}/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, @PathParam("disiplineId") String disiplineId, Report entity) {

        createList(entity);
        super.create(entity);
        linkToCompanyNative(companyId, entity.getReportId());
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
            for(ReportSectionLanguage reportSectionLanguage : reportSection.getReportSectionLanguageList()) {
                reportSectionLanguage.setReportSection(reportSection);
            }
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
        Project project = ProjectFacadeREST.getInstance().find(projectId);
        if (project != null && report != null) {
            if (!project.getReportList().contains(report)) {
                project.getReportList().add(report);
            }
            if (!report.getProjectList().contains(project)) {
                report.getProjectList().add(project);

            }
            ProjectFacadeREST.getInstance().editProjectOnly(project.getProjectId(),project);
            super.edit(report);
        }
    }

    public void linkToProjectNative(String projectId, String reportId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                    SELECT COUNT(*) FROM project_has_report
                    WHERE project_project_id = ?1 AND report_report_id = ?2
                    """)
                        .setParameter(1, projectId)
                        .setParameter(2, reportId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                        INSERT INTO project_has_report (project_project_id, report_report_id)
                        VALUES (?, ?)
                        """)
                            .setParameter(1, projectId)
                            .setParameter(2, reportId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Advarsel ved linking av rapport til prosjekt (kan allerede eksistere): " + exp.getMessage());
                // Aksepterer at linken kan eksistere fra før - ingen exception kastes
            }
        } catch (Exception e) {
            System.out.println("Feil ved opprettelse av EntityManager: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke linke rapport til prosjekt", e);
        }
    }

    @PUT
    @Path("linkToCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId, Report entity) {
        Report report = this.find(entity.getReportId());
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        if (company != null && report != null) {
            if (!company.getReportList().contains(report)) {
                company.getReportList().add(report);
            }
            if (!report.getCompanyList().contains(company)) {
                report.getCompanyList().add(company);

            }
            CompanyFacadeREST.getInstance().edit(company);
            super.edit(report);
        }
    }

    public void linkToCompanyNative(String companyId, String reportId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                    SELECT COUNT(*) FROM company_has_report
                    WHERE company_company_id = ?1 AND report_report_id = ?2
                    """)
                        .setParameter(1, companyId)
                        .setParameter(2, reportId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                        INSERT INTO company_has_report (company_company_id, report_report_id)
                        VALUES (?, ?)
                        """)
                            .setParameter(1, companyId)
                            .setParameter(2, reportId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Advarsel ved linking av rapport til selskap (kan allerede eksistere): " + exp.getMessage());
                // Aksepterer at linken kan eksistere fra før - ingen exception kastes
            }
        } catch (Exception e) {
            System.out.println("Feil ved opprettelse av EntityManager: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke linke rapport til selskap", e);
        }
    }

    @PUT
    @Deprecated
    @Path("linkToDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToDisipline(@PathParam("disiplineId") String disiplineId, Report entity) {
        Report report = this.find(entity.getReportId());
        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
        if (disipline != null && report != null) {
//            if (!disipline.getReportList().contains(report)) {
//                disipline.getReportList().add(report);
//            }
            report.setDisipline(disipline);
            DisiplineFacadeREST.getInstance().edit(disipline);
            super.edit(report);
        }
    }

    @PUT
    @Path("removeSections/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void removeSections(@PathParam("id") String id, Report entity) {
        Report report = this.find(entity.getReportId());
        List<ReportSection> reportSections = new ArrayList(report.getReportSectionList());
        for (int i = 0; i < reportSections.size(); i++) {
            String sectionId = reportSections.get(i).getReportSectionId();
            ReportSectionFacadeREST.getInstance().remove(sectionId);
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
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        if (company != null) {
            for (Report report : company.getReportList()) {
                result.add(report);
            }
        }
        return result;
    }

    @GET
    @Path("loadTemplateByCompany/{companyId}/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> loadTemplateByCompany(@PathParam("companyId") String companyId, @PathParam("disiplineId") String disiplineId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Report> resultList = (List<Report>) em.createNativeQuery("""
                SELECT * FROM report r
                JOIN company_has_report chr ON chr.report_report_id = r.report_id
                WHERE chr.company_company_id = ?1 AND r.disipline = ?2
                """, Report.class)
                    .setParameter(1, companyId)
                    .setParameter(2, disiplineId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av rapportmaler for selskap: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste rapportmaler for selskap", e);
        }
    }

    @GET
    @Path("loadMasterByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> loadMasterByDisipline(@PathParam("disiplineId") String disiplineId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Report> resultList = (List<Report>) em.createNativeQuery("""
                SELECT DISTINCT r.* FROM report r
                LEFT JOIN report_section rs ON rs.report = r.report_id
                LEFT JOIN report_section_language rsl ON rsl.report_section = rs.report_section_id
                WHERE r.disipline = ?1 AND r.is_master = 1
                """, Report.class)
                    .setParameter(1, disiplineId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av master-rapporter for disiplin: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste master-rapporter for disiplin", e);
        }
    }

    @GET
    @Path("loadByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> loadByProject(@PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Report> resultList = (List<Report>) em.createNativeQuery("""
                SELECT DISTINCT r.* FROM report r
                JOIN project_has_report phr ON phr.report_report_id = r.report_id
                LEFT JOIN report_section rs ON rs.report = r.report_id
                LEFT JOIN report_section_language rsl ON rsl.report_section = rs.report_section_id
                WHERE phr.project_project_id = ?1
                """, Report.class)
                    .setParameter(1, projectId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av rapporter for prosjekt: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste rapporter for prosjekt", e);
        }
    }

    //
    @GET
    @Path("loadAllMasters")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Report> loadAllMasters() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Report> resultList = (List<Report>) em.createNativeQuery("""
                SELECT * FROM report
                WHERE is_master = 1
                """, Report.class)
                    .getResultList();

            for (Report report : resultList) {
                report.setDisipline(null);
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av alle master-rapporter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste alle master-rapporter", e);
        }
    }

    @GET
    @Deprecated
    @Path("findByProject/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Report> findByProject(@PathParam("projectId") String projectId) {
        List<Report> result = new ArrayList<>();
        Project project = ProjectFacadeREST.getInstance().find(projectId);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Report> resultList = (List<Report>) em.createNativeQuery("""
                SELECT * FROM report
                WHERE disipline = ?1 AND is_master = ?2
                """, Report.class)
                    .setParameter(1, disiplineId)
                    .setParameter(2, true)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter root-rapporter for disiplin: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke finne root-rapporter for disiplin", e);
        }
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
