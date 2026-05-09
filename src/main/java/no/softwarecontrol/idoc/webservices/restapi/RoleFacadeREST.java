package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Role;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.role")
@RolesAllowed({"ApplicationRole"})
public class RoleFacadeREST extends AbstractFacade<Role> {

    private static RoleFacadeREST instance;
    public RoleFacadeREST() {
        super(Role.class);
        instance = this;
    }

    public static RoleFacadeREST getInstance() {
        if (instance == null) {
            instance = new RoleFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(Role entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Role entity) {
        Role role = this.find(id);
        if (role != null) {
            role.setName(entity.getName());
            role.setRoleGroup(entity.getRoleGroup());
            role.setRoleType(entity.getRoleType());
            super.edit(role);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Role find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Path("findByType/{type}")
    @Produces({ MediaType.APPLICATION_JSON})

    public List<Role> findByType(@PathParam("type") String type) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            type = type + "%";
            List<Role> resultList = (List<Role>) em.createNativeQuery("""
                SELECT * FROM role r
                WHERE r.role_type LIKE ?1
                """, Role.class)
                    .setParameter(1, type)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter roller etter type: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke finne roller etter type", e);
        }
    }

    @GET
    @Path("findByGroup/{group}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Role> findByGroup(@PathParam("group") String group) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Role> resultList = (List<Role>) em.createNativeQuery("""
                SELECT * FROM role r
                WHERE r.role_group = ?1
                """, Role.class)
                    .setParameter(1, group)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter roller etter gruppe: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke finne roller etter gruppe", e);
        }
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Role> findAll() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Role> resultList = (List<Role>) em.createNativeQuery("""
                SELECT * FROM role r
                """, Role.class)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved henting av alle roller: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke hente alle roller", e);
        }
    }
}
