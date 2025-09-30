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
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.data.entityobject.ProjectGroup;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.projectgroup")
@RolesAllowed({"ApplicationRole"})
public class ProjectGroupFacadeREST extends AbstractFacade<ProjectGroup> {

    @EJB
    private ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();

    public ProjectGroupFacadeREST() {
        super(ProjectGroup.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "ProjectGroup.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(ProjectGroup entity) {
        System.out.println("creating project group");
        super.create(entity);
        System.out.println("created project group");
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, ProjectGroup entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public ProjectGroup find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<ProjectGroup> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<ProjectGroup> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    
    @PUT
    @Path("linkToProject/{projectId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void linkToProject(@PathParam("projectId") String projectId, ProjectGroup entity) {
        ProjectGroup projectGroup = this.find(entity.getProjectGroupId());
        Project project = projectFacadeREST.find(projectId);
        if (project != null && projectGroup != null) {
            if (!projectGroup.getProjectList().contains(project)) {
                projectGroup.getProjectList().add(project);
                projectFacadeREST.editProjectOnly(project.getProjectId(),project);
            }
            project.setProjectGroup(projectGroup);
            this.edit(projectGroup);
        }
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

}
