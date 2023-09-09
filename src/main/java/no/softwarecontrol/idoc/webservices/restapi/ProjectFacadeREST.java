/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityhelper.*;
import no.softwarecontrol.idoc.data.entityjson.ProjectLite;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.entityobject.query.ResultItem;
import no.softwarecontrol.idoc.data.requestparams.ProjectRequestParameters;
import no.softwarecontrol.idoc.statistics.StatisticsFactory;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
//import no.softwarecontrol.idoc.webservices.zoho.ZohoClient;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.project")
//@RolesAllowed({"ApplicationRole"})
public class ProjectFacadeREST extends AbstractFacade<Project> {

    public ProjectFacadeREST() {
        super(Project.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Project.findAll";
    }


    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Project entity) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Project existing = find(entity.getProjectId());
        if (existing == null) {
            for (Project child : entity.getProjectList()) {
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
                if (child.getStartDate() == null) {
                    child.setStartDate(new Date());
                }
                child.setParent(entity);
                if (child.getAsset() != null) {
                    child.setParent(entity);
                    if (child.getAsset() != null) {
                        Asset existingAsset = assetFacadeREST.find(child.getAsset().getAssetId());
                        if (existingAsset != null) {
                            child.setAsset(existingAsset);
                            if (!existingAsset.getProjectList().contains(child)) {
                                existingAsset.getProjectList().add(child);
                            }
                        }
                    }
                }
            }
            entity.setCreatedDate(new Date());
            entity.setModifiedDate(new Date());
            if (entity.getStartDate() == null) {
                entity.setStartDate(new Date());
            }
            if (entity.getCreatedCompany() != null) {
                if (!entity.getCreatedCompany().isEmpty()) {
                    CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
                    ProjectNumber projectNumber = companyFacadeREST.incrementProjectCounter(entity.getCreatedCompany());
                    entity.setProjectNumber(projectNumber.getProjectCounter());
                }
            }
            super.create(entity);
            for (Project child : entity.getProjectList()) {
                if (child.getAsset() != null) {
                    assetFacadeREST.edit(child.getAsset());
                }
            }
            //this.edit(entity);
        }
    }


    /**
     * createStack is a new api for creating project. No need for linking users and companies after creation
     * accepts both single projects and group projects
     *
     * @param customerId
     * @param authorityId
     * @param disiplineId
     * @param params
     * @return a new created project
     */
    @POST
    @Path("createStack/{customerId}/{authorityId}/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Project createStack(@PathParam("customerId") String customerId,
                               @PathParam("authorityId") String authorityId,
                               @PathParam("disiplineId") String disiplineId,
                               List<List<String>> params) {

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company customer = companyFacadeREST.find(customerId);

        List<String> assetIds = params.get(0);
        List<String> userIds = params.get(1);
        Project project = createMemoryProject(authorityId, disiplineId, assetIds);
        project.setName(customer.getFullName());
        project.setProjectState(0);
        project.setGrouped(Boolean.TRUE);
        project.setCreatedCompany(authorityId);
        create(project);

        linkToCompany(authorityId, project);
        linkToCompany(customerId, project);

        for (Project child : project.getProjectList()) {
            linkToCompany(authorityId, child);
            linkToCompany(customerId, child);
        }

        for (String userId : userIds) {
            linkToUser(userId, project);
            for (Project child : project.getProjectList()) {
                linkToUser(userId, child);
            }
        }
        return project;
    }

    private Project createMemoryProject(String authorityId, String disiplineId, List<String> assetIds) {
        Project project = new Project();
        project.setProjectId(UUID.randomUUID().toString());

        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Disipline disipline = disiplineFacadeREST.find(disiplineId);

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        ProjectNumber projectNumber = companyFacadeREST.incrementProjectCounter(authorityId);

        project.setProjectNumber(projectNumber.getProjectCounter());
        project.setState(Project.State.INIT);
        project.setDeleted(false);
        project.setDisipline(disipline);
        project.setCreatedCompany(authorityId);
        if (assetIds.size() == 1) {
            String assetId = assetIds.get(0);
            Asset asset = assetFacadeREST.find(assetId);
            project.setGrouped(false);
            project.setName(asset.getDefaultName());
            project.setAsset(asset);
        } else {
            project.setGrouped(true);
            for (String assetId : assetIds) {
                Asset asset = assetFacadeREST.find(assetId);
                Project child = new Project();
                ProjectNumber childNumber = companyFacadeREST.incrementProjectCounter(authorityId);
                child.setState(Project.State.INIT);
                child.setDeleted(false);
                child.setAsset(asset);
                child.setName(asset.getDefaultName());
                child.setProjectNumber(childNumber.getProjectCounter());
                child.setProjectId(UUID.randomUUID().toString());
                child.setDisipline(disipline);
                child.setGrouped(false);
                child.setCreatedCompany(authorityId);
                project.getProjectList().add(child);
            }
        }
        return project;
    }


    /**
     * createWithParameters
     *
     * @param entity @see no.softwarecontrol.idoc.data.entityhelper.ProjectParameter
     * @summary Create Project with Parameters
     */
    @POST
    @Path("createWithParameters")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithParameters(ProjectParameters entity) {

        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();

        // Check if project exists
        Project project = entity.getProject();
        Project existingProject = find(project.getProjectId());
        Asset asset = assetFacadeREST.find(entity.getAssetId());
        Company authority = companyFacadeREST.findNative(entity.getAuthorityId());
        Company customer = companyFacadeREST.findNative(entity.getCustomerId());
        Disipline disipline = disiplineFacadeREST.find(entity.getDisiplineId());

        // Verify that all users are stored in the database
        List<User> existingUsers = new ArrayList<User>();
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        for (User incomingUser : project.getUserList()) {
            User existingUser = userFacadeREST.find(incomingUser.getUserId());
            if (existingUser == null) {
                userFacadeREST.create(incomingUser);
            }
        }
        if (existingProject == null) {
            if (asset != null) {
                project.setAsset(asset);
            }
            project.setDisipline(disipline);
            project.setCreatedDate(new Date());
            project.setModifiedDate(new Date());
            if (project.getStartDate() == null) {
                project.setStartDate(new Date());
            }
            ProjectNumber projectNumber = companyFacadeREST.incrementProjectCounter(entity.getAuthorityId());
            project.setProjectNumber(projectNumber.getProjectCounter());

            List<Project> existingChildren = new ArrayList<>();
            for (Project child : project.getProjectList()) {
                child.setParent(project);
                if (find(child.getProjectId()) != null) {
                    existingChildren.add(child);
                }
            }
            for (Project child : existingChildren) {
                project.getProjectList().remove(child);
            }

            if (!existingChildren.isEmpty()) {
                for (Project child : existingChildren) {
                    project.getProjectList().add(child);
                }
            }
            for (Project child : project.getProjectList()) {
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
                child.setParent(project);
                ProjectNumber childNumber = companyFacadeREST.incrementProjectCounter(entity.getAuthorityId());
                child.setProjectNumber(childNumber.getProjectCounter());
                if (child.getDisipline() == null) {
                    child.setDisipline(project.getDisipline());
                }
                if (child.getAsset() != null) {
                    Asset existingAsset = assetFacadeREST.find(child.getAsset().getAssetId());
                    if (existingAsset != null) {
                        child.setAsset(existingAsset);
                    }
                }
            }

            for (Project child : project.getProjectList()) {
                child.getUserList().clear();
            }
            for (UserRole userRole : project.getUserRoleList()) {
                userRole.setProject(project);
            }
            super.create(project);

//            List<User> users = new ArrayList<>(project.getUserList());
//            project.getUserList().clear();
//            project.getUserList().addAll(users);
            // Link companies and users
            for (User user : project.getUserList()) {
                linkToUser(user.getUserId(), project);
            }
            linkToCompany(entity.getAuthorityId(), project);
            linkToCompany(entity.getCustomerId(), project);
            for (Project child : project.getProjectList()) {
                linkToCompany(entity.getAuthorityId(), child);
                linkToCompany(entity.getCustomerId(), child);
                for (User user : project.getUserList()) {
                    linkToUser(user.getUserId(), child);
                }
            }
            editProjectOnly(project.getProjectId(), project);
            // Incrementing projectCounter in Zoho
//            ZohoClient zohoClient = new ZohoClient();
//            zohoClient.incrementProjectCounter(authority.getCompanyId());
        } else {
            System.out.println("Prosjektet eksisterer fra før.......????!!");
        }
    }


    public void linkUser(String userId, Project entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM project_has_user \n " +
                            " WHERE project_project_id = ?1 AND user_user_id = ?2")
                    .setParameter(1, entity.getProjectId())
                    .setParameter(2, userId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO project_has_user (project_project_id, user_user_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, entity.getProjectId())
                        .setParameter(2, userId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: observation_has_measurement already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into project_has_user: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("replaceCustomer/{newCustomerId}/{oldCustomerId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Project replaceCustomer(@PathParam("newCustomerId") String newCustomerId, @PathParam("oldCustomerId") String oldCustomerId, Project entity) {

        linkToCompany(newCustomerId, entity);
        unlinkCompany(oldCustomerId, entity);

        Project project = findNative(entity.getProjectId());
        project.setModifiedDate(new Date());
        editProjectOnly(project.getProjectId(), project);

        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        if (project.getAsset() != null) {
            assetFacadeREST.replaceCustomer(oldCustomerId, newCustomerId, project.getAsset());
        }
        return project;
    }

    @PUT
    @Path("linkParent/{parentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkParent(@PathParam("parentId") String parentId, Project entity) {
        Project child = this.find(entity.getProjectId());
        Project parent = this.find(parentId);
        if (parent != null && child != null) {
            child.setParent(parent);
            parent.getProjectList().add(child);
            parent.setModifiedDate(new Date());
            super.edit(parent);
            super.edit(child);
        } else {
            System.out.println("linkParent FAILED");
        }
    }

    private void linkObservations(Project project, List<Observation> observations) {
        for (Observation observation : observations) {
            observation.setProject(project);
            if (!observation.getEquipmentId().isEmpty()) {
                EquipmentFacadeREST equipmentFacadeREST = new EquipmentFacadeREST();
                Equipment equipment = equipmentFacadeREST.find(observation.getEquipmentId());
                observation.setEquipment(equipment);
            }
            if (!project.getObservationList().contains(observation)) {
                project.getObservationList().add(observation);
            }
        }
    }

    @POST
    @Path("createWithReferences/{assetId}/{authorityId}/{customerId}/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithReferences(@PathParam("assetId") String assetId,
                                     @PathParam("authorityId") String authorityId,
                                     @PathParam("customerId") String customerId,
                                     @PathParam("disiplineId") String disiplineId,
                                     Project entity) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        // Check if project exists


        Asset asset = assetFacadeREST.find(assetId);

        entity.setAsset(asset);
        asset.getProjectList().add(entity);

        Company authority = companyFacadeREST.findNative(authorityId);
        authority.getProjectList().add(entity);
        entity.getCompanyList().add(authority);

        Company customer = companyFacadeREST.findNative(customerId);
        customer.getProjectList().add(entity);
        entity.getCompanyList().add(customer);

        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        disipline.getProjectList().add(entity);
        entity.setDisipline(disipline);
        entity.setCreatedDate(new Date());
        entity.setModifiedDate(new Date());
        Project existingProject = find(entity.getProjectId());
        if (existingProject == null) {
            super.create(entity);
        } else {
            System.out.println("Prosjektet eksisterer fra før.......????!!");
        }
        disiplineFacadeREST.edit(disipline);
        assetFacadeREST.edit(asset);
        companyFacadeREST.edit(authority);
        companyFacadeREST.edit(customer);
        disiplineFacadeREST.edit(disipline);

        super.edit(entity);
    }

    @POST
    @Path("createWithCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, Project entity) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company company = companyFacadeREST.findNative(companyId);
        Project project = entity;
        super.create(project);
        // reconnect asset to child projects
        for (Project children : project.getProjectList()) {
            children.setParent(project);
            if (children.getAsset() != null) {
                linkChildrenToAsset(children, entity);
            }
            this.edit(children);
        }
        if (company != null) {
            if (!project.getCompanyList().contains(company)) {
                project.getCompanyList().add(company);
                this.edit(project);
            }
            if (!company.getProjectList().contains(project)) {
                company.getProjectList().add(project);
                companyFacadeREST.edit(company);
            }
        }
    }


    private void linkChildrenToAsset(Project child, Project entity) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        child.setParent(entity);
        if (child.getAsset() != null) {
            Asset existingAsset = assetFacadeREST.find(child.getAsset().getAssetId());
            if (existingAsset != null) {
                child.setAsset(existingAsset);
                if (!existingAsset.getProjectList().contains(child)) {
                    existingAsset.getProjectList().add(child);
                }
            }
            assetFacadeREST.edit(existingAsset);
        }
        this.edit(child);
    }


    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Project entity) {
        editProjectOnly(id, entity);
//        entity.setModifiedDate(new Date());
//        for (Project child : entity.getProjectList()) {
//            linkChildrenToAsset(child, entity);
//        }
//        super.edit(entity);
    }

    @PUT
    @Path("removeFromGroup/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void removeFromGroup(@PathParam("id") String id, Project entity) {
        Project project = this.find(id);
        Project parentProject = project.getParent();
        parentProject.getProjectList().remove(project);
        project.setParent(null);

        super.edit(parentProject);
        super.edit(project);
    }

    @GET
    @Path("updateStartDateForAllProjects")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer updateStartDateForAllProjects() {
        Integer counter = 0;

        EntityManager em2 = LocalEntityManagerFactory.createEntityManager();
        List<Project> projects = (List<Project>) em2.createNativeQuery("SELECT "
                                + "p.* FROM project p\n"
                                + "WHERE p.start_date is null LIMIT 0,10000",
                        Project.class)
                .getResultList();

        //projects = projects.subList(0,2);
        em2.close();
        for (Project project : projects) {
            if (project.getStartDate() == null && project.getCreatedDate() != null) {
                EntityManager em = LocalEntityManagerFactory.createEntityManager();
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    project.setStartDate(project.getCreatedDate());
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                    dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String strStartDate = dateFormatter.format(project.getCreatedDate());

                    final int i = em.createNativeQuery(
                                    "UPDATE project SET start_date = ?\n" +
                                            "WHERE (project_id = ?);"
                            ).setParameter(1, strStartDate)
                            .setParameter(2, project.getProjectId())
                            .executeUpdate();
                    tx.commit();
                    counter++;
                    System.out.println("updated = " + counter);
                } catch (Exception exp) {
                    tx.rollback();
                } finally {
                    em.close();
                }
            }
        }
        return counter;
    }

    @GET
    @Path("getProjectCounter/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer getProjectCounter(@PathParam("projectId") String id) {
        Project project = findNative(id);
        if (project != null) {
            Integer counter = project.getProjectNumber();
            if (counter == 0) {
                CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
                ProjectNumber pn = companyFacadeREST.incrementProjectCounter(project.getCreatedCompany());
                counter = pn.getProjectCounter();
                project.setProjectNumber(counter);
                editProjectOnly(project.getProjectId(), project);
            }
            return counter;
        }
        return -1;
    }

    @PUT
    @Path("editNative/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editNative(@PathParam("id") String id, Project entity) {

    }


    @PUT
    @Path("editProjectOnly/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editProjectOnly(@PathParam("id") String id, Project entity) {
        Project project = this.findNative(id);
        if (project != null) {
            project.setDeleted(entity.isDeleted());
            if (entity.getCreatedCompany() != null) {
                project.setCreatedCompany(entity.getCreatedCompany());
            }

            project.setModifiedDate(new Date());
            project.setModifiedUser(entity.getModifiedUser());

            project.setName(entity.getName());
            project.setOrderNo(entity.getOrderNo());
            project.setCustomerRef(entity.getCustomerRef());
            project.setFreeText(entity.getFreeText());
            project.setProjectState(entity.getProjectState());
            if (entity.getProjectNumber() == 0) {

            } else {
                project.setProjectNumber(entity.getProjectNumber());
            }
            project.setNextControlDate(entity.getNextControlDate());
            if (entity.getStartDate() != null) {
                project.setStartDate(entity.getStartDate());
            }
            if (entity.getEndDate() != null) {
                project.setEndDate(entity.getEndDate());
            }
            project.setRecurring(entity.getRecurring());
            project.setDurationText(entity.getDurationText());

            project.setIntegrationList(entity.getIntegrationList());
            // Check if disipline has been changed

            if (entity.getDisipline() != null) {
                DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
                Disipline disipline = disiplineFacadeREST.find(entity.getDisipline().getDisiplineId());
                if (disipline != null) {
                    project.setDisipline(disipline);
                }
            } else {

            }

            // Check if subproject has been removed
            List<Project> removedProjects = new ArrayList<>();
            for (Project child : project.getProjectList()) {
                List<Project> existing = entity.getProjectList()
                        .stream()
                        .filter(r -> r.getProjectId().equalsIgnoreCase(child.getProjectId()))
                        .collect(Collectors.toList());
                if (existing.isEmpty()) {
                    removedProjects.add(child);
                }
            }
            /*for(Project removed : removedProjects){
                project.getProjectList().remove(removed);
                removed.setParent(null);
                super.edit(removed);
            }*/
            for (Project child : project.getProjectList()) {

                //child.setDeleted(project.isDeleted());
                child.setParent(project);
                try {
                    super.edit(child);
                } catch (Exception e) {
                    System.out.println("Could not edit....");
                }
            }
            super.edit(project);
        } else {
            // Restore
            ProjectParameters projectParameters = new ProjectParameters();
            if (entity.getAsset() != null) {
                projectParameters.setAssetId(entity.getAsset().getAssetId());
            }
            if (entity.getAuthority() != null) {
                projectParameters.setAuthorityId(entity.getAuthority().getCompanyId());
            }
            if (entity.getCustomer() != null) {
                projectParameters.setCustomerId(entity.getCustomer().getCompanyId());
            }
            CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
            Company authority = companyFacadeREST.findNative(entity.getCreatedCompany());
            UserFacadeREST userFacadeREST = new UserFacadeREST();
            List<Company> companies = new ArrayList<>();
            String createdUserId = null;
            for (User user : entity.getUserList()) {
                List<Company> userCompanies = userFacadeREST.findUserCompanies(user.getUserId());
                companies.addAll(userCompanies);
                for (Company userCompany : userCompanies) {
                    if (userCompany.getCompanyId().equalsIgnoreCase(entity.getCreatedCompany())) {
                        createdUserId = user.getUserId();
                    }
                }
            }
            if (createdUserId != null) {
                entity.setCreatedUser(createdUserId);
            }

            ProjectNumber pNumber = companyFacadeREST.incrementProjectCounter(entity.getCreatedCompany());
            entity.setProjectNumber(pNumber.getProjectCounter());
            entity.setCreatedDate(new Date());
            entity.setModifiedDate(new Date());
            for (Project child : entity.getProjectList()) {
                child.setParent(entity);
                ProjectNumber cNumber = companyFacadeREST.incrementProjectCounter(entity.getCreatedCompany());
                child.setProjectNumber(cNumber.getProjectCounter());
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
            }
            super.create(entity);
            for (Company company : companies) {
                linkToCompany(company.getCompanyId(), entity);
            }
            for (Project child : entity.getProjectList()) {
                for (Company company : companies) {
                    linkToCompany(company.getCompanyId(), child);
                }
            }
            System.out.println("editProjectOnly: Trying to save a project that does not exist: " + entity.getProjectId() + ": " + entity.getName());
        }
    }


    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

