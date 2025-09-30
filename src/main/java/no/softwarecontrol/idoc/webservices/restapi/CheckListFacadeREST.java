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
import no.softwarecontrol.idoc.data.entityobject.*;
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
    @Path("loadOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public CheckList loadOptimized(@PathParam("id") String id) {
        CheckList checkList = super.find(id);
        return optimize(checkList);
    }

    private CheckList optimize(CheckList optimized) {
        for(Disipline disipline: optimized.getDisiplineList()) {
            optimized.getDisiplineIds().add(disipline.getDisiplineId());
        }
        optimized.getDisiplineList().clear();
        for(EquipmentType equipmentType: optimized.getEquipmentTypeList()) {
            optimized.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
        }
        optimized.getEquipmentTypeList().clear();
        for(CheckListSection checkListSection: optimized.getCheckListSectionList()) {
            for(CheckListQuestion checkListQuestion: checkListSection.getCheckListQuestionList()) {
                for(EquipmentType equipmentType: checkListQuestion.getEquipmentTypeList()) {
                    checkListQuestion.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
                }
                checkListQuestion.getEquipmentTypeList().clear();
                for(CheckListAnswer checkListAnswer: checkListQuestion.getCheckListAnswerList()) {
                    for(QuickChoiceItem quickChoiceItem: checkListAnswer.getQuickChoiceItemList()) {
                        checkListAnswer.getQuickChoiceItemIds().add(quickChoiceItem.getQuickChoiceItemId());
                    }
                    checkListAnswer.getQuickChoiceItemList().clear();
                }
            }
        }
        return optimized;
    }

    @GET
    @Path("root")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> findRoot() {
        List<CheckList> roots = new ArrayList<>();
        List<CheckList> checkLists = this.findAll();
        for(CheckList checkList: checkLists)
        for(EquipmentType equipmentType: checkList.getEquipmentTypeList()) {
            checkList.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
        }
        for (CheckList checkList : checkLists) {
            if (checkList.getParent() == null) {
                roots.add(checkList);
            }
        }
        return roots;
    }

    @GET
    @Path("loadAllByDisiplines")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadAllByDisiplines() {
        String sql = """
                select DISTINCT cl.* from disipline_has_check_list dhcl
                join check_list cl on dhcl.check_list = cl.check_list_id
                """;

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<CheckList> resultList = (List<CheckList>) em.createNativeQuery(sql, CheckList.class)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadAllByDisiplinesLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckListLite> loadAllByDisiplinesLites() {
        List<CheckList> roots = loadAllByDisiplines();
        List<CheckListLite> lites = new ArrayList<>();
        for(CheckList checkList: roots) {
            lites.add(new CheckListLite(checkList));
        }
        return lites;
    }

    @GET
    @Path("loadAllByEquipmentTypes")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadAllByEquipmentTypes() {
        String sql = """
                select DISTINCT cl.* from equipment_type_has_check_list ethcl
                join check_list cl on ethcl.check_list_check_list_id = cl.check_list_id
                
                """;

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<CheckList> resultList = (List<CheckList>) em.createNativeQuery(sql, CheckList.class)
                .getResultList();
        em.close();

        for(CheckList checkList: resultList) {
            for(EquipmentType equipmentType: checkList.getEquipmentTypeList()) {
                if(!checkList.getEquipmentTypeIds().contains(equipmentType.getEquipmentTypeId())) {
                    checkList.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
                }
            }
        }
        return resultList;
    }

    @GET
    @Path("loadAllByEquipmentTypesLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckListLite> loadAllByEquipmentTypesLites() {
        List<CheckList> roots = loadAllByEquipmentTypes();

        List<CheckListLite> lites = new ArrayList<>();
        for(CheckList checkList: roots) {
            lites.add(new CheckListLite(checkList));

        }
        return lites;
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
        for(CheckList checkList: resultList) {
            for (EquipmentType equipmentType : checkList.getEquipmentTypeList()) {
                checkList.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
            }
        }
        return  resultList;
    }

    @GET
    @Path("loadByEquipmentTypeOptimized/{equipmentTypeId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByEquipmentTypeOptimized(@PathParam("equipmentTypeId") String equipmentTypeId) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<CheckList> resultList = loadByEquipmentType(equipmentTypeId);
        List<CheckList> optimizedList = new ArrayList<>();
        for(CheckList checkList: resultList) {
            optimizedList.add(optimize(checkList));
        }
        return  optimizedList;
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
