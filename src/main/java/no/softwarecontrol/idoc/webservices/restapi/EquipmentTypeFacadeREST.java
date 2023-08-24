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

    public EquipmentTypeFacadeREST() {
        super(EquipmentType.class);
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
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<EquipmentType> resultList = (List<EquipmentType>) em.createNativeQuery("SELECT "
                        + "* FROM equipment_type o\n"
                        + "WHERE o.ns_number = ?1",
                EquipmentType.class)
                .setParameter(1, code)
                .getResultList();
        em.close();
        if (!resultList.isEmpty()) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<EquipmentType> findAll() {
        return super.findAll();
    }

    
}