//    @GET
//    @Path("testCRM/{companyId}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public void testCRM(@PathParam("companyId") String companyId) {
//        ZohoClient zohoClient = new ZohoClient();
//        zohoClient.testCRM(companyId);
//    }

    @GET
    @Path("findOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Project findOptimized(@PathParam("id") String id) {
        Project project = findNative(id);
        optimizeProject(project, false);
        Gson gson = new GsonBuilder().create();

        project.getObservationList().clear();
        //System.out.println(gson.toJson(project));
        return project;
    }

    public Project findNative(String id) {
        Project project = null;
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "WHERE p.project_id = ?1",
                        Project.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (resultList.isEmpty()) {
            return null;
        } else {

            return resultList.get(0);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Project find(@PathParam("id") String id) {
        return findNative(id);
        //return super.find(id);
    }

    @GET
    @Path("loadParent/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadParent(@PathParam("id") String id) {
        List<Project> parents = new ArrayList<>();
        Project project = find(id);
        if (project.getParent() != null) {
            parents.add(project.getParent());
        }
        return parents;
    }

    @GET
    @Path("loadAsset/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadAsset(@PathParam("projectId") String projectId) {
        List<Asset> assets = new ArrayList<>();
        Project project = find(projectId);
        if (project != null) {
            if (project.getAsset() != null) {
                assets.add(project.getAsset());
            }
        }
        return assets;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("queryAllByAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryAllByAuthority(@PathParam("companyid") String id) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        //findByCompanyId
        List<ResultItem> resultItems = new ArrayList<>();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "p.project_id, p.name FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company c\n"
                                + "    on chp.company_company_id = c.company_id\n"
                                + "WHERE chp.company_company_id = ?1",
                        Project.class)
                .setParameter(1, id)
                .getResultList();
        for (Project project : resultList) {
            ResultItem item = new ResultItem();
            item.setName(project.getName());
            item.setId(project.getProjectId());
            resultItems.add(item);
        }
        em.close();
        return resultItems;
    }

    @GET
    @Path("queryByAuthority/{companyid}/{querystring}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryByAuthority(@PathParam("companyid") String id, @PathParam("querystring") String queryString) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        //queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.toString());
        queryString = "%" + queryString + "%";
        //findByCompanyId
        List<ResultItem> resultItems = new ArrayList<>();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "p.project_id, p.name FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company c\n"
                                + "    on chp.company_company_id = c.company_id\n"
                                + "WHERE chp.company_company_id = ?1 AND p.name LIKE ?2",
                        Project.class)
                .setParameter(1, id)
                .setParameter(2, queryString)
                .getResultList();
        for (Project project : resultList) {
            ResultItem item = new ResultItem();
            item.setName(project.getName());
            item.setId(project.getProjectId());
            resultItems.add(item);
        }
        em.close();
        return resultItems;


    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


    @PUT
    @Path("linkDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToDisipline(@PathParam("disiplineId") String disiplineId, Project entity) {
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Project project = this.findNative(entity.getProjectId());
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        if (disipline != null && project != null) {
            project.setDisipline(disipline);
            this.edit(project);
        }
    }

    @PUT
    @Path("linkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Project entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM project_has_user \n " +
                            " WHERE project_project_id = ?1 AND user_user_id = ?2")
                    .setParameter(1, entity.getProjectId())
                    .setParameter(2, userId);

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO project_has_user (project_project_id, user_user_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, entity.getProjectId())
                        .setParameter(2, userId)
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: project_has_user already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into project_has_user: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("unlinkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkToUser(@PathParam("userId") String userId, Project entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createNativeQuery("DELETE FROM project_has_user \n " +
                            " WHERE project_project_id = ?1 AND user_user_id = ?2")
                    .setParameter(1, entity.getProjectId())
                    .setParameter(2, userId);

            Number counter = (Number) query.executeUpdate();
            if (counter.intValue() == 1) {
                //System.out.println("DELETED company_has_project SUCCEEDED");
            }
            tx.commit();
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_project: " + exp.getMessage());
        } finally {
            em.close();
        }

    }

    @PUT
    @Path("linkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId, Project entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_project \n " +
                            " WHERE company_company_id = ?1 AND project_project_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, entity.getProjectId());

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                                "INSERT INTO company_has_project (company_company_id, project_project_id)\n" +
                                        "VALUES (?, ?);"
                        ).setParameter(1, companyId)
                        .setParameter(2, entity.getProjectId())
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: company_has_project already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_project: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("companyId") String companyId, Project entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createNativeQuery("DELETE FROM company_has_project \n " +
                            " WHERE company_company_id = ?1 AND project_project_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, entity.getProjectId());

            Number counter = (Number) query.executeUpdate();
            if (counter.intValue() == 1) {
                System.out.println("DELETED company_has_project SUCCEEDED");
            }
            tx.commit();
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_project: " + exp.getMessage());
        } finally {
            em.close();
        }
    }

    @PUT
    @Path("linkAsset/{assetid}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToAsset(@PathParam("assetid") String assetId, Project entity) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Project project = this.find(entity.getProjectId());
        Asset asset = assetFacadeREST.find(assetId);
        if (project != null && asset != null) {
            project.setAsset(asset);
            if (!asset.getProjectList().contains(project)) {
                asset.getProjectList().add(project);
                assetFacadeREST.edit(asset);
            }
        }
        this.edit(project);
    }

    @GET
    @Path("projectrefcompanies/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectListRefCompany findProjectsRefCompany(@PathParam("companyid") String companyId) {
        return findProjectsRefCompany(companyId, companyId, "2");
    }


    public List<Project> loadAllProjectsInPeriod(String fromDate,
                                                 String toDate,
                                                 String batchOffset,
                                                 String batchSize) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);
        DateTime jodaFromTime = new DateTime(fromDate);
        DateTime jodaToTime = new DateTime(toDate);

        List<Project> resultList = (List<Project>) em.createNativeQuery(
                        "SELECT distinct projects.* FROM " +
                                "(SELECT p.* FROM company c " +
                                "JOIN company_has_project chp on chp.company_company_id = c.company_id " +
                                "JOIN project p on chp.project_project_id = p.project_id " +
                                "WHERE c.company_type = 'AUTHORITY' AND " +
                                "p.parent is null AND " +
                                "p.created_date > ?1 AND " +
                                "p.created_date < ?2 AND " +
                                "p.created_company != 'E07121A7-024A-4D0E-8B58-A064F0BC4A22' AND " +
                                "(p.deleted is null or p.deleted = 0)) projects " +
                                "JOIN company_has_project chp on chp.project_project_id = projects.project_id " +
                                "JOIN company customer on chp.company_company_id = customer.company_id " +
                                "WHERE customer.company_type = 'OWNER' AND (customer.demo is null OR customer.demo = 0) " +
                                "ORDER BY projects.created_date " +
                                "LIMIT ?3,?4", Project.class)
                .setParameter(1, jodaFromTime.toString())
                .setParameter(2, jodaToTime.toString())
                .setParameter(3, offset)
                .setParameter(4, size)
                .getResultList();

        em.close();
        return resultList;
    }


    private List<WalletProject> loadWalletProjects(String fromDate,
                                                   String toDate,
                                                   int batchOffset,
                                                   int batchSize) {
        DateTime jodaFromTime = new DateTime(fromDate);
        DateTime jodaToTime = new DateTime(toDate);

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<WalletProject> resultList = (List<WalletProject>) em.createNativeQuery(
                        "SELECT \n" +
                                "   wallet_project.project_id as project_id, \n" +
                                "   wallet_project.created_date as created_date, \n" +
                                "   d.name as disipline_name, \n" +
                                "   auth.name as authority_name, \n" +
                                "   count(wallet_project.project_id) as point_count,\n" +
                                "   d.point_price as point_factor, \n" +
                                "   d.max_children as max_children,\n" +
                                "   (SELECT point_price \n" +
                                "       FROM invoice\n" +
                                "       WHERE company = wallet_project.created_company order by start_date DESC\n" +
                                "       LIMIT 0,1) as point_price,\n" +
                                "   (SELECT point_discount \n" +
                                "       FROM invoice\n" +
                                "       WHERE company = wallet_project.created_company order by start_date DESC\n" +
                                "       LIMIT 0,1) as point_discount\n" +
                                "FROM project as wallet_project\n" +
                                "   join disipline d on wallet_project.disipline = d.disipline_id     \n" +
                                "   left join company auth on wallet_project.created_company = auth.company_id     \n" +
                                "   left join project child ON child.parent = wallet_project.project_id   \n" +
                                "WHERE \n" +
                                "   wallet_project.created_date > ?1 and      \n" +
                                "   wallet_project.created_date < ?2 and  \n" +
                                "   wallet_project.created_company != \"E07121A7-024A-4D0E-8B58-A064F0BC4A22\" and     \n" +
                                "   (wallet_project.deleted = 0 or wallet_project.deleted is null) AND     \n" +
                                "   (child.deleted = 0 or child.deleted is null) and    \n" +
                                "   wallet_project.parent is null /*order by p.project_id*/ \n" +
                                "group by wallet_project.project_id \n" +
                                "order by auth.name LIMIT ?3, ?4", WalletProject.class
                )
                .setParameter(1, jodaFromTime.toString())
                .setParameter(2, jodaToTime.toString())
                .setParameter(3, batchOffset)
                .setParameter(4, batchSize)
                .getResultList();
        return resultList;
    }

    @GET
    @Path("loadProductionStatisticsForPeriod/{authorityId}/{fromDate}/{toDate}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProductionStatistic> loadProductionStatisticsForPeriod(@PathParam("authorityId") String authorityId,
                                                                       @PathParam("fromDate") String fromDateString,
                                                                       @PathParam("toDate") String toDateString) {


        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(authorityId);
        DateTime fromDate = new DateTime(fromDateString);
        DateTime toDate = new DateTime(toDateString);

        List<ProductionStatistic> productionStatistics = new ArrayList<>();

        if (authority.getCompanyType().equalsIgnoreCase("SOFTWARE_CONTROL")) {
            List<WalletProject> walletProjects = loadWalletProjects(fromDate.toString(), toDate.toString(), 0, 9999);
            productionStatistics.addAll(StatisticsFactory.createMonthly(walletProjects));
        }
        if (!productionStatistics.isEmpty()) {
            ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
            ProductionStatistic productionStatistic = productionStatistics.get(0);

            List<WalletProject> projects = loadWalletProjects(fromDate.toString(), toDate.toString(), 0, 9999);
            for (WalletProject walletProject : projects) {
                Double invoicePointPrice = 0.0;
                if (walletProject.pointPrice != null) {
                    invoicePointPrice = walletProject.pointPrice;
                } else {
                    invoicePointPrice = 240.0;
                }
                Double projectPointCount = walletProject.pointCount;
                if (projectPointCount > walletProject.maxChildren && walletProject.maxChildren != 0) {
                    projectPointCount = walletProject.maxChildren;
                }
                Double revenueToday = productionStatistic.getRevenueToday();
                if (revenueToday == null) {
                    revenueToday = 0.0;
                }
                Double pointDiscount = 1.0;
                if (walletProject.pointDiscount != null) {
                    pointDiscount = walletProject.pointDiscount;
                }
                productionStatistic.setRevenueToday(revenueToday + invoicePointPrice * projectPointCount * walletProject.pointFactor * pointDiscount);
            }
            int counterTG0 = observationFacadeREST.countObservationsInPeriod(fromDate.toString(), toDate.toString(), 0);
            int counterTG1 = observationFacadeREST.countObservationsInPeriod(fromDate.toString(), toDate.toString(), 1);
            int counterTG2 = observationFacadeREST.countObservationsInPeriod(fromDate.toString(), toDate.toString(), 2);
            int counterTG3 = observationFacadeREST.countObservationsInPeriod(fromDate.toString(), toDate.toString(), 3);

            productionStatistic.setTg0Count(counterTG0);
            productionStatistic.setTg1Count(counterTG1);
            productionStatistic.setTg2Count(counterTG2);
            productionStatistic.setTg3Count(counterTG3);
        }
        return productionStatistics;
    }

    @GET
    @Path("loadProductionStatistics/{authorityId}/{period}/{periodCounter}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProductionStatistic> loadProductionStatistics(@PathParam("authorityId") String authorityId,
                                                              @PathParam("period") String period,
                                                              @PathParam("periodCounter") String periodCounter) {

        int intPeriodCounter = Integer.parseInt(periodCounter);
        //intPeriodCounter = 13;
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(authorityId);
        DateTime firstDayOfMonth = new DateTime().dayOfMonth().withMinimumValue();
        firstDayOfMonth = firstDayOfMonth.withMillisOfDay(0);
        DateTime toDate = new DateTime(new Date());
        DateTime fromDate = firstDayOfMonth.minusMonths(intPeriodCounter - 1);

        List<ProductionStatistic> productionStatistics = new ArrayList<>();

        if (authority.getCompanyType().equalsIgnoreCase("SOFTWARE_CONTROL")) {
            List<WalletProject> walletProjects = loadWalletProjects(fromDate.toString(), toDate.toString(), 0, 9999);
            productionStatistics.addAll(StatisticsFactory.createMonthly(walletProjects));
        }
        if (!productionStatistics.isEmpty()) {
            ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
            ProductionStatistic productionStatistic = productionStatistics.get(0);

            final DateTime.Property millisOfDay = toDate.millisOfDay();
            //DateTime last24hour = toDate.minusDays(1);
            DateTime last24hour = toDate.minusMillis(millisOfDay.get());

            List<WalletProject> walletProjects = loadWalletProjects(last24hour.toString(), toDate.toString(), 0, 9999);
            for (WalletProject walletProject : walletProjects) {
                Double invoicePointPrice = 0.0;
                if (walletProject.pointPrice != null) {
                    invoicePointPrice = walletProject.pointPrice;
                } else {
                    invoicePointPrice = 240.0;
                }
                Double projectPointCount = walletProject.pointCount;
                if (projectPointCount > walletProject.maxChildren && walletProject.maxChildren != 0) {
                    projectPointCount = walletProject.maxChildren;
                }
                Double revenueToday = productionStatistic.getRevenueToday();
                if (revenueToday == null) {
                    revenueToday = 0.0;
                }
                Double pointDiscount = 1.0;
                if (walletProject.pointDiscount != null) {
                    pointDiscount = walletProject.pointDiscount;
                }
                productionStatistic.setRevenueToday(revenueToday + invoicePointPrice * projectPointCount * walletProject.pointFactor * pointDiscount);
            }
            int counterTG0 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 0);
            int counterTG1 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 1);
            int counterTG2 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 2);
            int counterTG3 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 3);

            productionStatistic.setTg0Count(counterTG0);
            productionStatistic.setTg1Count(counterTG1);
            productionStatistic.setTg2Count(counterTG2);
            productionStatistic.setTg3Count(counterTG3);
        }
        return productionStatistics;
    }


    @GET
    @Path("loadDisiplineGroups/{companyId}/{authorityId}/{state}/{fromDate}/{toDate}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<DisiplineGroup> loadDisiplineGroups(@PathParam("companyId") String companyId,
                                                    @PathParam("authorityId") String authorityId,
                                                    @PathParam("state") String state,
                                                    @PathParam("fromDate") String fromDate,
                                                    @PathParam("toDate") String toDate,
                                                    @PathParam("batchOffset") String batchOffset,
                                                    @PathParam("batchSize") String batchSize) {

        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);
        DateTime jodaFromDateTime = new DateTime(fromDate);
        DateTime jodaToDateTime = new DateTime(toDate);

        ProjectRequestParameters projectRequestParameters = new ProjectRequestParameters();
        projectRequestParameters.authorityId = authorityId;

        projectRequestParameters.states.add(0);
        projectRequestParameters.states.add(1);
        projectRequestParameters.states.add(5);
        projectRequestParameters.states.add(8);
        projectRequestParameters.states.add(9);

        projectRequestParameters.fromDate = jodaFromDateTime.toDate();
        projectRequestParameters.toDate = jodaToDateTime.toDate();
        if (projectRequestParameters.toDate == null) {
            projectRequestParameters.toDate = new Date();
        }

        projectRequestParameters.parentEntity = "company";
        projectRequestParameters.entityIds.add(authorityId);
        projectRequestParameters.batchOffset = 0;
        projectRequestParameters.batchSize = 9999;
        projectRequestParameters.excludeUpcoming = true;
        projectRequestParameters.isDeleted = false;

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(projectRequestParameters, false, "");
        List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                .setParameter(1, projectRequestParameters.authorityId)
                .getResultList();
        em.close();
        List<DisiplineGroup> disiplineGroups = DisiplineGroup.createDisiplineGroupList(resultList);

        return disiplineGroups;
    }


    @GET
    @Path("loadProjectsForCompany/{companyId}/{authorityId}/{state}/{fromDate}/{toDate}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsForCompany(@PathParam("companyId") String companyId,
                                                @PathParam("authorityId") String authorityId,
                                                @PathParam("state") String state,
                                                @PathParam("fromDate") String fromDate,
                                                @PathParam("toDate") String toDate,
                                                @PathParam("batchOffset") String batchOffset,
                                                @PathParam("batchSize") String batchSize) {

        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);
        DateTime jodaFromDateTime = new DateTime(fromDate);
        DateTime jodaToDateTime = new DateTime(toDate);
        List<Project> resultList = loadProjects(companyId, authorityId, state, jodaFromDateTime.toString(), jodaToDateTime.toString(), offset, size, true, false);

        List<Project> paidProjects = resultList.stream().filter(r -> {
            if (r.getCustomer() != null) {
                return r.getCustomer().isDemo() == false;
            } else {
                return true;
            }
        }).collect(Collectors.toList());

        return paidProjects;
    }

    @GET
    @Path("loadProjectsForCompanyOptimized/{companyId}/{authorityId}/{state}/{fromDate}/{toDate}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsForCompanyOptimized(@PathParam("companyId") String companyId,
                                                         @PathParam("authorityId") String authorityId,
                                                         @PathParam("state") String state,
                                                         @PathParam("fromDate") String fromDate,
                                                         @PathParam("toDate") String toDate,
                                                         @PathParam("batchOffset") String batchOffset,
                                                         @PathParam("batchSize") String batchSize) {

        List<Project> projects = loadProjectsForCompany(companyId,
                authorityId,
                state,
                fromDate,
                toDate,
                batchOffset,
                batchSize);
        for (Project project : projects) {
            project.setAsset(null);
            //project.getObservationList().clear();
            project.setDisipline(null);
        }
        return projects;
    }

    @GET
    @Path("projectrefcompanies/{companyid}/{authorityid}/{state}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectListRefCompany findProjectsRefCompany(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId, @PathParam("state") String state) {

        ProjectListRefCompany ref = new ProjectListRefCompany();
        List<Project> resultList = loadProjects(companyId, authorityId, state, null, null, 0, 10, false, false);
        for (Project project : resultList) {
            if (!ref.getProjects().contains(project)) {
                ref.getProjects().add(project);
            }
            for (Company com : project.getCompanyList()) {
//                if (!ref.getCompanies().contains(com)) {
//                    ref.getCompanies().add(com);
//                }
            }
        }
        return ref;
    }

    private List<Project> loadProjectsByAssetNative(String assetId,
                                                    String companyId,
                                                    String state,
                                                    //String fromDate,
                                                    //String toDate,
                                                    int batchOffset,
                                                    int batchSize) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

