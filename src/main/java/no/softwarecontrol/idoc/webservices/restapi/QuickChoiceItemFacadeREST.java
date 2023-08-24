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
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroup;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceItem;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.quickchoiceitem")
@RolesAllowed({"ApplicationRole"})
public class QuickChoiceItemFacadeREST extends AbstractFacade<QuickChoiceItem> {
    @EJB
    private QuickChoiceGroupFacadeREST quickChoiceGroupFacadeREST = new QuickChoiceGroupFacadeREST();

    public QuickChoiceItemFacadeREST() {
        super(QuickChoiceItem.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "QuickChoiceItem.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(QuickChoiceItem entity) {
        super.create(entity);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Path("createWithQuickChoiceGroup/{quickChoiceGroupId}")
    public void createWithQuickChoiceGroup(@PathParam("quickChoiceGroupId") String quickChoiceGroupId, QuickChoiceItem entity) {
        QuickChoiceGroupFacadeREST quickChoiceGroupFacadeREST = new QuickChoiceGroupFacadeREST();
        QuickChoiceGroup quickChoiceGroup = quickChoiceGroupFacadeREST.find(quickChoiceGroupId);
        if(quickChoiceGroup != null) {
            entity.setQuickChoiceGroup(quickChoiceGroup);
            super.create(entity);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, QuickChoiceItem entity) {
        QuickChoiceItem quickChoiceItem = find(id);
        if(quickChoiceItem != null){
            quickChoiceItem.setDeleted(entity.isDeleted());
            quickChoiceItem.setAction(entity.getAction());
            quickChoiceItem.setDeviationGrade(entity.getDeviationGrade());
            quickChoiceItem.setRegulationReference(entity.getRegulationReference());
            quickChoiceItem.setFullText(entity.getFullText());
            quickChoiceItem.setName(entity.getName());
            quickChoiceItem.setSortIndex(entity.getSortIndex());
            quickChoiceItem.setThermoFlag(entity.isThermoFlag());
            if(entity.getKeyComponent() != null) {
                quickChoiceItem.setKeyComponent(entity.getKeyComponent());
            }
            if(entity.getKeyFault() != null) {
                quickChoiceItem.setKeyFault(entity.getKeyFault());
            }
        }
        super.edit(quickChoiceItem);
    }
    
    @PUT
    @Path("linkToQuickChoiceGroup/{parentId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToQuickChoiceGroup(@PathParam("parentId") String parentId, QuickChoiceItem entity) {
        QuickChoiceItem quickChoiceItem = this.find(entity.getQuickChoiceItemId());
        QuickChoiceGroup quickChoiceGroup = quickChoiceGroupFacadeREST.find(parentId);
        if (quickChoiceGroup != null && quickChoiceItem != null) {
            if (!quickChoiceGroup.getQuickChoiceItemList().contains(quickChoiceItem)) {
                quickChoiceGroup.getQuickChoiceItemList().add(quickChoiceItem);
                quickChoiceItem.setQuickChoiceGroup(quickChoiceGroup);
                quickChoiceGroupFacadeREST.edit(quickChoiceGroup);
            }
            
            this.edit(quickChoiceItem);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public QuickChoiceItem find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<QuickChoiceItem> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<QuickChoiceItem> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("queryFullText/{query}/{disiplineId}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<QuickChoiceItem> queryFullText(@PathParam("query") String queryString, @PathParam("disiplineId") String disiplineId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            List<QuickChoiceItem> quickChoiceItems = new ArrayList<>();

            String urlSearchString = URLEncoder.encode(queryString, "UTF-8");
            urlSearchString = urlSearchString.replaceAll("\\+", "%20");
            urlSearchString = urlSearchString.replaceAll(" ", "%20");
            queryString += "*";

            quickChoiceItems = (List<QuickChoiceItem>) em.createNativeQuery("SELECT * FROM quick_choice_item\n" +
                            "JOIN quick_choice_group on quick_choice_item.quick_choice_group = quick_choice_group.quick_choice_group_id\n" +
                            "WHERE MATCH (quick_choice_group.name) AGAINST (?1 IN BOOLEAN MODE)" +
                            "        OR MATCH (quick_choice_item.name,full_text) AGAINST (?1 IN boolean mode) LIMIT 0,30;",
                    QuickChoiceItem.class)
                    .setParameter(1, queryString)
                    .getResultList();

            // filter away deleted from result
            List<QuickChoiceItem> items = new ArrayList<>(quickChoiceItems);
            em.close();
            if(!items.isEmpty()){
                QuickChoiceGroup root = items.get(0).getQuickChoiceGroup().getRoot();
                /*List<QuickChoiceItem> filteredItems = items.stream().filter(
                        r -> r.isDeleted() == false && r.getQuickChoiceGroup().getRoot().getDisipline().getDisiplineId().equals(disiplineId))
                        .collect(Collectors.toList());*/
                List<QuickChoiceItem> filteredItems = items.stream().filter(
                        r -> r.isDeleted() == false /*&& r.getQuickChoiceGroup().getRoot().getDisipline().getDisiplineId().equals(disiplineId)*/)
                        .collect(Collectors.toList());
                return filteredItems;
            } else {
                return items;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<>();

        } finally {
            em.close();
        }
    }

    @GET
    @Path("query/{query}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<QuickChoiceItem> query(@PathParam("query") String queryString) {
        List<QuickChoiceItem> resultList = new ArrayList<>();
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        queryString = "%" + queryString + "%";
        List<QuickChoiceItem> componentQueryList = (List<QuickChoiceItem>) em.createNativeQuery("SELECT *  \n"
                        + "FROM quick_choice_item qci\n"
                        + "JOIN key_component kc\n"
                        + "    on qci.key_component = kc.key_component_id\n"
                        + "JOIN key_fault kf\n"
                        + "	on qci.key_fault = kf.key_fault_id\n"
                        + "WHERE concat(kc.description,kf.description) LIKE ?1 OR qci.name LIKE ?1 LIMIT 0,20",
                QuickChoiceItem.class)
                .setParameter(1, queryString)
                .getResultList();
        List<QuickChoiceItem> qciQueryList = (List<QuickChoiceItem>) em.createNativeQuery("SELECT *  \n"
                        + "FROM quick_choice_item qci\n"
                        + "WHERE concat(qci.name,qci.full_text) LIKE ?1 LIMIT 0,20",
                QuickChoiceItem.class)
                .setParameter(1, queryString)
                .getResultList();
        em.close();
        resultList.addAll(componentQueryList);
        resultList.addAll(qciQueryList);
        return resultList;
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
    
}
