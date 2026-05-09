/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.softwarecontrol.idoc.data.entityhelper.*;
import no.softwarecontrol.idoc.data.entityjson.ProjectLite;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.data.entityobject.query.ResultItem;
import no.softwarecontrol.idoc.data.requestparams.ObservationRequestParameters;
import no.softwarecontrol.idoc.data.requestparams.ProjectRequestParameters;
import no.softwarecontrol.idoc.data.requestparams.RequestParameterEnums;
import no.softwarecontrol.idoc.statistics.StatisticsFactory;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;
//import no.softwarecontrol.idoc.webservices.zoho.ZohoClient;
import org.joda.time.DateTime;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

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

    private static ProjectFacadeREST instance;


    public ProjectFacadeREST() {
        super(Project.class);
        instance = this;

    }

    public static ProjectFacadeREST getInstance() {
        if (instance == null) {
            instance = new ProjectFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return "Project.findAll";
    }

//    @OPTIONS
//    @Path("{path : .*}") // Match alle stier
//    public Response options() {
//        return Response.ok()
//                .header("Access-Control-Allow-Origin", "http://localhost:8181")
//                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
//                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
//                .build();
//    }

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

        Company customer = CompanyFacadeREST.getInstance().find(customerId);

        List<String> assetIds = params.get(0);
        List<String> userIds = params.get(1);
        Project project = createMemoryProject(authorityId, disiplineId, assetIds);
        project.setName(customer.getFullName());
        project.setProjectState(0);
        project.setGrouped(Boolean.TRUE);
        project.setCreatedCompany(authorityId);
        project.authorityId = authorityId;
        createProject(project);

        //ProjectNumber projectNumber = companyFacadeREST.incrementProjectCounter(authorityId);
        //project.setProjectNumber(projectNumber.getProjectCounter());

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


        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);

        ProjectNumber projectNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(authorityId);

        project.setProjectNumber(projectNumber.getProjectCounter());
        project.setState(Project.State.INIT);
        project.setDeleted(false);
        project.setDisipline(disipline);
        project.setCreatedCompany(authorityId);
        project.setCreatedDate(new Date());
        project.setModifiedDate(new Date());
        if (assetIds.size() == 1) {
            String assetId = assetIds.get(0);
            Asset asset = AssetFacadeREST.getInstance().find(assetId);
            project.setGrouped(false);
            project.setName(asset.getDefaultName());
            project.setAsset(asset);
        } else {
            project.setGrouped(true);
            for (String assetId : assetIds) {
                Asset asset = AssetFacadeREST.getInstance().find(assetId);
                Project child = new Project();
                ProjectNumber childNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(authorityId);
                child.setState(Project.State.INIT);
                child.setDeleted(false);
                child.setAsset(asset);
                child.setName(asset.getDefaultName());
                child.setProjectNumber(childNumber.getProjectCounter());
                child.setProjectId(UUID.randomUUID().toString());
                child.setDisipline(disipline);
                child.setGrouped(false);
                child.setCreatedCompany(authorityId);
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
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
    @Deprecated
    @POST
    @Path("createWithParameters")
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer createWithParameters(ProjectParameters entity) {

        // Check if project exists
        Project project = entity.getProject();
        Project existingProject = findNative(project.getProjectId());
        Asset asset = AssetFacadeREST.getInstance().find(entity.getAssetId());
        Disipline disipline = DisiplineFacadeREST.getInstance().find(entity.getDisiplineId());
        List<String> userIds = entity.getUserIdList();
        // Verify that all users are stored in the database
        List<User> existingUsers = new ArrayList<>();
        List<Company> externalCompanies = new ArrayList<>();

        for (User incomingUser : project.getUserList()) {
            User existingUser = UserFacadeREST.getInstance().find(incomingUser.getUserId());
            if (existingUser == null) {
                UserFacadeREST.getInstance().create(incomingUser);
            } else {
                if(!existingUser.getCompanyList().isEmpty()) {
                    Company external = existingUser.getCompanyList().get(0);
                    if(!external.getCompanyId().equalsIgnoreCase(entity.getAuthorityId()) && !external.getCompanyId().equalsIgnoreCase(entity.getCustomerId())) {
                        externalCompanies.add(external);
                    }
                }
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
            ProjectNumber projectNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.getAuthorityId());
            project.setProjectNumber(projectNumber.getProjectCounter());

            List<Project> children = new ArrayList<>(project.getProjectList());
            project.getProjectList().clear();

            System.out.println("Setting user roles for project " + project.getProjectId());
            System.out.println("============================================");
            for (UserRole userRole : project.getUserRoleList()) {
                System.out.println("Setting user role: " + userRole.getUserRoleId());
                userRole.setProject(project);
            }

            super.create(project);

            List<Project> existingChildren = new ArrayList<>();
            List<Project> missingChildren = new ArrayList<>();
            for (Project child : children) {
                child.setParent(project);
                if (findNative(child.getProjectId()) != null) {
                    existingChildren.add(child);
                    editProjectOnly(child.getProjectId(), child);
                } else {
                    missingChildren.add(child);
                }
            }

            for (Project child : missingChildren) {
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
                child.setParent(project);
                ProjectNumber childNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.getAuthorityId());
                child.setProjectNumber(childNumber.getProjectCounter());
                if (child.getDisipline() == null) {
                    child.setDisipline(project.getDisipline());
                }
                if (child.getAsset() != null) { // Verbose payload where asset is included. OLD
                    Asset existingAsset = AssetFacadeREST.getInstance().find(child.getAsset().getAssetId());
                    if (existingAsset != null) {
                        child.setAsset(existingAsset);
                    }
                } else { // Light payload where asset is removed. NEW & IMPROVED
                    Asset existingAsset = AssetFacadeREST.getInstance().find(child.assetId);
                    if (existingAsset != null) {
                        child.setAsset(existingAsset);
                    }
                }
                super.create(child);
            }

            for (Project child : project.getProjectList()) {
                child.getUserList().clear();
            }

//            System.out.println("============================================");
//            for (UserRole userRole : project.getUserRoleList()) {
//                System.out.println("Setting user role: " + userRole.getUserRoleId());
//                userRole.setProject(project);
//            }

//            List<User> users = new ArrayList<>(project.getUserList());
//            project.getUserList().clear();
//            project.getUserList().addAll(users);

            // Link companies and users
            for (User user : project.getUserList()) {
                linkToUser(user.getUserId(), project);
            }
            linkToCompany(entity.getAuthorityId(), project);
            linkToCompany(entity.getCustomerId(), project);
            for(Company external : externalCompanies) {
                linkToCompany(external.getCompanyId(), project);
            }
            for (Project child : missingChildren) {
                linkToCompany(entity.getAuthorityId(), child);
                linkToCompany(entity.getCustomerId(), child);
                for(Company external : externalCompanies) {
                    linkToCompany(external.getCompanyId(), child);
                }
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
        return project.getProjectNumber();
    }

    @POST
    @Path("create")
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer createProject(Project entity) {
        Integer projectNum = 0;
        Project existingProject = findNative(entity.getProjectId());
        if(existingProject == null) {
            // Check if some of the children exists, due to merging of project on client or something
            List<Project> existingChildren = new ArrayList<>();
            for (Project child : entity.getProjectList()) {
                Project existingChild = findNative(child.getProjectId());
                if (existingChild != null) {
                    existingChildren.add(existingChild);
                }
            }

            // Remove exisiting children from creating entity
            for (Project existingChild : existingChildren) {
                List<Project> filteredChildren = entity.getProjectList().stream().filter(r -> r.getProjectId().equalsIgnoreCase(existingChild.getProjectId())).collect(Collectors.toList());
                for (Project filteredChild : filteredChildren) {
                    entity.getProjectList().remove(filteredChild);
                }
            }

            ProjectNumber projectNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.authorityId);
            entity.setProjectNumber(projectNumber.getProjectCounter());
            projectNum = projectNumber.getProjectCounter();

            Disipline disipline = DisiplineFacadeREST.getInstance().find(entity.disiplineId);

            if (disipline != null) {
                entity.setDisipline(disipline);
            }
            if (entity.assetId != null) {
                Asset asset = AssetFacadeREST.getInstance().find(entity.assetId);
                if (asset != null) {
                    entity.setAsset(asset);
                }
            }

            entity.setCreatedDate(new Date());
            entity.setModifiedDate(new Date());
            for (Project child : entity.getProjectList()) {
                child.setParent(entity);
                child.parentId = entity.getProjectId();
                ProjectNumber pn = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.authorityId);
                child.setProjectNumber(pn.getProjectCounter());
                child.setCreatedDate(new Date());
                child.setModifiedDate(new Date());
                Disipline childDisipline = DisiplineFacadeREST.getInstance().find(child.disiplineId);
                if (childDisipline != null) {
                    child.setDisipline(childDisipline);
                }
                if (child.assetId != null) {
                    Asset asset = AssetFacadeREST.getInstance().find(child.assetId);
                    if (asset != null) {
                        child.setAsset(asset);
                    }
                }
            }

            // It might happen that a user is only existing on local client
            // Insert it to avoid error on project create
            for(User user : entity.getUserList()) {
                User exitingUser = UserFacadeREST.getInstance().find(user.getUserId());
                if (exitingUser == null) {
                    UserFacadeREST.getInstance().create(user);
                }
            }
            if(entity.parentId != null) {
                if(!entity.parentId.isEmpty()) {
                    Project parent = ProjectFacadeREST.getInstance().find(entity.parentId);
                    if(parent != null) {
                        entity.setParent(parent);
                    }
                }
            }
            super.create(entity);

            for (Project existingChild : existingChildren) {
                existingChild.setParent(entity);
                edit(existingChild);
            }
            // Link project to companies & users
            linkCompanySimple(entity.authorityId, entity.getProjectId());
            linkCompanySimple(entity.getCustomerId(), entity.getProjectId());
            for (User user : entity.getUserList()) {
                linkToUser(user.getUserId(), entity);
            }
            for (Project child : entity.getProjectList()) {
                linkCompanySimple(child.getCreatedCompany(), child.getProjectId());
                linkCompanySimple(child.getCustomerId(), child.getProjectId());
                for (User user : entity.getUserList()) {
                    linkToUser(user.getUserId(), child);
                }
            }
            System.out.println("Project inserted... " + entity.getProjectId());
            return projectNum;
        } else {
            return 0;
        }
    }

    private int prepareCreateProject(Project project) {

        ProjectNumber projectNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(project.authorityId);
        project.setProjectNumber(projectNumber.getProjectCounter());
        if (!project.assetId.isEmpty()) {
            Asset asset = AssetFacadeREST.getInstance().find(project.assetId);
            project.setAsset(asset);
        }
        if (!project.disiplineId.isEmpty()) {
            Disipline disipline = DisiplineFacadeREST.getInstance().find(project.disiplineId);
            project.setDisipline(disipline);
        }
        // Verify that all users are stored in the database
        List<User> existingUsers = new ArrayList<>();
        for (User incomingUser : project.getUserList()) {
            User existingUser = UserFacadeREST.getInstance().find(incomingUser.getUserId());
            if (existingUser == null) {
                UserFacadeREST.getInstance().create(incomingUser);
            }
        }
        for (UserRole userRole : project.getUserRoleList()) {
            userRole.setProject(project);
        }


        return 1;
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

        if (project.getAsset() != null) {
            AssetFacadeREST.getInstance().replaceCustomer(oldCustomerId, newCustomerId, project.getAsset());
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
                Equipment equipment = EquipmentFacadeREST.getInstance().find(observation.getEquipmentId());
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

        Asset asset = AssetFacadeREST.getInstance().find(assetId);

        entity.setAsset(asset);
        asset.getProjectList().add(entity);

        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);
        authority.getProjectList().add(entity);
        entity.getCompanyList().add(authority);

        Company customer = CompanyFacadeREST.getInstance().findNative(customerId);
        customer.getProjectList().add(entity);
        entity.getCompanyList().add(customer);

        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
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
        DisiplineFacadeREST.getInstance().edit(disipline);
        AssetFacadeREST.getInstance().edit(asset);
        CompanyFacadeREST.getInstance().edit(authority);
        CompanyFacadeREST.getInstance().edit(customer);
        DisiplineFacadeREST.getInstance().edit(disipline);

        super.edit(entity);
    }

    @POST
    @Path("createWithCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, Project entity) {

        Company company = CompanyFacadeREST.getInstance().findNative(companyId);
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
                CompanyFacadeREST.getInstance().edit(company);
            }
        }
    }


    private void linkChildrenToAsset(Project child, Project entity) {

        child.setParent(entity);
        if (child.getAsset() != null) {
            Asset existingAsset = AssetFacadeREST.getInstance().find(child.getAsset().getAssetId());
            if (existingAsset != null) {
                child.setAsset(existingAsset);
                if (!existingAsset.getProjectList().contains(child)) {
                    existingAsset.getProjectList().add(child);
                }
            }
            AssetFacadeREST.getInstance().edit(existingAsset);
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

        List<Project> projects;
        try (EntityManager em2 = LocalEntityManagerFactory.createEntityManager()) {
            projects = (List<Project>) em2.createNativeQuery("SELECT "
                                    + "p.* FROM project p\n"
                                    + "WHERE p.start_date is null LIMIT 0,10000",
                            Project.class)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception while loading projects without start date: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load projects for start date update", e);
        }

        for (Project project : projects) {
            if (project.getStartDate() == null && project.getCreatedDate() != null) {
                try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
                        if (tx.isActive()) {
                            tx.rollback();
                        }
                        System.out.println("Exception while updating start date for project " + project.getProjectId() + ": " + exp.getMessage());
                        exp.printStackTrace(System.err);
                        // Continue with next project instead of failing completely
                    }
                } catch (Exception e) {
                    System.out.println("Exception while creating EntityManager for project " + project.getProjectId() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    // Continue with next project instead of failing completely
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
                ProjectNumber pn = CompanyFacadeREST.getInstance().incrementProjectCounter(project.getCreatedCompany());
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
    @Path("editModifiedDate/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editModifiedDate(@PathParam("id") String id, Project entity) {
        Project project = this.findNative(id);
        project.setModifiedDate(entity.getModifiedDate());
        this.edit(project);
    }

    @PUT
    @Path("editProjectOnly/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void editProjectOnly(@PathParam("id") String id, Project entity) {
        Project project = this.findNative(entity.getProjectId());
        if (project != null) {

            project.setDeleted(entity.isDeleted());
            if (entity.getCreatedCompany() != null) {
                project.setCreatedCompany(entity.getCreatedCompany());
            }

            project.setModifiedDate(new Date());
            project.setModifiedUser(entity.getModifiedUser());
            project.setName(entity.getName());
            project.setOrderNo(entity.getOrderNo());
            project.setCustomerName(entity.getCustomerName());
            project.setCustomerContactPerson(entity.getCustomerContactPerson());
            project.setCustomerContactEmail(entity.getCustomerContactEmail());
            project.setCustomerRef(entity.getCustomerRef());
            project.setFreeText(entity.getFreeText());
            project.setProjectState(entity.getProjectState());
            project.setLanguageCode(entity.getLanguageCode());
            if (entity.getScheduledStartDate() != null) {
                project.setScheduledStartDate(entity.getScheduledStartDate());
            }
            if (entity.getScheduledEndDate() != null) {
                project.setScheduledEndDate(entity.getScheduledEndDate());
            }
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

            // check if parent is present
            if (entity.parentId != null) {
                Project parent = find(entity.parentId);
                if (parent != null) {
                    project.setParent(parent);
                }
            }

            if (entity.disiplineId != null) {
                Disipline disipline = DisiplineFacadeREST.getInstance().find(entity.disiplineId);
                if (disipline != null) {
                    project.setDisipline(disipline);
                }
            }
            // Check if disipline has been changed
            if (entity.getDisipline() != null) {
                Disipline disipline = DisiplineFacadeREST.getInstance().find(entity.getDisipline().getDisiplineId());
                if (disipline != null) {
                    project.setDisipline(disipline);
                }
            } else {

            }

            // Check if images have been added
            for (Media image : entity.getImageList()) {
                if (!project.getImageList().contains(image)) {
                    project.getImageList().add(image);
                }
            }

//            if(!entity.getImageList().isEmpty()) {
//                project.setImageList(entity.getImageList());
//            }

            // Check if reports have been added
            if (!entity.getReportList().isEmpty()) {
                project.setReportList(entity.getReportList());
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

            List<Company> companies = new ArrayList<>();
            String createdUserId = null;
            for (User user : entity.getUserList()) {
                List<Company> userCompanies = UserFacadeREST.getInstance().findUserCompanies(user.getUserId());
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

            ProjectNumber pNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.getCreatedCompany());
            entity.setProjectNumber(pNumber.getProjectCounter());
            entity.setCreatedDate(new Date());
            entity.setModifiedDate(new Date());
            for (Project child : entity.getProjectList()) {
                child.setParent(entity);
                ProjectNumber cNumber = CompanyFacadeREST.getInstance().incrementProjectCounter(entity.getCreatedCompany());
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
            System.out.println("Trying to save a project that does not exist: " + entity.getProjectId() + ": " + entity.getName());
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

    private void optimizeChildren(Project project) {
        for (Project child : project.getProjectList()) {
            child.setDisipline(null);
            //child.setAsset(null);
            child.setUserRoleList(new ArrayList<>());
            child.setUserList(new ArrayList<>());
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Project find(@PathParam("id") String id) {
        Project project = findNative(id);
        if (project != null) {
            optimizeChildren(project);
        }
        return project;
    }


    @GET
    @Path("findOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Project findOptimized(@PathParam("id") String id) {
        Project project = findNative(id);
        if (project != null) {
            boolean isLiteAccount = false;
            if (project.getAuthority() != null) {
                if (project.getAuthority().getIsLiteAccount()) {
                    isLiteAccount = true;
                }
            }
            optimizeProject(project, isLiteAccount);

            project.setAsset(null);
            project.setObservationList(new ArrayList<>());
            String authorityId = project.authorityId;
            if(authorityId == null || authorityId.isBlank()) {
                if(project.getAuthority() != null) {
                    authorityId = project.getAuthority().getCompanyId();
                }
            }

            List<String> projectIds = new ArrayList<>();
            projectIds.add(project.getProjectId());
            ObservationRequestParameters observationRequestParameters = new ObservationRequestParameters();
            observationRequestParameters.authorityId = authorityId;
            observationRequestParameters.fromDate = null;
            observationRequestParameters.toDate = null;
            observationRequestParameters.showOpenOnly = true;
            observationRequestParameters.disiplineIds = new ArrayList<>();
            observationRequestParameters.tgs.add(1);
            observationRequestParameters.tgs.add(2);
            observationRequestParameters.tgs.add(3);
            observationRequestParameters.parentEntity = "project";
            observationRequestParameters.entityIds = projectIds;
            List<DeviationCounter> deviationCounters = ObservationFacadeREST.getInstance().countProjectDeviations(observationRequestParameters);

            if(!deviationCounters.isEmpty()) {
                project.deviationCounter = deviationCounters.get(0);
            }
            //project.getDisipline().setEquipmentTypeList(new ArrayList<>());
            optimizeChildren(project);

            return project;

        }
        return null;
    }

    @Deprecated
    @GET
    @Path("findOptimized2/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Project findOptimized2(@PathParam("id") String id) {
        Project project = findNative(id);
        optimizeProject(project, false);
        project.setAsset(null);
        project.setObservationList(new ArrayList<>());
        return project;
    }


    public Project findNative(String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Project> resultList = (List<Project>) em.createNativeQuery("SELECT "
                                    + "* FROM project p\n"
                                    + "WHERE p.project_id = ?1",
                            Project.class)
                    .setParameter(1, id)
                    .getResultList();
            if (resultList.isEmpty()) {
                return null;
            } else {
                return resultList.get(0);
            }
        } catch (Exception e) {
            System.out.println("Exception while finding project by id: " + id + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new InternalServerErrorException("Failed to find project with id: " + id, e);
        }
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

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Project> findAll() {
//        return super.findAll();
//    }
//
//    @GET
//    @Path("{from}/{to}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Project> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
//        return super.findRange(new int[]{from, to});
//    }

    @GET
    @Path("queryAllByAuthority/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<ResultItem> queryAllByAuthority(@PathParam("companyid") String id) {
        List<ResultItem> resultItems = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT p.project_id, p.name FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company c
                                    on chp.company_company_id = c.company_id
                                WHERE chp.company_company_id = ?1
                                """,
                            Project.class)
                    .setParameter(1, id)
                    .getResultList();
            for (Project project : resultList) {
                ResultItem item = new ResultItem();
                item.setName(project.getName());
                item.setId(project.getProjectId());
                resultItems.add(item);
            }
            return resultItems;
        } catch (Exception e) {
            System.out.println("Exception while querying all projects by authority " + id + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to query projects by authority", e);
        }
    }

    @GET
    @Path("queryByAuthority/{companyid}/{querystring}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ResultItem> queryByAuthority(@PathParam("companyid") String id, @PathParam("querystring") String queryString) {
        List<ResultItem> resultItems = new ArrayList<>();
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            queryString = "%" + queryString + "%";

            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT p.project_id, p.name FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company c
                                    on chp.company_company_id = c.company_id
                                WHERE chp.company_company_id = ?1 AND p.name LIKE ?2
                                """,
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
            return resultItems;
        } catch (Exception e) {
            System.out.println("Exception while querying projects by authority " + id + " with query '" + queryString + "': " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to query projects by authority with search string", e);
        }
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
        Project project = this.findNative(entity.getProjectId());
        Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
        if (disipline != null && project != null) {
            project.setDisipline(disipline);
            this.edit(project);
        }
    }

    @GET
    @Path("linkUserSimple/{userId}/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkUserSimple(@PathParam("userId") String userId, @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM project_has_user
                            WHERE project_project_id = ?1 AND user_user_id = ?2
                            """)
                        .setParameter(1, projectId)
                        .setParameter(2, userId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                INSERT INTO project_has_user (project_project_id, user_user_id)
                                VALUES (?, ?);
                                """)
                            .setParameter(1, projectId)
                            .setParameter(2, userId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while linking user " + userId + " to project " + projectId + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                throw new RuntimeException("Failed to link user to project", exp);
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for linkUserSimple: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @GET
    @Path("unlinkUserSimple/{userId}/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})

    public void unlinkUserSimple(@PathParam("userId") String userId, @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Query query = em.createNativeQuery("""
                            DELETE FROM project_has_user
                            WHERE project_project_id = ?1 AND user_user_id = ?2
                            """)
                        .setParameter(1, projectId)
                        .setParameter(2, userId);

                Number counter = (Number) query.executeUpdate();
                if (counter.intValue() == 1) {
                    //System.out.println("DELETED project_has_user SUCCEEDED");
                }
                tx.commit();
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while unlinking user " + userId + " from project " + projectId + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                throw new RuntimeException("Failed to unlink user from project", exp);
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for unlinkUserSimple: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @PUT
    @Path("linkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToUser(@PathParam("userId") String userId, Project entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM project_has_user
                            WHERE project_project_id = ?1 AND user_user_id = ?2
                            """)
                        .setParameter(1, entity.getProjectId())
                        .setParameter(2, userId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                INSERT INTO project_has_user (project_project_id, user_user_id)
                                VALUES (?, ?);
                                """)
                            .setParameter(1, entity.getProjectId())
                            .setParameter(2, userId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while linking user " + userId + " to project " + entity.getProjectId() + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være opprettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for linkToUser: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }


    @PUT
    @Path("unlinkUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkToUser(@PathParam("userId") String userId, Project entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Query query = em.createNativeQuery("""
                            DELETE FROM project_has_user
                            WHERE project_project_id = ?1 AND user_user_id = ?2
                            """)
                        .setParameter(1, entity.getProjectId())
                        .setParameter(2, userId);

                Number counter = (Number) query.executeUpdate();
                if (counter.intValue() == 1) {
                    //System.out.println("DELETED project_has_user SUCCEEDED");
                }
                tx.commit();
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while unlinking user " + userId + " from project " + entity.getProjectId() + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være slettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for unlinkToUser: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @GET
    @Path("linkCompanySimple/{companyId}/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkCompanySimple(@PathParam("companyId") String companyId, @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM company_has_project
                            WHERE company_company_id = ?1 AND project_project_id = ?2
                            """)
                        .setParameter(1, companyId)
                        .setParameter(2, projectId);

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                INSERT INTO company_has_project (company_company_id, project_project_id)
                                VALUES (?, ?);
                                """)
                            .setParameter(1, companyId)
                            .setParameter(2, projectId)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while linking company " + companyId + " to project " + projectId + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være opprettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for linkCompanySimple: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @GET
    @Path("unlinkCompanySimple/{companyId}/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})

    public void unlinkCompanySimple(@PathParam("companyId") String companyId, @PathParam("projectId") String projectId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Query query = em.createNativeQuery("""
                            DELETE FROM company_has_project
                            WHERE company_company_id = ?1 AND project_project_id = ?2
                            """)
                        .setParameter(1, companyId)
                        .setParameter(2, projectId);

                Number counter = (Number) query.executeUpdate();
                if (counter.intValue() == 1) {
                    System.out.println("DELETED company_has_project SUCCEEDED");
                }
                tx.commit();
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while unlinking company " + companyId + " from project " + projectId + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være slettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for unlinkCompanySimple: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @Deprecated
    @PUT
    @Path("linkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})

    public void linkToCompany(@PathParam("companyId") String companyId, Project entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                Query query = em.createNativeQuery("""
                            SELECT COUNT(*) FROM company_has_project
                            WHERE company_company_id = ?1 AND project_project_id = ?2
                            """)
                        .setParameter(1, companyId)
                        .setParameter(2, entity.getProjectId());

                Number counter = (Number) query.getSingleResult();
                if (counter.intValue() == 0) {
                    tx.begin();
                    final int i = em.createNativeQuery("""
                                INSERT INTO company_has_project (company_company_id, project_project_id)
                                VALUES (?, ?);
                                """)
                            .setParameter(1, companyId)
                            .setParameter(2, entity.getProjectId())
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while linking company " + companyId + " to project " + entity.getProjectId() + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være opprettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for linkToCompany: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})

    public void unlinkCompany(@PathParam("companyId") String companyId, Project entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Query query = em.createNativeQuery("""
                            DELETE FROM company_has_project
                            WHERE company_company_id = ?1 AND project_project_id = ?2
                            """)
                        .setParameter(1, companyId)
                        .setParameter(2, entity.getProjectId());

                Number counter = (Number) query.executeUpdate();
                if (counter.intValue() == 1) {
                    System.out.println("DELETED company_has_project SUCCEEDED");
                }
                tx.commit();
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while unlinking company " + companyId + " from project " + entity.getProjectId() + ": " + exp.getMessage());
                exp.printStackTrace(System.err);
                // Ikke kast exception - aksepter at recorden kan være slettet allerede
            }
        } catch (Exception e) {
            System.out.println("Exception while creating EntityManager for unlinkCompany: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to initialize EntityManager", e);
        }
    }

    @PUT
    @Path("linkAsset/{assetid}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToAsset(@PathParam("assetid") String assetId, Project entity) {
        Project project = this.find(entity.getProjectId());
        Asset asset = AssetFacadeREST.getInstance().find(assetId);
        if (project != null && asset != null) {
            project.setAsset(asset);
            if (!asset.getProjectList().contains(project)) {
                asset.getProjectList().add(project);
                AssetFacadeREST.getInstance().edit(asset);
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



    private List<WalletProject> loadWalletProjects(String fromDate,
                                                   String toDate,
                                                   int batchOffset,
                                                   int batchSize) {
        DateTime jodaFromTime = new DateTime(fromDate);
        DateTime jodaToTime = new DateTime(toDate);

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sqlString = """
                SELECT
                   wallet_project.project_id as project_id,
                   wallet_project.created_date as created_date,
                   d.name as disipline_name,
                   auth.name as authority_name,
                   count(wallet_project.project_id) as point_count,
                   d.point_price as point_factor,
                   d.max_children as max_children,
                   (SELECT point_price
                       FROM invoice
                       WHERE company = wallet_project.created_company order by start_date DESC
                       LIMIT 0,1) as point_price,
                   (SELECT point_discount
                       FROM invoice
                       WHERE company = wallet_project.created_company order by start_date DESC
                       LIMIT 0,1) as point_discount
                FROM project as wallet_project
                   join disipline d on wallet_project.disipline = d.disipline_id
                   left join company auth on wallet_project.created_company = auth.company_id
                   left join project child ON child.parent = wallet_project.project_id
                WHERE
                   wallet_project.created_date > ?1 and
                   wallet_project.created_date < ?2 and
                   wallet_project.created_company != "E07121A7-024A-4D0E-8B58-A064F0BC4A22" and
                   (wallet_project.deleted = 0 or wallet_project.deleted is null) AND
                   (child.deleted = 0 or child.deleted is null) and
                   wallet_project.parent is null
                group by wallet_project.project_id
                order by auth.name LIMIT ?3, ?4
                """;

            List<WalletProject> resultList = (List<WalletProject>) em.createNativeQuery(sqlString, WalletProject.class)
                    .setParameter(1, jodaFromTime.toString())
                    .setParameter(2, jodaToTime.toString())
                    .setParameter(3, batchOffset)
                    .setParameter(4, batchSize)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading wallet projects: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load wallet projects", e);
        }
    }

    @GET
    @Path("loadProductionStatisticsForPeriod/{authorityId}/{fromDate}/{toDate}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProductionStatistic> loadProductionStatisticsForPeriod(@PathParam("authorityId") String authorityId,
                                                                       @PathParam("fromDate") String fromDateString,
                                                                       @PathParam("toDate") String toDateString) {

        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);
        DateTime fromDate = new DateTime(fromDateString);
        DateTime toDate = new DateTime(toDateString);

        List<ProductionStatistic> productionStatistics = new ArrayList<>();

        if (authority.getCompanyType().equalsIgnoreCase("SOFTWARE_CONTROL")) {
            List<WalletProject> walletProjects = loadWalletProjects(fromDate.toString(), toDate.toString(), 0, 9999);
            productionStatistics.addAll(StatisticsFactory.createMonthly(walletProjects));
        }
        if (!productionStatistics.isEmpty()) {
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
            int counterTG0 = ObservationFacadeREST.getInstance().countObservationsInPeriod(fromDate.toString(), toDate.toString(), 0);
            int counterTG1 = ObservationFacadeREST.getInstance().countObservationsInPeriod(fromDate.toString(), toDate.toString(), 1);
            int counterTG2 = ObservationFacadeREST.getInstance().countObservationsInPeriod(fromDate.toString(), toDate.toString(), 2);
            int counterTG3 = ObservationFacadeREST.getInstance().countObservationsInPeriod(fromDate.toString(), toDate.toString(), 3);

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
        intPeriodCounter = 1;

        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);
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
//            int counterTG0 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 0);
//            int counterTG1 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 1);
//            int counterTG2 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 2);
//            int counterTG3 = observationFacadeREST.countObservationsInPeriod(firstDayOfMonth.toString(), toDate.toString(), 3);
//
//            productionStatistic.setTg0Count(counterTG0);
//            productionStatistic.setTg1Count(counterTG1);
//            productionStatistic.setTg2Count(counterTG2);
//            productionStatistic.setTg3Count(counterTG3);

            productionStatistic.setTg0Count(0);
            productionStatistic.setTg1Count(0);
            productionStatistic.setTg2Count(0);
            productionStatistic.setTg3Count(0);
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

        //projectRequestParameters.states.add(0);
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
        projectRequestParameters.dateField = ProjectRequestParameters.DateField.CREATED_DATE;

        List<Project> filteredList = loadInvoiceProjects(projectRequestParameters);
        List<DisiplineGroup> disiplineGroups = DisiplineGroup.createDisiplineGroupList(filteredList);

        return disiplineGroups;
    }

    @PUT
    @Path("loadInvoiceProjects")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})

    public List<Project> loadInvoiceProjects(ProjectRequestParameters projectRequestParameters) {

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String queryString = createSqlString(projectRequestParameters, false, "");
            List<Project> resultList = em.createNativeQuery(queryString,
                            Project.class)
                    .setParameter(1, projectRequestParameters.authorityId)
                    .getResultList();

            //List<Project> resultList = loadProjectsOptimized(projectRequestParameters);
            List<Project> filteredList = new ArrayList<>();

            List<Company> companies = em.createNativeQuery(
                            "SELECT c.* " +
                                    "FROM company c " +
                                    "WHERE c.company_id = ?1",
                            Company.class)
                    .setParameter(1, projectRequestParameters.authorityId)
                    .getResultList();

            if (!companies.isEmpty()) {
                for (Project project : resultList) {
                    optimizeProject(project, companies.get(0).getIsLiteAccount());
                    project.setUserRoleList(new ArrayList<>());
                    project.setUserList(new ArrayList<>());
                    project.setIntegrationList(new ArrayList<>());
                    //project.setDisipline(null);
                    if (project.getCustomer() != null) {
                        if (!project.getCustomer().isDemo()) {
                            filteredList.add(project);
                        }
                    }
                    for(Project child: project.getProjectList()) {
                        child.setUserRoleList(new ArrayList<>());
                        child.setUserList(new ArrayList<>());
                        child.setIntegrationList(new ArrayList<>());
                        child.setDisipline(null);
                    }
                }
            }

            //List<DisiplineGroup> disiplineGroups = DisiplineGroup.createDisiplineGroupList(filteredList);

            return filteredList;
        } catch (Exception e) {
            System.out.println("Exception while loading invoice projects: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load invoice projects", e);
        }
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT * FROM project p
                                JOIN company_has_project chp ON chp.project_project_id = p.project_id
                                JOIN asset a ON a.asset_id = p.asset
                                WHERE
                                 a.asset_id = ?1 AND
                                 chp.company_company_id = ?2 AND
                                 p.project_state < ?3 AND
                                 p.deleted = ?4
                                ORDER BY p.modified_date DESC
                                LIMIT ?5,?6
                                """,
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
                    List<Media> assetMedias = (List<Media>) em.createNativeQuery("""
                                        SELECT * FROM image img
                                        JOIN asset_has_image ahi
                                        	ON img.image_id = ahi.image
                                        WHERE ahi.asset = ?1
                                        """,
                                    Media.class)
                            .setParameter(1, project.getAsset().getAssetId())
                            .getResultList();
                    project.getAsset().setImageList(assetMedias);
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading projects by asset: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load projects by asset", e);
        }
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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

            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT * FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company_has_project ahp
                                	ON ahp.project_project_id = p.project_id
                                WHERE chp.company_company_id = ?1 AND ahp.company_company_id = ?2 AND p.project_state < ?3 AND %s  > ?4 AND %s < ?5 AND p.deleted = ?6 AND p.parent IS NULL
                                ORDER BY p.modified_date DESC
                                LIMIT ?7,?8
                                """.formatted(dateField, dateField),
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
                    List<Media> assetMedias = (List<Media>) em.createNativeQuery("""
                                        SELECT * FROM image img
                                        JOIN asset_has_image ahi
                                        	ON img.image_id = ahi.image
                                        WHERE ahi.asset = ?1
                                        """,
                                    Media.class)
                            .setParameter(1, project.getAsset().getAssetId())
                            .getResultList();
                    project.getAsset().setImageList(assetMedias);
                }
            }
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading projects: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load projects", e);
        }
    }

    @GET
    @Path("countForCompany/{companyid}/{authorityid}")
    @Produces({MediaType.APPLICATION_JSON})

    public Integer countForCompany(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Number result = (Number) em.createNativeQuery("""
                                SELECT COUNT(*) FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company_has_project ahp
                                	ON ahp.project_project_id = p.project_id
                                WHERE chp.company_company_id = ?1 AND p.parent IS NULL AND
                                 ahp.company_company_id = ?2
                                """)
                    .setParameter(1, companyId)
                    .setParameter(2, authorityId)
                    .getSingleResult();
            return result.intValue();
        } catch (Exception e) {
            System.out.println("Exception while counting projects for company " + companyId + " and authority " + authorityId + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count projects for company", e);
        }
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            if (strDate == null) {
                strDate = "0";
            }
            ProjectListRefCompany ref = new ProjectListRefCompany();
            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT * FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company_has_project ahp
                                	ON ahp.project_project_id = p.project_id
                                JOIN project_has_user phu
                                	ON phu.project_project_id = p.project_id
                                WHERE chp.company_company_id = ?1 AND ahp.company_company_id = ?2 AND phu.user_user_id = ?3 AND p.project_state < ?4 AND p.modified_date > ?5 AND p.deleted = ?6 AND p.parent IS NULL
                                ORDER BY p.modified_date DESC
                                LIMIT ?7,?8
                                """,
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
            return ref;
        } catch (Exception e) {
            System.out.println("Exception while loading projects for user " + userId + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load projects for user", e);
        }
    }


    private List<Project> loadProjectsByUser(String companyId,
                                             String authorityId,
                                             String userId,
                                             String state,
                                             String strDate,
                                             boolean excludeDeleted,
                                             int batchOffset,
                                             int batchSize) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            if (strDate == null) {
                strDate = "0";
            }
            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                                SELECT * FROM project p
                                JOIN company_has_project chp
                                	ON chp.project_project_id = p.project_id
                                JOIN company_has_project ahp
                                	ON ahp.project_project_id = p.project_id
                                JOIN project_has_user phu
                                	ON phu.project_project_id = p.project_id
                                WHERE chp.company_company_id = ?1 AND ahp.company_company_id = ?2 AND phu.user_user_id = ?3 AND p.project_state < ?4 AND p.modified_date > ?5 AND p.deleted = ?6 AND p.parent IS NULL
                                ORDER BY p.modified_date DESC
                                LIMIT ?7,?8
                                """,
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

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while loading projects by user " + userId + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load projects by user", e);
        }
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

        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);
        for (Project project : projects) {
            optimizeProject(project, authority.getIsLiteAccount());
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

        if (parameters.isDeleted) {
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
                    String sortDate = "modified_date";
                    if (parameters.dateField == ProjectRequestParameters.DateField.CREATED_DATE) {
                        sortDate = "created_date";
                    }
                    orderLimitString = " ORDER BY p." + sortDate + " DESC LIMIT " + parameters.batchOffset + "," + parameters.batchSize + " ";
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
            String nextDateQuery = " (p.next_control_date > '" + strFromDate + "' AND p.next_control_date < '" + strToDate + "')\n";
            String dateField = parameters.dateField.getValue();
            if (parameters.dateField != ProjectRequestParameters.DateField.SCHEDULED_DATE) {
                queryDateString = " ((p." + dateField + " > '" + strFromDate + "' AND p." + dateField + " <= '" + strToDate + "') OR " +
                        nextDateQuery;
                if (parameters.excludeUpcoming) {
                    queryDateString = " (p." + dateField + " > '" + strFromDate + "' AND p." + dateField + " <= '" + strToDate + "'\n";
                }
                if (parameters.isUpcoming) {
                    queryDateString = /*"(" + */nextDateQuery;
                } else {
                    queryDateString += ")";
                }
                queryDateString += " AND \n";
            } else {
                queryDateString =
                        "(( \n" +
                                "(p.scheduled_start_date is NOT NULL AND p.scheduled_start_date >= '" + strFromDate + "' AND p.scheduled_start_date <= '" + strToDate + "')\n" +
                                "OR\n" +
                                "(p.scheduled_start_date is NULL AND p.created_date >= '" + strFromDate + "' AND p.created_date <= '" + strToDate + "'))\n";
                if (!parameters.excludeUpcoming) {
                    queryDateString +=
                            "OR\n" +
                                    nextDateQuery +
                                    ")\n";
                }
                queryDateString += " AND \n";
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {

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
            return disiplines;
        } catch (Exception e) {
            System.out.println("Exception while loading relevant disiplines: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to load relevant disiplines", e);
        }
    }

    @PUT
    @Path("countProjects")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})

    public Integer countProjects(ProjectRequestParameters parameters) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            String sqlQuery = createSqlString(parameters, true, "");
            Query queryCounter = em.createNativeQuery(sqlQuery);
            //.setParameter(1, parameters.authorityId);
            Number counterUnassigned = (Number) queryCounter.getSingleResult();
            Integer integerCounter = Integer.parseInt(counterUnassigned.toString());

            return integerCounter;
        } catch (Exception e) {
            System.out.println("Exception while counting projects: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to count projects", e);
        }
    }


    @Deprecated
    @PUT
    @Path("loadProjects")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjects(ProjectRequestParameters parameters) {
        try {
            Company authority = CompanyFacadeREST.getInstance().findNative(parameters.authorityId);

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                String queryString = createSqlString(parameters, false, "");
                List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                        .getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, authority.getIsLiteAccount());
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste prosjekter", e);
        }
    }


    @PUT
    @Path("loadProjectsOptimized")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsOptimized(ProjectRequestParameters parameters) {
        try {
            Company authority = CompanyFacadeREST.getInstance().findNative(parameters.authorityId);

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                String queryString = createSqlString(parameters, false, "");
                List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                        .setParameter(1, parameters.authorityId)
                        .getResultList();

                List<String> projectIds = new ArrayList<>();

                for (Project project : resultList) {
                    optimizeProject(project, authority.getIsLiteAccount());
                    projectIds.add(project.getProjectId());
                    List<Project> activeChildren = project.getProjectList().stream().filter(r -> r.isDeleted() == false).toList();
                    project.childCounter = activeChildren.size();
                    project.getProjectList().clear();  // WARNING::: Removing children for hard optimizing
                    for (Project child : project.getProjectList()) {

                        optimizeProject(child, authority.getIsLiteAccount());
                        child.setDisipline(null);
                        for (User user : child.getUserList()) {
                            user.setUserRoleList(new ArrayList<>());
                        }
                        child.setUserRoleList(new ArrayList<>());
                    }
//                    Disipline reducedDisipline = project.getDisipline();
//                    reducedDisipline.setEquipmentTypeList(new ArrayList<>());
//                    reducedDisipline.setCompanyList(new ArrayList<>());
//                    reducedDisipline.setMeasurementList(new ArrayList<>());
//                    reducedDisipline.setParameterList(new ArrayList<>());
//                    reducedDisipline.setCheckListList(new ArrayList<>());
//                    reducedDisipline.setReportList(new ArrayList<>());
//                    project.setDisipline(reducedDisipline);
                    for (User user : project.getUserList()) {
                        user.setUserRoleList(new ArrayList<>());
                    }
                    //project.setUserRoleList(new ArrayList<>());
                }
                if(!resultList.isEmpty()) {
                    String authorityId = resultList.get(0).authorityId;
                    if (authorityId == null || authorityId.isBlank()) {
                        if (resultList.get(0).getAuthority() != null) {
                            authorityId = resultList.get(0).getAuthority().getCompanyId();
                        }
                    }

                    ObservationRequestParameters observationRequestParameters = new ObservationRequestParameters();
                    observationRequestParameters.authorityId = authorityId;
                    observationRequestParameters.fromDate = null;
                    observationRequestParameters.toDate = null;
                    observationRequestParameters.showOpenOnly = true;
                    observationRequestParameters.disiplineIds = new ArrayList<>();
                    observationRequestParameters.tgs.add(1);
                    observationRequestParameters.tgs.add(2);
                    observationRequestParameters.tgs.add(3);
                    observationRequestParameters.parentEntity = "project";
                    observationRequestParameters.entityIds = projectIds;
                    List<DeviationCounter> deviationCounters = ObservationFacadeREST.getInstance().countProjectDeviations(observationRequestParameters);

                    for(Project project: resultList) {
                        DeviationCounter deviationCounter = deviationCounters.stream()
                                .filter(dc -> dc.getEntityId().equals(project.getProjectId()))
                                .findFirst()
                                .orElse(null);
                        if (deviationCounter != null) {
                            project.deviationCounter = deviationCounter;
                        }
                    }
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av optimaliserte prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste optimaliserte prosjekter", e);
        }
    }

    @PUT
    @Path("loadProjectsUltraOptimized")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Project> loadProjectsUltraOptimized(ProjectRequestParameters parameters) {
        List<Project> projects = loadProjectsOptimized(parameters);

        List<Project> optimizedProjects = new ArrayList<>();
        for(Project project: projects) {
            Project op = new Project();
            op.setProjectId(project.getProjectId());
            op.setName(project.getName());
            op.setCustomerName(project.getCustomerName());
            op.setCustomerId(project.getCustomerId());
            op.setProjectNumber(project.getProjectNumber());
            op.setProjectState(project.getProjectState());
            op.setCreatedDate(project.getCreatedDate());
            op.setModifiedDate(project.getModifiedDate());
            op.setStartDate(project.getStartDate());
            op.setEndDate(project.getEndDate());
            op.assetName = project.assetName;
            optimizedProjects.add(op);
        }
        return optimizedProjects;
    }

    @PUT
    @Path("loadProjectIds")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<String> loadProjectIds(ProjectRequestParameters parameters) {

        List<Project> resultList = loadProjectsOptimized(parameters);
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
        try {
            Company authority = CompanyFacadeREST.getInstance().findNative(parameters.authorityId);

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                String queryString = createSqlString(parameters, false, "");
                List<Project> resultList = em.createNativeQuery(queryString, Project.class)
                        .setParameter(1, parameters.authorityId)
                        .getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, authority.getIsLiteAccount());

                    project.getUserList().clear();
                    project.getUserRoleList().clear();
                    project.setDisipline(null);
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av minimale prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste minimale prosjekter", e);
        }
    }

    @GET
    @Path("loadLatestProject/{assetId}/{authorityId}/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> loadLatestProject(
            @PathParam("assetId") String assetId,
            @PathParam("authorityId") String authorityId,
            @PathParam("disiplineId") String disiplineId
    ) {
        try {
            String sqlQuery = """
                SELECT p.*
                FROM project p
                         JOIN company_has_project chp ON p.project_id = chp.project_project_id
                WHERE disipline = ?1
                  AND asset = ?2
                  AND chp.company_company_id = ?3
                ORDER BY created_date DESC
                LIMIT 0,1;
                """;

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Project> resultList = em.createNativeQuery(sqlQuery, Project.class)
                        .setParameter(1, disiplineId)
                        .setParameter(2, assetId)
                        .setParameter(3, authorityId)
                        .getResultList();

                for (Project project : resultList) {
                    if (project.getAsset() != null) {
                        project.assetId = project.getAsset().getAssetId();
                        project.assetName = project.getAsset().getDefaultName();
                        project.setAsset(null);
                    }
                    project.disiplineId = project.getDisipline().getDisiplineId();
                    project.setDisipline(null);
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av siste prosjekt: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste siste prosjekt", e);
        }
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

        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);
        for (Project project : projects) {
            optimizeProject(project, authority.getIsLiteAccount());
        }
        return projects;
    }

    private void optimizeProject(Project project, boolean isLiteAccount) {

        List<Integer> tgs = new ArrayList<>();
        tgs.add(0);
        tgs.add(1);
        tgs.add(2);
        tgs.add(3);

        project.observationCounter = ObservationFacadeREST.getInstance().countProjectObservationsNative(project.getProjectId(), tgs);
        if (project.getCustomer() != null) {
            project.setCustomerId(project.getCustomer().getCompanyId());
            project.setCustomerName(project.getCustomer().getName());
        } else {
            System.out.println("Project missing customer");
        }

        if (isLiteAccount) {

        } else {
            if (project.getCustomer() != null) {
                project.setCustomerName(project.getCustomer().getFullName());
            }
        }
        if (project.getAuthority() != null) {
            project.authorityId = project.getAuthority().getCompanyId();
            project.authorityName = project.getAuthority().getFullName();
        }
        if (project.getParent() != null) {
            project.parentId = project.getParent().getProjectId();
        }
        if (project.getDisipline() != null) {
            project.disiplineId = project.getDisipline().getDisiplineId();
            project.disiplineName = project.getDisipline().getName();
        }
        if (project.getAsset() != null) {
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

        for (Project project : resultList) {
            ProjectLite projectLE = new ProjectLite(project);

            List<Integer> tgs = new ArrayList<>();
            tgs.add(0);
            tgs.add(1);
            tgs.add(2);
            tgs.add(3);

            List<UserRole> userRoles = UserRoleFacadeREST.getInstance().loadByProject(project.getProjectId(), userId);
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

            projectLE.observationCounter = ObservationFacadeREST.getInstance().countProjectObservationsNative(project.getProjectId(), tgs);
            projectJsons.add(projectLE);
        }
        return projectJsons;
    }

    @PUT
    @Path("renewObservations/{renewedProjectId}/{oldProjectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})

    public Integer renewObservations(@PathParam("renewedProjectId") String renewedProjectId, @PathParam("oldProjectId") String oldProjectId, List<String> observationIds) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
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
                return counter;
            } catch (Exception exp) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Feil ved fornyelse av observasjoner: " + exp.getMessage());
                exp.printStackTrace(System.err);
                throw new RuntimeException("Kunne ikke fornye observasjoner", exp);
            }
        } catch (Exception e) {
            System.out.println("Feil ved opprettelse av EntityManager: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke fornye observasjoner", e);
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

        for (Project project : resultList) {
            ProjectLite projectLE = new ProjectLite(project);

            List<Integer> tgs = new ArrayList<>();
            tgs.add(0);
            tgs.add(1);
            tgs.add(2);
            tgs.add(3);

            projectLE.observationCounter = ObservationFacadeREST.getInstance().countProjectObservationsNative(project.getProjectId(), tgs);
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


        Company authority = CompanyFacadeREST.getInstance().findNative(authorityId);

        for (Project project : projects) {
            optimizeProject(project, authority.getIsLiteAccount());
        }
        return projects;
        //return loadProjects(companyId, authorityId, userId, state, null, true, offset, size);
    }


    private List<Project> queryProjects(String companyId,
                                        String authorityId,
                                        String queryString,
                                        boolean excludeDeleted) {
        try {
            int projectNumber = -1;
            try {
                projectNumber = Integer.parseInt(queryString);
            } catch (Exception e) {
                // Ikke et tall, fortsetter med søkestreng
            }

            queryString = "%" + queryString + "%";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Project> resultList = (List<Project>) em.createNativeQuery("""
                    SELECT * FROM project p
                    JOIN company_has_project chp
                        ON chp.project_project_id = p.project_id
                    JOIN company_has_project ahp
                        ON ahp.project_project_id = p.project_id
                    LEFT JOIN asset a ON a.asset_id = p.asset
                    WHERE chp.company_company_id = ?1
                      AND ahp.company_company_id = ?2
                      AND p.deleted = 0
                      AND (p.name LIKE ?3 OR p.project_number = ?4 OR a.name LIKE ?3 OR p.customer_name LIKE ?3)
                    ORDER BY p.modified_date DESC
                    LIMIT 0,10
                    """, Project.class)
                        .setParameter(1, companyId)
                        .setParameter(2, authorityId)
                        .setParameter(3, queryString)
                        .setParameter(4, projectNumber)
                        .getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, false);
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved søk i prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke i prosjekter", e);
        }
    }


    private List<Project> queryProjectsOptimized(String companyId,
                                                 String authorityId,
                                                 String queryString,
                                                 boolean excludeDeleted) {
        try {
            int projectNumber = -1;
            try {
                projectNumber = Integer.parseInt(queryString);
            } catch (Exception e) {
                // Ikke et tall, fortsetter med søkestreng
            }

            queryString = "%" + queryString + "%";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Project> resultList = (List<Project>) em.createNativeQuery("""
                    SELECT * FROM project p
                    JOIN company_has_project chp
                        ON chp.project_project_id = p.project_id
                    JOIN company_has_project ahp
                        ON ahp.project_project_id = p.project_id
                    LEFT JOIN asset a ON a.asset_id = p.asset
                    WHERE chp.company_company_id = ?1
                      AND ahp.company_company_id = ?2
                      AND p.deleted = 0
                      AND (p.name LIKE ?3 OR p.project_number = ?4 OR a.name LIKE ?3 OR p.customer_name LIKE ?3)
                    ORDER BY p.modified_date DESC
                    LIMIT 0,8
                    """, Project.class)
                        .setParameter(1, companyId)
                        .setParameter(2, authorityId)
                        .setParameter(3, queryString)
                        .setParameter(4, projectNumber)
                        .getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, false);
                }
                for (Project project : resultList) {
                    project.setCompanyList(new ArrayList<>());
                    project.setUserRoleList(new ArrayList<>());
                    project.setProjectList(new ArrayList<>());
                    project.setDisipline(null);
                    project.setUserList(new ArrayList<>());
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved optimalisert søk i prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke i prosjekter (optimalisert)", e);
        }
    }

    @GET
    @Path("queryProjects/{companyid}/{authorityid}/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> queryProjects(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId, @PathParam("query") String query) {
        List<Project> projects = queryProjects(companyId, authorityId, query, true);
        return projects;
    }

    @GET
    @Path("queryProjectsFaster/{companyId}/{authorityId}/{queryString}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Project> queryProjectsFaster(@PathParam("companyId") String companyId,
                                             @PathParam("authorityId") String authorityId,
                                             @PathParam("queryString") String queryString) {
        try {
            int projectNumber = -1;
            try {
                projectNumber = Integer.parseInt(queryString);
            } catch (Exception e) {
                // Ikke et tall, fortsetter med søkestreng
            }

            queryString = "%" + queryString + "%";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<Project> resultList = (List<Project>) em.createNativeQuery("""
                    SELECT * FROM project p
                    JOIN company_has_project chp
                        ON chp.project_project_id = p.project_id
                    JOIN company_has_project ahp
                        ON ahp.project_project_id = p.project_id
                    WHERE chp.company_company_id = ?1
                      AND ahp.company_company_id = ?2
                      AND p.deleted = 0
                      AND (p.name LIKE ?3 OR p.project_number = ?4 OR p.customer_name LIKE ?3)
                    ORDER BY p.modified_date DESC
                    LIMIT 0,10
                    """, Project.class)
                        .setParameter(1, companyId)
                        .setParameter(2, authorityId)
                        .setParameter(3, queryString)
                        .setParameter(4, projectNumber)
                        .getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, false);
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved raskere søk i prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke i prosjekter (raskere)", e);
        }
    }

    @GET
    @Path("queryProjectsOptimized/{companyid}/{authorityid}/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> queryProjectsOptimized(@PathParam("companyid") String companyId, @PathParam("authorityid") String authorityId, @PathParam("query") String query) {
        List<Project> projects = queryProjectsOptimized(companyId, authorityId, query, true);
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


        ProjectListRefCompany ref = new ProjectListRefCompany();
        DateTime jodaDateTime = new DateTime(strDate);
        List<Project> resultList = loadProjects(companyId, companyId, state, jodaDateTime.toString(), null, 0, 20, false, false);
        for (Project project : resultList) {
            if (!ref.getProjects().contains(project)) {
                ref.getProjects().add(project);
            }
        }
        return ref;
    }

    @GET
    @Path("loadUpdatedProjectsOptimized/{companyid}/{updateDate}/{state}/")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Project> findProjectsRefCompanyDateOptimized(@PathParam("companyid") String companyId, @PathParam("updateDate") String strDate, @PathParam("state") String state) {
        DateTime jodaDateTime = new DateTime(strDate);
        List<Project> resultList = loadProjects(companyId, companyId, state, jodaDateTime.toString(), null, 0, 20, false, false);

        Company authority = CompanyFacadeREST.getInstance().findNative(companyId);
        for (Project project : resultList) {
            optimizeProject(project, authority.getIsLiteAccount());
        }
        return resultList;
    }

    @GET
    @Path("companyprojects/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public CompanyProject findCompanyProjects(@PathParam("companyid") String companyId) {

        CompanyProject companyProject = new CompanyProject();
        Company company = CompanyFacadeREST.getInstance().find(companyId);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<String> missingIds = new ArrayList<>();
            for (String projectId : projectIds) {
                Query query = em.createNativeQuery("""
                    SELECT COUNT(*) FROM project p
                    WHERE p.project_id = ?1
                    """)
                        .setParameter(1, projectId);

                Number counter = (Number) query.getSingleResult();
                int intCounter = Integer.parseInt(counter.toString());

                if (intCounter == 0) {
                    missingIds.add(projectId);
                }
            }
            return missingIds;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter manglende prosjekter: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke etter manglende prosjekter", e);
        }
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})

    public List<Project> loadByIntegration(@PathParam("service") String service, @PathParam("primaryKey") String primaryKey, @PathParam("authorityId") String authorityId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Project> resultList = (List<Project>) em.createNativeQuery("""
                SELECT * FROM project p
                JOIN project_has_integration phi on phi.project_project_id = p.project_id
                JOIN integration i on i.integration_id = phi.integration_integration_id
                JOIN company_has_project chp on chp.project_project_id = p.project_id
                WHERE i.keyy = 'PRIMARY_KEY'
                  AND i.service = ?1
                  AND i.valuee = ?2
                  AND chp.company_company_id = ?3
                """, Project.class)
                    .setParameter(1, service)
                    .setParameter(2, primaryKey)
                    .setParameter(3, authorityId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av prosjekter via integrasjon: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste prosjekter via integrasjon", e);
        }
    }

    @PUT
    @Path("loadByIds/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)

    public List<Project> loadByIds(List<String> projectIds) {
        if (projectIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Bygg placeholder string for IN clause
            String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                Query query = em.createNativeQuery("""
                    SELECT * FROM project p
                    WHERE p.project_id IN (%s)
                    """.formatted(placeholders), Project.class);

                // Sett parametere
                for (int i = 0; i < projectIds.size(); i++) {
                    query.setParameter(i + 1, projectIds.get(i));
                }

                List<Project> resultList = (List<Project>) query.getResultList();

                for (Project project : resultList) {
                    optimizeProject(project, false);
                    project.setDisipline(null);
                    project.getUserList().clear();
                    project.getUserRoleList().clear();
                }
                return resultList;
            }
        } catch (Exception e) {
            System.out.println("Feil ved lasting av prosjekter etter IDer: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste prosjekter etter IDer", e);
        }
    }


    private Integer countReportTasksForProject(String projectId) {
        try {
            Project project = findOptimized(projectId);
            String comparer = ">";

            // Check if disipline is from Flir ACE platform.
            // Note to self: QUICK AND VERY DIRTY
            if (project.getDisipline().getDisiplineId().equalsIgnoreCase("7e445a7a-ce06-474c-93ff-d718a8c8cfe70")) {
                comparer = ">=";
            }
            // Check if projects disipline hides TGs
            if (Parameter.getBooleanParameter(project, Parameter.ParameterType.REPORT_HIDE_TG)) {
                comparer = ">=";
            }

            String sqlCountObservation = """
                SELECT COUNT(i.image_id) FROM observation o
                JOIN observation_has_image ohi ON o.observation_id = ohi.observation
                JOIN image i ON ohi.image = i.image_id
                WHERE o.deleted = 0
                  AND i.deleted = 0
                  AND deviation %s 0
                  AND o.project = ?1
                """.formatted(comparer);

            String sqlCountChecklistQuestions = """
                SELECT COUNT(DISTINCT clq.check_list_question_id) as clCount
                FROM answer_value av
                JOIN check_list_answer cla ON av.check_list_answer = cla.check_list_answer_id
                JOIN check_list_question clq ON cla.check_list_question = clq.check_list_question_id
                WHERE av.project = ?1
                """;

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                Query query = em.createNativeQuery(sqlCountObservation);
                query.setParameter(1, projectId);
                Number oCounter = (Number) query.getSingleResult();

                Query query2 = em.createNativeQuery(sqlCountChecklistQuestions);
                query2.setParameter(1, projectId);
                Number clCounter = (Number) query2.getSingleResult();

                int taskCounter = Integer.parseInt(oCounter.toString()) + Integer.parseInt(clCounter.toString());
                return taskCounter;
            }
        } catch (Exception e) {
            System.out.println("Feil ved telling av rapportoppgaver for prosjekt: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke telle rapportoppgaver for prosjekt", e);
        }
    }

    @GET
    @Path("countReportTasks/{projectId}")
    public Integer countReportTasks(@PathParam("projectId") String projectId) {
        int taskCounter = 0;
        taskCounter = countReportTasksForProject(projectId);
        // Count for child projects
        Project project = findNative(projectId);
        for (Project child : project.getProjectList()) {
            taskCounter += countReportTasksForProject(child.getProjectId()) + 2;
        }
        return taskCounter;
    }

}