//        if (fromDate == null) {
//            fromDate = "0";
//        }
//        if (toDate == null) {
//            DateTime jodaDateTime = new DateTime();
//            toDate = jodaDateTime.toString();
//        }

//        String dateField = "p.modified_date";
//        if (isCreatedDate) {
//            dateField = "p.created_date";
//        }

        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT " +
                                "* FROM project p\n" +
                                "JOIN company_has_project chp ON chp.project_project_id = p.project_id\n" +
                                "JOIN asset a ON a.asset_id = p.asset\n" +
                                "WHERE \n" +
                                " a.asset_id = ?1 AND \n" +
                                " chp.company_company_id = ?2 AND \n" +
                                " p.project_state < ?3 AND \n" +
                                " p.deleted = ?4 \n" +
                                //"AND p.parent IS NULL\n" +
                                "ORDER BY p.modified_date DESC \n" +
                                "LIMIT ?5,?6",
                        Project.class)
                .setParameter(1, assetId)
                .setParameter(2, companyId)
                .setParameter(3, state)
                .setParameter(4, false)
                .setParameter(5, batchOffset)
                .setParameter(6, batchSize)
                .getResultList();

        for (Project project : resultList) {
            if (project.getAsset() != null) {
                List<Media> assetMedias = (List<Media>) em.createNativeQuery("SELECT "
                                        + "* FROM image img\n"
                                        + "JOIN asset_has_image ahi\n"
                                        + "	ON img.image_id = ahi.image\n"
                                        + "WHERE " +
                                        " ahi.asset = ?1",
                                Media.class)
                        .setParameter(1, project.getAsset().getAssetId())
                        .getResultList();
                project.getAsset().setImageList(assetMedias);
            }
        }
        em.close();
        return resultList;
    }

    private List<Project> loadProjects(String companyId,
                                       String authorityId,
                                       String state,
                                       String fromDate,
                                       String toDate,
                                       int batchOffset,
                                       int batchSize,
                                       boolean isCreatedDate,
                                       boolean isDeleted) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        if (fromDate == null) {
            fromDate = "0";
        }
        if (toDate == null) {
            DateTime jodaDateTime = new DateTime();
            toDate = jodaDateTime.toString();
        }

        String dateField = "p.modified_date";
        if (isCreatedDate) {
            dateField = "p.created_date";
        }

        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company_has_project ahp\n"
                                + "	ON ahp.project_project_id = p.project_id\n"
                                + "WHERE " +
                                " chp.company_company_id = ?1 AND" +
                                " ahp.company_company_id = ?2 AND " +
                                " p.project_state < ?3 AND " +
                                dateField + "  > ?4 AND " +
                                dateField + " < ?5 AND " +
                                " p.deleted = ?6 AND" +
                                " p.parent IS NULL\n"
                                + "ORDER BY p.modified_date DESC \n"
                                + "LIMIT ?7,?8",
                        Project.class)
                .setParameter(1, companyId)
                .setParameter(2, authorityId)
                .setParameter(3, state)
                .setParameter(4, fromDate)
                .setParameter(5, toDate)
                .setParameter(6, isDeleted)
                .setParameter(7, batchOffset)
                .setParameter(8, batchSize)
                .getResultList();

        for (Project project : resultList) {
            if (project.getAsset() != null) {
                List<Media> assetMedias = (List<Media>) em.createNativeQuery("SELECT "
                                        + "* FROM image img\n"
                                        + "JOIN asset_has_image ahi\n"
                                        + "	ON img.image_id = ahi.image\n"
                                        + "WHERE " +
                                        " ahi.asset = ?1",
                                Media.class)
                        .setParameter(1, project.getAsset().getAssetId())
                        .getResultList();
                project.getAsset().setImageList(assetMedias);
            }
        }
        em.close();
        return resultList;
    }

    @GET
    @Path("countForCompany/{companyid}/{authorityid}")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer countForCompany(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company_has_project ahp\n"
                                + "	ON ahp.project_project_id = p.project_id\n"
                                + "WHERE " +
                                " chp.company_company_id = ?1 AND" +
                                " p.parent IS NULL AND\n" +
                                " ahp.company_company_id = ?2",
                        Project.class)
                .setParameter(1, companyId)
                .setParameter(2, authorityId)
                .getResultList();
        em.close();
        return resultList.size();
    }

    /**
     * Loads latest project for a company and user
     *
     * @param companyId
     * @param authorityId
     * @param userId
     * @param state
     * @param strDate
     * @param excludeDeleted
     * @return
     */
    private ProjectListRefCompany loadProjects(String companyId,
                                               String authorityId,
                                               String userId,
                                               String state,
                                               String strDate,
                                               boolean excludeDeleted,
                                               int batchOffset,
                                               int batchSize) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        if (strDate == null) {
            strDate = "0";
        }
        ProjectListRefCompany ref = new ProjectListRefCompany();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company_has_project ahp\n"
                                + "	ON ahp.project_project_id = p.project_id\n"
                                + "JOIN project_has_user phu\n"
                                + "	ON phu.project_project_id = p.project_id\n"
                                + "WHERE " +
                                " chp.company_company_id = ?1 AND" +
                                " ahp.company_company_id = ?2 AND " +
                                " phu.user_user_id = ?3 AND " +
                                " p.project_state < ?4 AND " +
                                " p.modified_date > ?5 AND" +
                                " p.deleted = ?6 AND" +
                                " p.parent IS NULL\n"
                                + "ORDER BY p.modified_date DESC \n"
                                + "LIMIT ?7,?8",
                        Project.class)
                .setParameter(1, companyId)
                .setParameter(2, authorityId)
                .setParameter(3, userId)
                .setParameter(4, state)
                .setParameter(5, strDate)
                .setParameter(6, false)
                .setParameter(7, batchOffset)
                .setParameter(8, batchSize)
                .getResultList();

        ref.getProjects().addAll(resultList);
        em.close();
        return ref;
    }

    private List<Project> loadProjectsByUser(String companyId,
                                             String authorityId,
                                             String userId,
                                             String state,
                                             String strDate,
                                             boolean excludeDeleted,
                                             int batchOffset,
                                             int batchSize) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        if (strDate == null) {
            strDate = "0";
        }
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company_has_project ahp\n"
                                + "	ON ahp.project_project_id = p.project_id\n"
                                + "JOIN project_has_user phu\n"
                                + "	ON phu.project_project_id = p.project_id\n"
                                + "WHERE " +
                                " chp.company_company_id = ?1 AND" +
                                " ahp.company_company_id = ?2 AND " +
                                " phu.user_user_id = ?3 AND " +
                                " p.project_state < ?4 AND " +
                                " p.modified_date > ?5 AND" +
                                " p.deleted = ?6 AND" +
                                " p.parent IS NULL\n"
                                + "ORDER BY p.modified_date DESC \n"
                                + "LIMIT ?7,?8",
                        Project.class)
                .setParameter(1, companyId)
                .setParameter(2, authorityId)
                .setParameter(3, userId)
                .setParameter(4, state)
                .setParameter(5, strDate)
                .setParameter(6, false)
                .setParameter(7, batchOffset)
                .setParameter(8, batchSize)
                .getResultList();

        em.close();
        return resultList;
    }


    @GET
    @Path("loadProjectsByCompany/{companyid}/{authorityid}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectListRefCompany loadProjectsByCompany(@PathParam("companyid") String companyId,
                                                       @PathParam("authorityid") String authorityId,
                                                       @PathParam("state") String state,
                                                       @PathParam("batchOffset") String batchOffset,
                                                       @PathParam("batchSize") String batchSize) {
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        ProjectListRefCompany ref = new ProjectListRefCompany();
        List<Project> resultList = loadProjects(companyId, authorityId, state, null, null, offset, size, false, false);
        for (Project project : resultList) {
            if (!ref.getProjects().contains(project)) {
                ref.getProjects().add(project);
            }
        }
        return ref;
    }

    // ==========================================================================================
    // Not to confused with loadProjectsForCompany
    // ==========================================================================================
    @GET
    @Path("loadProjectsByCompany2/{companyid}/{authorityid}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsByCompany2(@PathParam("companyid") String companyId,
                                                @PathParam("authorityid") String authorityId,
                                                @PathParam("state") String state,
                                                @PathParam("batchOffset") String batchOffset,
                                                @PathParam("batchSize") String batchSize) {
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        List<Project> resultList = loadProjects(companyId, authorityId, state, null, null, offset, size, false, false);
        return resultList;
    }

    @GET
    @Path("loadProjectsByAsset/{assetid}/{authorityid}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsByAsset(@PathParam("assetid") String assetId,
                                             @PathParam("authorityid") String authorityId,
                                             @PathParam("state") String state,
                                             @PathParam("batchOffset") String batchOffset,
                                             @PathParam("batchSize") String batchSize) {

        List<Project> projects = loadProjectsByAssetNative(
                assetId,
                authorityId,
                state,
                Integer.parseInt(batchOffset),
                Integer.parseInt(batchSize));

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(authorityId);
        for (Project project : projects) {
            optimizeProject(project, authority.isFreeAccount());
        }
        return projects;
    }


    private String createSqlString(ProjectRequestParameters parameters, Boolean isCounting, String groupByField) {
        String sqlString = "";

        String selectString = "";
        String joinUserString = "";
        String queryUserString = "";
        String queryStateString = "";
        String joinParentString = "";
        String queryParentString = "";
        String queryDisiplineString = "";
        String queryDateString = "";
        String queryNextControlDateString = "";
        String groupByString = "";
        String orderLimitString = "";
        String deletedString = "0";

        if(parameters.isDeleted) {
            deletedString = "1";
        }

        if (parameters.nextControlDate != null) {
            selectString = "SELECT max(p.next_control_date), p.* FROM project p\n";
            if (parameters.fromDate == null) {
                parameters.fromDate = new Date(0);
            }
            if (parameters.toDate == null) {
                parameters.toDate = new Date(Long.MAX_VALUE);
            }

            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String strNextControlDate = dateFormatter.format(parameters.nextControlDate);
            queryNextControlDateString = "p.next_control_date < '" + strNextControlDate + "' AND \n";
            groupByString = "GROUP BY p.disipline \n";
            orderLimitString = "ORDER BY p.disipline, next_control_date DESC LIMIT " + parameters.batchOffset + "," + parameters.batchSize + " ";
        }

        if (isCounting) {
            if (parameters.nextControlDate != null) {
                selectString = "SELECT COUNT(*) FROM (SELECT max(p.next_control_date), p.* FROM project p\n";
                orderLimitString = orderLimitString + ") AS something;";
            } else {
                selectString = "SELECT count(distinct p.project_id) FROM project p\n";
            }
        } else {
            if (parameters.nextControlDate != null) {

            } else {
                selectString = "SELECT * FROM project p\n";
                if (parameters.batchOffset != null && parameters.batchSize != null) {
                    orderLimitString = " ORDER BY p.modified_date DESC LIMIT " + parameters.batchOffset + "," + parameters.batchSize + " ";
                }
            }
        }
        if (groupByString.isEmpty() && !groupByField.isEmpty()) {
            groupByString = "GROUP BY p." + groupByField + " \n";
        }

        if (!parameters.disiplineIds.isEmpty()) {
            String disiplinesString = "(";
            for (String disiplineId : parameters.disiplineIds) {
                disiplinesString += "'" + disiplineId + "',";
            }
            disiplinesString = disiplinesString.substring(0, disiplinesString.length() - 1);
            disiplinesString += ")";

            queryDisiplineString = " p.disipline in " + disiplinesString + " AND \n";
        }

        if (!parameters.states.isEmpty()) {
            String statesString = "(";
            for (Integer state : parameters.states) {
                statesString += state.toString() + ",";
            }
            statesString = statesString.substring(0, statesString.length() - 1);
            statesString += ")";
            queryStateString = " p.project_state in " + statesString + " AND \n";
        }

        if (parameters.fromDate != null && parameters.toDate != null) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            String strFromDate = dateFormatter.format(parameters.fromDate);
            String strToDate = dateFormatter.format(parameters.toDate);

            String dateField = parameters.dateField.getValue();
            queryDateString = " ((p." + dateField + " >= '" + strFromDate + "' AND p." + dateField + " <= '" + strToDate + "') OR " +
                    "(p.next_control_date >= '" + strFromDate + "' AND p.next_control_date < '" + strToDate + "')) AND \n";
            if (parameters.excludeUpcoming) {
                queryDateString = " (p." + dateField + " >= '" + strFromDate + "' AND p." + dateField + " <= '" + strToDate + "') AND \n";
            }
            if (parameters.isUpcoming) {
                queryDateString = " (p.next_control_date > '" + strFromDate + "' AND p.next_control_date < '" + strToDate + "') AND \n";
            }
        }

        if (!parameters.entityIds.isEmpty()) {
            if (parameters.parentEntity.equalsIgnoreCase("company")) {
                String companyId = parameters.entityIds.get(0);
                joinParentString = " JOIN company_has_project chp ON chp.project_project_id = p.project_id \n";
                queryParentString = " chp.company_company_id = '" + companyId + "' AND p.parent IS NULL \n";

            } else if (parameters.parentEntity.equalsIgnoreCase("asset")) {
                String assetId = parameters.entityIds.get(0);
                joinParentString = " JOIN asset a ON a.asset_id = p.asset \n";
                queryParentString = " a.asset_id = '" + assetId + "' \n";
            }
        }

        if (!parameters.userIds.isEmpty()) {
            String userId = parameters.userIds.get(0);
            joinUserString = " LEFT JOIN project_has_user phu ON phu.project_project_id = p.project_id \n";
            queryUserString = " phu.user_user_id = '" + userId + "' AND\n";
        }



        sqlString = selectString +
                " JOIN company_has_project ahp	ON ahp.project_project_id = p.project_id\n" +
                joinParentString +
                joinUserString +
                " WHERE \n" +
                " p.deleted = " + deletedString + " AND \n" +
                queryUserString +
                queryDisiplineString +
                queryStateString +
                queryDateString +
                " ahp.company_company_id = '" + parameters.authorityId + "' AND \n" +
                queryParentString +
                queryNextControlDateString +
                groupByString +
                orderLimitString;

        return sqlString;
    }

    @PUT
    @Path("loadRelevantDisiplines")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public List<Disipline> loadRelevantDisiplines(ProjectRequestParameters parameters) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();

