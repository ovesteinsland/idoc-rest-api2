/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityjson.CheckListLite;
import no.softwarecontrol.idoc.data.entityobject.Asset;
import no.softwarecontrol.idoc.data.entityobject.CheckList;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.checklist")
@RolesAllowed({"ApplicationRole"})
public class CheckListFacadeREST extends AbstractFacade<CheckList> {

    public CheckListFacadeREST() {
        super(CheckList.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "CheckList.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(CheckList entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, CheckList entity) {
        CheckList existing = this.find(entity.getCheckListId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setSortIndex(entity.getSortIndex());
            existing.setDescription(entity.getDescription());
        }
        super.edit(existing);
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckList find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Path("root")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> findRoot() {
        List<CheckList> roots = new ArrayList<>();
        List<CheckList> checkLists = this.findAll();
        for (CheckList checkList : checkLists) {
            if (checkList.getParent() == null) {
                roots.add(checkList);
            }
        }
        return roots;
    }


    @GET
    @Path("loadByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByDisipline(@PathParam("disiplineId") String disiplineId) {
        DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        return disipline.getCheckListList();
    }

    @GET
    @Path("loadByAsset/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByAsset(@PathParam("assetId") String assetId) {
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Asset asset = assetFacadeREST.find(assetId);
        return asset.getCheckListList();
    }

    @GET
    @Path("loadByEquipmentType/{equipmentTypeId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByEquipmentType(@PathParam("equipmentTypeId") String equipmentTypeId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<CheckList> resultList = (List<CheckList>) em.createNativeQuery("SELECT " +
                        "c.* FROM check_list c " +
                        "JOIN equipment_type_has_check_list ethcl ON ethcl.check_list_check_list_id = c.check_list_id " +
                        "WHERE ethcl.equipment_type_equipment_type_id = ?1",
                CheckList.class)
                .setParameter(1, equipmentTypeId)
                .getResultList();
        em.close();
        return  resultList;
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> findAll() {
        return super.findAll();
    }

    @GET
    @Path("findAllLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckListLite> findAllLites() {
        List<CheckList> roots = findAll();
        List<CheckListLite> lites = new ArrayList<>();
        for(CheckList checkList: roots) {
            lites.add(new CheckListLite(checkList));
        }
        return lites;
    }
}
