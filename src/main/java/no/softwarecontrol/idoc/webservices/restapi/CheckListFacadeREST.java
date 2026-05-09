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

    private static CheckListFacadeREST instance;


    public CheckListFacadeREST() {
        super(CheckList.class);
        instance = this;
    }

    public static CheckListFacadeREST getInstance() {
        if (instance == null) {
            instance = new CheckListFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
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
        if (existing != null) {
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
        for (Disipline disipline : optimized.getDisiplineList()) {
            optimized.getDisiplineIds().add(disipline.getDisiplineId());
        }
        optimized.getDisiplineList().clear();
        for (EquipmentType equipmentType : optimized.getEquipmentTypeList()) {
            optimized.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
        }
        optimized.getEquipmentTypeList().clear();
        for (CheckListSection checkListSection : optimized.getCheckListSectionList()) {
            for (CheckListQuestion checkListQuestion : checkListSection.getCheckListQuestionList()) {
                for (EquipmentType equipmentType : checkListQuestion.getEquipmentTypeList()) {
                    checkListQuestion.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
                }
                checkListQuestion.getEquipmentTypeList().clear();
                for (CheckListAnswer checkListAnswer : checkListQuestion.getCheckListAnswerList()) {
                    for (QuickChoiceItem quickChoiceItem : checkListAnswer.getQuickChoiceItemList()) {
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
        for (CheckList checkList : checkLists)
            for (EquipmentType equipmentType : checkList.getEquipmentTypeList()) {
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery(
                            """
                                    SELECT DISTINCT cl.*
                                    FROM disipline_has_check_list dhcl
                                    JOIN check_list cl ON dhcl.check_list = cl.check_list_id
                                    """,
                            CheckList.class)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadAllByDisiplines");
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("loadAllByDisiplinesLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckListLite> loadAllByDisiplinesLites() {
        List<CheckList> roots = loadAllByDisiplines();
        List<CheckListLite> lites = new ArrayList<>();
        for (CheckList checkList : roots) {
            lites.add(new CheckListLite(checkList));
        }
        return lites;
    }

    @GET
    @Path("loadAllByEquipmentTypes")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadAllByEquipmentTypes() {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<CheckList> resultList = em.createNativeQuery(
                            """
                                    SELECT DISTINCT cl.*
                                    FROM equipment_type_has_check_list ethcl
                                    JOIN check_list cl ON ethcl.check_list_check_list_id = cl.check_list_id
                                    """,
                            CheckList.class)
                    .getResultList();

            // Bygg opp equipment type ID-liste
            for (CheckList checkList : resultList) {
                for (EquipmentType equipmentType : checkList.getEquipmentTypeList()) {
                    if (!checkList.getEquipmentTypeIds().contains(equipmentType.getEquipmentTypeId())) {
                        checkList.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
                    }
                }
            }

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception in loadAllByEquipmentTypes");
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }


    @GET
    @Path("loadAllByEquipmentTypesLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckListLite> loadAllByEquipmentTypesLites() {
        List<CheckList> roots = loadAllByEquipmentTypes();

        List<CheckListLite> lites = new ArrayList<>();
        for (CheckList checkList : roots) {
            lites.add(new CheckListLite(checkList));

        }
        return lites;
    }

    @GET
    @Path("loadByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByDisipline(@PathParam("disiplineId") String disiplineId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            Disipline disipline = DisiplineFacadeREST.getInstance().find(disiplineId);
            List<CheckList> checkLists = disipline.getCheckListList();
            return checkLists;
//            String sql = """
//
//                        select * from check_list cl
//                             join disipline_has_check_list dhcl on dhcl.check_list = cl.check_list_id
//                             join check_list_section clss on clss.check_list = cl.check_list_id
//                             join check_list_question clq on clq.check_list_section = clss.check_list_section_id
//                             join check_list_answer cla on cla.check_list_question = clq.check_list_question_id
//                    where dhcl.disipline = ?1
//
//                    """;
//            List<CheckList> resultList = (List<CheckList>) em.createNativeQuery(sql, CheckList.class)
//                    .setParameter(1, disiplineId)
//                    .getResultList();
//            return resultList;
        } catch (Exception e) {
            System.out.println("Feil ved søk etter brukere: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke etter brukere", e);
        }
    }

    @GET
    @Path("loadByAsset/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByAsset(@PathParam("assetId") String assetId) {
        Asset asset = AssetFacadeREST.getInstance().find(assetId);
        return asset.getCheckListList();
    }

    @GET
    @Path("loadByEquipmentType/{equipmentTypeId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByEquipmentType(@PathParam("equipmentTypeId") String equipmentTypeId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<CheckList> resultList = em.createNativeQuery(
                            "SELECT c.* " +
                                    "FROM check_list c " +
                                    "JOIN equipment_type_has_check_list ethcl ON ethcl.check_list_check_list_id = c.check_list_id " +
                                    "WHERE ethcl.equipment_type_equipment_type_id = ?1",
                            CheckList.class)
                    .setParameter(1, equipmentTypeId)
                    .getResultList();

            // Bygg opp equipment type ID-liste
            for (CheckList checkList : resultList) {
                for (EquipmentType equipmentType : checkList.getEquipmentTypeList()) {
                    checkList.getEquipmentTypeIds().add(equipmentType.getEquipmentTypeId());
                }
            }

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception in loadByEquipmentType for Equipment Type ID: " + equipmentTypeId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("loadByEquipmentTypeOptimized/{equipmentTypeId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadByEquipmentTypeOptimized(@PathParam("equipmentTypeId") String equipmentTypeId) {
        List<CheckList> resultList = loadByEquipmentType(equipmentTypeId);
        List<CheckList> optimizedList = new ArrayList<>();
        for (CheckList checkList : resultList) {
            optimizedList.add(optimize(checkList));
        }
        return optimizedList;
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
        for (CheckList checkList : roots) {
            lites.add(new CheckListLite(checkList));
        }
        return lites;
    }
}