//        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT * FROM project p\n" +
//                        "JOIN company_has_project chp ON chp.project_project_id = p.project_id\n" +
//                        "JOIN company_has_project ahp ON ahp.project_project_id = p.project_id\n" +
//                        "WHERE\n " +
//                        " chp.company_company_id = ?1 AND\n" +
//                        " ahp.company_company_id = ?2 AND\n" +
//                        " p.parent is NULL AND\n" +
//                        " p.deleted = 0\n" +
//                        " group by p.disipline \n",
//                Project.class)
//                .setParameter(1, companyId)
//                .setParameter(2, authorityId)
//                //.setParameter(3,null)
//                .getResultList();

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(parameters.authorityId);
        parameters.disiplineIds.clear();
        List<Project> resultList = em.createNativeQuery(createSqlString(parameters, false, "disipline"), Project.class)
                .setParameter(1, parameters.authorityId)
                .getResultList();


        List<Disipline> disiplines = new ArrayList<>();
        for (Project project : resultList) {
            if (project.getDisipline() != null) {
                disiplines.add(project.getDisipline());
            }
        }
        em.close();
        return disiplines;
    }

    @PUT
    @Path("countProjects")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer countProjects(ProjectRequestParameters parameters) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String sqlQuery = createSqlString(parameters, true, "");
        Query queryCounter = em.createNativeQuery(sqlQuery);
                //.setParameter(1, parameters.authorityId);
        Number counterUnassigned = (Number) queryCounter.getSingleResult();
        Integer integerCounter = Integer.parseInt(counterUnassigned.toString());

        em.close();
        return integerCounter;
    }

    @Deprecated
    @PUT
    @Path("loadProjects")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjects(ProjectRequestParameters parameters) {

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(parameters.authorityId);

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(parameters, false, "");
        List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                //.setParameter(1, parameters.authorityId)
                .getResultList();

        em.close();

        for (Project project : resultList) {
            optimizeProject(project, authority.isFreeAccount());

            //--------------------------------
            // Following 3 lines are added for
            // further optimization.
            // Remove if shit happens...
            //--------------------------------
            //project.setDisipline(null);
            //project.getUserList().clear();
            //project.getUserRoleList().clear();
        }
        return resultList;
        //return integerCounter;
    }

    @PUT
    @Path("loadProjectsOptimized")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsOptimized(ProjectRequestParameters parameters) {

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(parameters.authorityId);

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(parameters, false, "");
        List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                .setParameter(1, parameters.authorityId)
                .getResultList();

        em.close();

        for (Project project : resultList) {
            optimizeProject(project, authority.isFreeAccount());
            for (Project child : project.getProjectList()) {
                optimizeProject(child, authority.isFreeAccount());
                child.setDisipline(null);
                for (User user : child.getUserList()) {
                    user.setUserRoleList(new ArrayList<>());
                }
                child.setUserRoleList(new ArrayList<>());
            }
            project.setDisipline(null);
            for (User user : project.getUserList()) {
                user.setUserRoleList(new ArrayList<>());
            }
            project.setUserRoleList(new ArrayList<>());
        }
        return resultList;
    }

