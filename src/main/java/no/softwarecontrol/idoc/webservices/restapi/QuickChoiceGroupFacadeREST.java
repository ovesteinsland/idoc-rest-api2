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
import no.softwarecontrol.idoc.data.entityhelper.CheckListAssetReference;
import no.softwarecontrol.idoc.data.entityhelper.QuickChoiceParentReference;
import no.softwarecontrol.idoc.data.entityjson.QuickChoiceGroupLite;
import no.softwarecontrol.idoc.data.entityobject.Asset;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroup;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.quickchoicegroup")
@RolesAllowed({"ApplicationRole"})
public class QuickChoiceGroupFacadeREST extends AbstractFacade<QuickChoiceGroup> {

    @EJB
    private AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
    @EJB
    private DisiplineFacadeREST disiplineFacadeREST = new DisiplineFacadeREST();

    public QuickChoiceGroupFacadeREST() {
        super(QuickChoiceGroup.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "QuickChoiceGroup.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(QuickChoiceGroup entity) {
        super.create(entity);
    }

    @PUT
    @Path("linkToDisipline/{disiplineId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToDisipline(@PathParam("disiplineId") String disiplineId, QuickChoiceGroup entity) {
        QuickChoiceGroup quickChoiceGroup = this.find(entity.getQuickChoiceGroupId());
        Disipline disipline = disiplineFacadeREST.find(disiplineId);
        if (disipline != null && quickChoiceGroup != null) {
            if (!disipline.getQuickChoiceGroupList().contains(quickChoiceGroup)) {
                disipline.getQuickChoiceGroupList().add(quickChoiceGroup);
                disiplineFacadeREST.edit(disipline);
            }
            quickChoiceGroup.setDisipline(disipline);
            this.edit(quickChoiceGroup);

        }
    }

    @PUT
    @Path("createWithAsset")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAsset(CheckListAssetReference reference) {
//        QuickChoiceGroup checkList = reference.getCheckList();
//        super.create(checkList);
//        Asset asset = assetFacadeREST.find(reference.getAssetId());
//        checkList.getAssetList().add(asset);
//        asset.getCheckListList().add(checkList);
//        super.edit(checkList);
//        assetFacadeREST.edit(asset);
    }

    @PUT
    @Path("createWithParent")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithParent(QuickChoiceParentReference reference) {
        QuickChoiceGroup checkList = reference.getQuickChoiceGroup();
        QuickChoiceGroup parent = this.find(reference.getParentId());
        checkList.setParent(parent);
        parent.getQuickChoiceGroupList().add(checkList);
        super.create(checkList);
        super.edit(parent);
    }
    
    @PUT
    @Path("{id}")
    /* id to parent check list*/
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, QuickChoiceGroup entity) {
        if (id != null) {
            QuickChoiceGroup quickChoiceGroup = super.find(id);
            if (quickChoiceGroup != null) {
                quickChoiceGroup.setDeleted(entity.isDeleted());
                quickChoiceGroup.setName(entity.getName());
                quickChoiceGroup.setSortIndex(entity.getSortIndex());
            }
            super.edit(quickChoiceGroup);
        }

    }

    @PUT
    @Path("editWithParent/{id}")
    /* id to parent check list*/
    @Consumes({MediaType.APPLICATION_JSON})
    public void editWithParent(@PathParam("id") String id, QuickChoiceGroup entity) {
        if (id != null) {
            QuickChoiceGroup parent = super.find(id);
            if (parent != null) {
                entity.setParent(parent);
            }
        }
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public QuickChoiceGroup find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> findAll() {
        return super.findAll();
    }


    public QuickChoiceGroup findByDisipline(String id) {
        QuickChoiceGroup quickChoiceGroup = null;
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<QuickChoiceGroup> resultList = (List<QuickChoiceGroup>) em.createNativeQuery("SELECT "
                        + "* FROM quick_choice_group qcg\n"
                        + "WHERE qcg.disipline = ?1",
                QuickChoiceGroup.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (!resultList.isEmpty()) {
            return resultList.get(0);
        }
        return quickChoiceGroup;
    }

    public List<QuickChoiceGroup> findByDisipline2(String id) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<QuickChoiceGroup> quickChoiceGroups = (List<QuickChoiceGroup>) em.createNativeQuery("SELECT "
                        + "* FROM quick_choice_group qcg\n"
                        + "JOIN disipline_has_quick_choice_group dhqcg \n"
                        + "	ON dhqcg.quick_choice_group_quick_choice_group_id = qcg.quick_choice_group_id\n"
                        + "WHERE dhqcg.disipline_disipline_id = ?1",
                QuickChoiceGroup.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        return quickChoiceGroups;
    }

    @GET
    @Path("loadByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> loadByDisipline(@PathParam("disiplineId") String disiplineId) {
        List<QuickChoiceGroup> disiplineGroups = findByDisipline2(disiplineId);
        return disiplineGroups;
    }


    @GET
    @Path("loadMeasurementsByDisipline/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> loadMeasurementsByDisipline(@PathParam("disiplineId") String disiplineId) {
        List<QuickChoiceGroup> roots = new ArrayList<>();
        List<QuickChoiceGroup> disiplineGroups = findByDisipline2(disiplineId);
        //QuickChoiceGroup disiplineGroup = findByDisipline2(disiplineId);
        for (QuickChoiceGroup group : disiplineGroups) {
            if (group.getName().equalsIgnoreCase("Målinger")) {
                roots.add(group);
            }
        }
        return roots;
    }

    @GET
    @Path("loadMeasurementsByDisipline2/{disiplineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> loadMeasurementsByDisipline2(@PathParam("disiplineId") String disiplineId) {
        List<QuickChoiceGroup> roots = new ArrayList<>();
        List<QuickChoiceGroup> disiplineGroups = findByDisipline2(disiplineId);
        //QuickChoiceGroup disiplineGroup = findByDisipline2(disiplineId);
        for (QuickChoiceGroup group : disiplineGroups) {
            if (group.getName().equalsIgnoreCase("Measurements")) {
                roots.add(group);
            }
        }
        return roots;
    }

    @GET
    @Path("root")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> findRoot() {
        List<QuickChoiceGroup> roots = new ArrayList<>();
        List<QuickChoiceGroup> checkLists = this.findAll();
        for (QuickChoiceGroup checkList : checkLists) {
            if (checkList.getParent() == null) {
                roots.add(checkList);
            }
        }
        return roots;
    }

    @GET
    @Path("rootLites")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroupLite> findRootLites() {
        List<QuickChoiceGroup> roots = findRoot();
        List<QuickChoiceGroupLite> lites = new ArrayList<>();
        for(QuickChoiceGroup quickChoiceGroup: roots) {
            lites.add(new QuickChoiceGroupLite(quickChoiceGroup));
        }
        return lites;
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    @GET
    @Path("findAssetQuickChoiceGroups/{assetid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceGroup> findAssetQuickChoiceGroups(@PathParam("assetid") String assetId) {
        Asset asset = assetFacadeREST.find(assetId);
        List<QuickChoiceGroup> checkLists = new ArrayList<>();
        for (QuickChoiceGroup checkList : asset.getQuickChoiceGroupList()) {
            if (checkList.getParent() == null) {
                checkLists.add(checkList);
            }
        }
        return checkLists;
    }

}
