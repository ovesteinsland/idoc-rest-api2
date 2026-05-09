package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.data.entityobject.Role;
import no.softwarecontrol.idoc.data.entityobject.User;
import no.softwarecontrol.idoc.data.entityobject.UserRole;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.userrole")
@RolesAllowed({"ApplicationRole"})
public class UserRoleFacadeREST extends AbstractFacade<UserRole> {

    private static UserRoleFacadeREST instance;


    public UserRoleFacadeREST() {
        super(UserRole.class);
        instance = this;
    }

    public static UserRoleFacadeREST getInstance() {
        if (instance == null) {
            instance = new UserRoleFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, UserRole entity) {
        UserRole existing = find(entity.getUserRoleId());
        if(existing != null) {
            existing.setParameter(entity.getParameter());
            if(entity.getRole() != null) {
                existing.setRole(entity.getRole());
            }
            super.edit(existing);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Override
    public void create(UserRole entity) {
        super.create(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        UserRole userRole = find(id);
        if (userRole != null) {
            super.remove(userRole);
        }
    }

    @POST
    @Path("createWithProject/{projectId}/{userId}/{roleId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithProject(@PathParam("projectId") String projectId, @PathParam("userId") String userId, @PathParam("roleId") String roleId, UserRole entity) {

        List<UserRole> existingUserRoles = loadByProject(projectId, userId);
        if(existingUserRoles.isEmpty()) {
            Project project = ProjectFacadeREST.getInstance().find(projectId);

            User user = UserFacadeREST.getInstance().find(userId);

            Role role = RoleFacadeREST.getInstance().find(roleId);
            entity.setRole(role);
            entity.setProject(project);
            entity.setUser(user);
            create(entity);

            project.getUserRoleList().add(entity);
            user.getUserRoleList().add(entity);
            //role.getUserRoleList().add(entity);

            ProjectFacadeREST.getInstance().editProjectOnly(projectId, project);
            UserFacadeREST.getInstance().editPassword(userId, user);
        }
    }

    @GET
    @Path("loadByProject/{projectId}/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserRole> loadByProject(@PathParam("projectId") String projectId,
                                        @PathParam("userId") String userId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<UserRole> resultList = (List<UserRole>) em.createNativeQuery("""
                SELECT * FROM user_role ur
                WHERE ur.project = ?1 AND ur.user = ?2
                """, UserRole.class)
                    .setParameter(1, projectId)
                    .setParameter(2, userId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved lasting av brukerroller for prosjekt: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke laste brukerroller for prosjekt", e);
        }
    }

}