/*    @PUT
    @Path("loadProjectsByIds")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsByIds(List<String> projectIds) {
        String projectString = "(";
        for (String projectID : projectIds) {
            projectString += "'" + projectID + "',";
        }
        projectString = projectString.substring(0, projectString.length() - 1);
        projectString += ")";

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = "SELECT * FROM project p\n" +
                                "WHERE " +
                                " p.project_id in " + projectString +
                                " ORDER BY p.created_date DESC \n";

        List<Project> result = (List<Project>)em.createNativeQuery(queryString, Project.class)
                .getResultList();
        em.close();

        for(Project project:result) {
            optimizeProject(project,false);
            project.setDisipline(null);
            project.getUserList().clear();
            project.getUserRoleList().clear();
        }
        return result;
    } */

    @PUT
    @Path("loadProjectIds")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<String> loadProjectIds(ProjectRequestParameters parameters) {

        List<Project> resultList = loadProjects(parameters);
        List<String> projectIds = new ArrayList<>();
        for (Project project : resultList) {
            projectIds.add(project.getProjectId());
        }
        return projectIds;

        //return integerCounter;
    }


    @PUT
    @Path("loadProjectsMinimal")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsMinimal(ProjectRequestParameters parameters) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(parameters.authorityId);
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        String queryString = createSqlString(parameters, false, "");
        List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                .setParameter(1, parameters.authorityId)
                .getResultList();

        em.close();

        for (Project project : resultList) {
            optimizeProject(project, authority.isFreeAccount());

            project.getUserList().clear();
            project.getUserRoleList().clear();
            project.setDisipline(null);
        }
        return resultList;

        //return integerCounter;
    }

    @GET
    @Path("loadProjectsByCompanyOptimized/{companyid}/{authorityid}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsByCompanyOptimized(@PathParam("companyid") String companyId,
                                                        @PathParam("authorityid") String authorityId,
                                                        @PathParam("state") String state,
                                                        @PathParam("batchOffset") String batchOffset,
                                                        @PathParam("batchSize") String batchSize) {
        List<Project> projects = loadProjectsByCompany2(companyId,
                authorityId,
                state,
                batchOffset,
                batchSize);

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(authorityId);
        for (Project project : projects) {
            optimizeProject(project, authority.isFreeAccount());
        }
        return projects;
    }

    private void optimizeProject(Project project, boolean isLiteAccount) {

        List<Integer> tgs = new ArrayList<>();
        tgs.add(0);
        tgs.add(1);
        tgs.add(2);
        tgs.add(3);

        ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
        project.observationCounter = observationFacadeREST.countProjectObservationsNative(project.getProjectId(), tgs);
        if (project.getCustomer() != null) {
            project.setCustomerId(project.getCustomer().getCompanyId());
        } else {
            System.out.println("Project missing customer");
        }

        if (project.getAuthority().isFreeAccount()) {

        } else {
            if (project.getCustomer() != null) {
                project.setCustomerName(project.getCustomer().getFullName());
            }
        }
        project.authorityId = project.getAuthority().getCompanyId();
        project.authorityName = project.getAuthority().getFullName();

        if (project.getParent() != null) {
            project.parentId = project.getParent().getProjectId();
        }
        if (project.getDisipline() != null) {
            project.disiplineId = project.getDisipline().getDisiplineId();
            project.disiplineName = project.getDisipline().getName();
        }
        if (project.getAsset() != null && !isLiteAccount) {
            if (project.getImage() != null) {
                project.getImageList().add(project.getImage());
            }
        }
        //project.getImageList().add(project.getImage());
        if (project.getAsset() != null) {
            project.assetName = project.getAsset().getDefaultName();
            project.assetId = project.getAsset().getAssetId();
        }
        project.setAsset(null);
        //project.setDisipline(null);

        for (Project child : project.getProjectList()) {
            optimizeProject(child, isLiteAccount);
        }
    }

    /**
     * This method is a replacement for loadProjectsByCompany. This method has much lighter JSON compare
     * to the original method.
     *
     * @param companyId
     * @param authorityId
     * @param state
     * @param batchOffset
     * @param batchSize
     * @return
     */
    @GET
    @Path("loadProjectsByCompanyJson/{companyid}/{authorityid}/{userId}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProjectLite> loadProjectsByCompanyJson(@PathParam("companyid") String companyId,
                                                       @PathParam("authorityid") String authorityId,
                                                       @PathParam("userId") String userId,
                                                       @PathParam("state") String state,
                                                       @PathParam("batchOffset") String batchOffset,
                                                       @PathParam("batchSize") String batchSize) {
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);
        List<ProjectLite> projectJsons = new ArrayList<>();
        List<Project> resultList = loadProjects(companyId, authorityId, state, null, null, offset, size, false, false);
        ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
        for (Project project : resultList) {
            ProjectLite projectLE = new ProjectLite(project);

            List<Integer> tgs = new ArrayList<>();
            tgs.add(0);
            tgs.add(1);
            tgs.add(2);
            tgs.add(3);

            UserRoleFacadeREST userRoleFacadeREST = new UserRoleFacadeREST();
            List<UserRole> userRoles = userRoleFacadeREST.loadByProject(project.getProjectId(), userId);
            if (!userRoles.isEmpty()) {
                UserRole userRole = userRoles.get(0);
                if (userRole.getRole().getRoleType().contains("_RESTRICTED")) {
                    if (!userRole.getParameter().isEmpty()) {
                        UserRoleParameter userRoleParameter = UserRoleParameter.fromJsonString(userRole.getParameter());
                        tgs.clear();
                        tgs.addAll(userRoleParameter.tgList);
                    }
                }
            }

            projectLE.observationCounter = observationFacadeREST.countProjectObservationsNative(project.getProjectId(), tgs);
            projectJsons.add(projectLE);
        }
        return projectJsons;
    }

    @PUT
    @Path("renewObservations/{renewedProjectId}/{oldProjectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Integer renewObservations(@PathParam("renewedProjectId") String renewedProjectId, @PathParam("oldProjectId") String oldProjectId, List<String> observationIds) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        int counter = 1;
        try {
            tx.begin();
            for (String observationId : observationIds) {
                final int i = em.createNativeQuery(
                                "UPDATE observation SET project = ?, old_project = ?, observation_no = ?\n" +
                                        "WHERE (observation_id = ?);"
                        ).setParameter(1, renewedProjectId)
                        .setParameter(2, oldProjectId)
                        .setParameter(3, counter)
                        .setParameter(4, observationId)
                        .executeUpdate();
                counter++;
            }
            tx.commit();

        } catch (Exception exp) {
            tx.rollback();
            return -1;
        } finally {
            em.close();
            return counter;
        }

    }


    @GET
    @Path("loadDeletedJson/{companyId}/{authorityId}/{fromDate}/{toDate}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProjectLite> loadDeletedJson(@PathParam("companyId") String companyId,
                                             @PathParam("authorityId") String authorityId,
                                             @PathParam("fromDate") String fromDate,
                                             @PathParam("toDate") String toDate) {

        List<ProjectLite> projectJsons = new ArrayList<>();
        List<Project> resultList = loadProjects(companyId, authorityId, "99", fromDate, toDate, 0, 1000, false, true);
        ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();
        for (Project project : resultList) {
            ProjectLite projectLE = new ProjectLite(project);

            List<Integer> tgs = new ArrayList<>();
            tgs.add(0);
            tgs.add(1);
            tgs.add(2);
            tgs.add(3);

            projectLE.observationCounter = observationFacadeREST.countProjectObservationsNative(project.getProjectId(), tgs);
            projectJsons.add(projectLE);
        }
        return projectJsons;
    }

    @GET
    @Path("loadProjectsByUser/{companyId}/{authorityId}/{userId}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectListRefCompany loadProjectsByUser(@PathParam("companyId") String companyId,
                                                    @PathParam("authorityId") String authorityId,
                                                    @PathParam("userId") String userId,
                                                    @PathParam("state") String state,
                                                    @PathParam("batchOffset") String batchOffset,
                                                    @PathParam("batchSize") String batchSize) {
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        return loadProjects(companyId, authorityId, userId, state, null, true, offset, size);
    }

    @GET
    @Path("loadProjectsByUserOptimized/{companyId}/{authorityId}/{userId}/{state}/{batchOffset}/{batchSize}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsByUserOptimized(@PathParam("companyId") String companyId,
                                                     @PathParam("authorityId") String authorityId,
                                                     @PathParam("userId") String userId,
                                                     @PathParam("state") String state,
                                                     @PathParam("batchOffset") String batchOffset,
                                                     @PathParam("batchSize") String batchSize) {
        int offset = Integer.parseInt(batchOffset);
        int size = Integer.parseInt(batchSize);

        List<Project> projects = loadProjectsByUser(companyId, authorityId, userId, state, null, true, offset, size);

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(authorityId);

        for (Project project : projects) {
            optimizeProject(project, authority.isFreeAccount());
        }
        return projects;
        //return loadProjects(companyId, authorityId, userId, state, null, true, offset, size);
    }

    private List<Project> queryProjects(String companyId,
                                        String authorityId,
                                        String queryString,
                                        boolean excludeDeleted) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        int projectNumber = -1;
        try {
            projectNumber = Integer.parseInt(queryString);
        } catch (Exception e) {

        }

        queryString = "%" + queryString + "%";
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN company_has_project chp\n"
                                + "	ON chp.project_project_id = p.project_id\n"
                                + "JOIN company_has_project ahp\n"
                                + "	ON ahp.project_project_id = p.project_id\n"
                                + "LEFT JOIN asset a ON a.asset_id = p.asset\n"
                                + "WHERE " +
                                " chp.company_company_id = ?1 AND" +
                                " ahp.company_company_id = ?2 AND" +
                                " p.deleted = 0 AND " +
                                " (p.name LIKE ?3 OR p.project_number = ?4 OR a.name LIKE ?3) "
                                + "ORDER BY p.modified_date DESC \n"
                                + "LIMIT 0,6",
                        Project.class)
                .setParameter(1, companyId)
                .setParameter(2, authorityId)
                .setParameter(3, queryString)
                .setParameter(4, projectNumber)
                //.setParameter(3,null)
                .getResultList();
