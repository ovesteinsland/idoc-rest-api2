package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.EquipmentType;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.equipmenttype")
@RolesAllowed({"ApplicationRole"})
public class EquipmentTypeFacadeREST extends AbstractFacade<EquipmentType>{

    public static EquipmentTypeFacadeREST instance;

    public EquipmentTypeFacadeREST() {
        super(EquipmentType.class);
        instance = this;
    }

    public static EquipmentTypeFacadeREST getInstance() {
        if (instance == null) {
            instance = new EquipmentTypeFacadeREST();
        }
        return instance;
    }

    public EquipmentTypeFacadeREST(Class<EquipmentType> entityClass) {
        super(entityClass);
    }

    @Override
    protected String getSelectAllQuery(){
        return "EquipmentType.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(EquipmentType entity) {
        super.create(entity);
    }


    @GET
    @Path("root")
    @Produces({MediaType.APPLICATION_JSON})
    public List<EquipmentType> findRoot() {
        List<EquipmentType> roots = new ArrayList<>();
        List<EquipmentType> equipmentTypes = this.findAll();
        for (EquipmentType equipmentType : equipmentTypes) {
            if (equipmentType.getParent() == null) {
                roots.add(equipmentType);
            }
        }
        return roots;
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public EquipmentType find(@PathParam("id") String id) {
        EquipmentType equipmentType = super.find(id);
        return equipmentType;
    }

    @GET
    @Path("findByCode/{code}")
    @Produces({ MediaType.APPLICATION_JSON})
    public EquipmentType findByCode(@PathParam("code") String code) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<EquipmentType> resultList = em.createNativeQuery("""
                SELECT *
                FROM equipment_type
                WHERE ns_number = ?1
                """,
                            EquipmentType.class)
                    .setParameter(1, code)
                    .getResultList();

            return resultList.isEmpty() ? null : resultList.get(0);

        } catch (Exception e) {
            System.out.println("Exception in findByCode for code: " + code);
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<EquipmentType> findAll() {
        return super.findAll();
    }

    @GET
    @Path("loadAll")
    @Produces({MediaType.APPLICATION_JSON})
    public List<EquipmentType> loadAll() {
        List<EquipmentType> equipmentTypes = super.findAll();
        for(EquipmentType equipmentType: equipmentTypes) {
            equipmentType.getEquipmentTypeList().clear();
        }
        return equipmentTypes;
    }

    
}