//        if (excludeDeleted) {
//            resultList = resultList.stream().filter(r -> r.isDeleted() == false).collect(Collectors.toList());
//        }
        em.close();
        for (Project project : resultList) {
            optimizeProject(project, false);
        }
        return resultList;
    }

    @GET
    @Path("queryProjects/{companyid}/{authorityid}/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> queryProjects(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId, @PathParam("query") String query) {
        List<Project> projects = queryProjects(companyId, authorityId, query, true);
        return projects;
    }

    @GET
    @Path("queryProjectJsons/{companyid}/{authorityid}/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProjectLite> queryProjectJsons(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId, @PathParam("query") String query) {
        List<Project> projects = queryProjects(companyId, authorityId, query, true);
        List<ProjectLite> projectLES = new ArrayList<>();
        for (Project project : projects) {
            projectLES.add(new ProjectLite(project));
        }
        return projectLES;
    }

    @GET
    @Path("loadUpdatedProjects/{companyid}/{updateDate}/{state}/")
    @Produces({MediaType.APPLICATION_JSON})
    public ProjectListRefCompany findProjectsRefCompanyDate(@PathParam("companyid") String companyId, @PathParam("updateDate") String strDate, @PathParam("state") String state) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();

        ProjectListRefCompany ref = new ProjectListRefCompany();
        DateTime jodaDateTime = new DateTime(strDate);
        List<Project> resultList = loadProjects(companyId, companyId, state, jodaDateTime.toString(), null, 0, 20, false, false);
        for (Project project : resultList) {
            if (!ref.getProjects().contains(project)) {
                ref.getProjects().add(project);
            }
            for (Company com : project.getCompanyList()) {
//                    if (!ref.getCompanies().contains(com)) {
//                        ref.getCompanies().add(com);
//                    }
            }
        }
        em.close();
        return ref;
    }

    @GET
    @Path("loadUpdatedProjectsOptimized/{companyid}/{updateDate}/{state}/")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> findProjectsRefCompanyDateOptimized(@PathParam("companyid") String companyId, @PathParam("updateDate") String strDate, @PathParam("state") String state) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        DateTime jodaDateTime = new DateTime(strDate);
        List<Project> resultList = loadProjects(companyId, companyId, state, jodaDateTime.toString(), null, 0, 20, false, false);
        em.close();

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company authority = companyFacadeREST.findNative(companyId);
        for (Project project : resultList) {
            optimizeProject(project, authority.isFreeAccount());
        }
        return resultList;
    }


    @GET
    @Path("companyprojects/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public CompanyProject findCompanyProjects(@PathParam("companyid") String companyId) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        CompanyProject companyProject = new CompanyProject();
        Company company = companyFacadeREST.find(companyId);
        List<Project> projects = company.getProjectList();

        for (Project project : projects) {
            if (project.getParent() == null) {
                companyProject.getProjectlist().add(project);
            }
            if (!companyProject.getAssetList().contains(project.getAsset())) {
                companyProject.getAssetList().add(project.getAsset());
            }
        }
        return companyProject;
    }


    private void updateUsersForProject(Project project, List<String> userIds) {
        // Remove existing users
        for (User user : project.getUserList()) {
            unlinkToUser(user.getUserId(), project);
            //project.getUserList().remove(user);
        }

        // Add new users to parent project
        for (String userId : userIds) {
            linkToUser(userId, project);
        }
        // Fix all children
        for (Project child : project.getProjectList()) {
            // Remove users from all childs
            for (User user : child.getUserList()) {
                unlinkToUser(user.getUserId(), child);
                //child.getUserList().remove(user);
            }

            // Add users to all childs
            for (String userId : userIds) {
                linkToUser(userId, child);
            }
        }
    }

    @PUT
    @Path("updateUsers/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public void updateUsers(@PathParam("projectId") String projectId, List<String> userIds) {
        Project project = findNative(projectId);
        if (project.getParent() != null) {
            updateUsersForProject(project.getParent(), userIds);
        } else {
            updateUsersForProject(project, userIds);
        }
    }

    private void updateCompaniesForProject(Project project, List<String> companyIds) {
        // Remove existing companies
        for (Company company : project.getCompanyList()) {
            if (!company.equals(project.getCustomer()) && !company.equals(project.getAuthority())) {
                unlinkCompany(company.getCompanyId(), project);
            }
        }

        // Add new company to parent project
        for (String companyId : companyIds) {
            linkToCompany(companyId, project);
        }

        // Fix all children
        for (Project child : project.getProjectList()) {
            // Remove users from all childs
            for (Company company : child.getCompanyList()) {
                unlinkCompany(company.getCompanyId(), child);
            }

            // Add users to all childs
            for (String companyId : companyIds) {
                linkToCompany(companyId, child);
            }
        }
    }

    @PUT
    @Path("updateCompanies/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public void updateCompanies(@PathParam("projectId") String projectId, List<String> companyIds) {
        Project project = findNative(projectId);
        if (project.getParent() != null) {
            updateCompaniesForProject(project.getParent(), companyIds);
        } else {
            updateCompaniesForProject(project, companyIds);
        }
    }


    @PUT
    @Path("findMissing/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> findMissing(List<String> projectIds) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<String> missingIds = new ArrayList<>();
        for (String projectId : projectIds) {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM project p " +
                            " WHERE p.project_id = ?1")
                    .setParameter(1, projectId);

            Number counter = (Number) query.getSingleResult();
            int intCounter = Integer.parseInt(counter.toString());

            if (intCounter == 0) {
                missingIds.add(projectId);
            }
        }
        em.close();

        return missingIds;
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadByIntegration(@PathParam("service") String service, @PathParam("primaryKey") String primaryKey, @PathParam("authorityId") String authorityId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                + "* FROM project p\n"
                                + "JOIN project_has_integration phi on phi.project_project_id = p.project_id \n"
                                + "JOIN integration i on i.integration_id = phi.integration_integration_id \n"
                                + "JOIN company_has_project chp on chp.project_project_id = p.project_id \n"
                                + "WHERE i.keyy = 'PRIMARY_KEY' AND  i.service = ?1 AND i.valuee = ?2 AND chp.company_company_id = ?3",
                        Project.class)
                .setParameter(1, service)
                .setParameter(2, primaryKey)
                .setParameter(3, authorityId)
                .getResultList();
        em.close();
        return resultList;
    }

    @PUT
    @Path("loadByIds/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> loadByIds(List<String> projectIds) {

        if (!projectIds.isEmpty()) {
            String projectIdsString = "(";
            for (String projectId : projectIds) {
                projectIdsString += "'" + projectId + "',";
            }
            projectIdsString = projectIdsString.substring(0, projectIdsString.length() - 1);
            projectIdsString += ")";

            EntityManager em = LocalEntityManagerFactory.createEntityManager();
            List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                    + "* FROM project p\n"
                                    + "where p.project_id in " + projectIdsString + "\n",
                            Project.class)
                    .getResultList();
            em.close();
            for (Project project : resultList) {
                optimizeProject(project, false);
                project.setDisipline(null);
                project.getUserList().clear();
                project.getUserRoleList().clear();
            }
            return resultList;
        } else {
            return new ArrayList<>();
        }

    }

}
