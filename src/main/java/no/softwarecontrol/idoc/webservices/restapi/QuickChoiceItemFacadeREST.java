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

    private static QuickChoiceItemFacadeREST instance;


    public QuickChoiceItemFacadeREST() {
        super(QuickChoiceItem.class);
        instance = this;
    }

    public static QuickChoiceItemFacadeREST getInstance() {
        if (instance == null) {
            instance = new QuickChoiceItemFacadeREST();
        }
        return instance;
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
        QuickChoiceGroup quickChoiceGroup = QuickChoiceGroupFacadeREST.getInstance().find(quickChoiceGroupId);
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
        QuickChoiceGroup quickChoiceGroup = QuickChoiceGroupFacadeREST.getInstance().find(parentId);
        if (quickChoiceGroup != null && quickChoiceItem != null) {
            if (!quickChoiceGroup.getQuickChoiceItemList().contains(quickChoiceItem)) {
                quickChoiceGroup.getQuickChoiceItemList().add(quickChoiceItem);
                quickChoiceItem.setQuickChoiceGroup(quickChoiceGroup);
                QuickChoiceGroupFacadeREST.getInstance().edit(quickChoiceGroup);
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

//    @GET
//    @Override
//    @Produces({ MediaType.APPLICATION_JSON})
//    public List<QuickChoiceItem> findAll() {
//        return super.findAll();
//    }

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
        try {
            String urlSearchString = URLEncoder.encode(queryString, "UTF-8");
            urlSearchString = urlSearchString.replaceAll("\\+", "%20");
            urlSearchString = urlSearchString.replaceAll(" ", "%20");
            queryString += "*";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                List<QuickChoiceItem> quickChoiceItems = (List<QuickChoiceItem>) em.createNativeQuery("""
                    SELECT * FROM quick_choice_item
                    JOIN quick_choice_group on quick_choice_item.quick_choice_group = quick_choice_group.quick_choice_group_id
                    WHERE MATCH (quick_choice_group.name) AGAINST (?1 IN BOOLEAN MODE)
                       OR MATCH (quick_choice_item.name,full_text) AGAINST (?1 IN BOOLEAN MODE)
                    LIMIT 0,30
                    """, QuickChoiceItem.class)
                        .setParameter(1, queryString)
                        .getResultList();

                // filter away deleted from result
                List<QuickChoiceItem> items = new ArrayList<>(quickChoiceItems);

                if (!items.isEmpty()) {
                    QuickChoiceGroup root = items.get(0).getQuickChoiceGroup().getRoot();
                    List<QuickChoiceItem> filteredItems = items.stream()
                            .filter(r -> !r.isDeleted())
                            .collect(Collectors.toList());
                    return filteredItems;
                } else {
                    return items;
                }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Feil ved URL-encoding av søkestreng: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke utføre fulltekstsøk", e);
        } catch (Exception e) {
            System.out.println("Feil ved fulltekstsøk i quick choice items: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke utføre fulltekstsøk", e);
        }
    }

    @GET
    @Path("query/{query}")
    @Produces({ MediaType.APPLICATION_JSON})

    public List<QuickChoiceItem> query(@PathParam("query") String queryString) {

            String searchString = "%" + queryString + "%";

            try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                // Bruk JPQL med JOIN FETCH for å unngå N+1
                List<QuickChoiceItem> componentQueryList = em.createQuery("""
                    SELECT DISTINCT qci FROM QuickChoiceItem qci
                    LEFT JOIN FETCH qci.keyComponent kc
                    LEFT JOIN FETCH qci.keyFault kf
                    WHERE (CONCAT(COALESCE(kc.description, ''), COALESCE(kf.description, '')) LIKE :search)
                       OR qci.name LIKE :search
                    """, QuickChoiceItem.class)
                        .setParameter("search", searchString)
                        .setMaxResults(20)
                        .getResultList();

                List<QuickChoiceItem> qciQueryList = em.createQuery("""
                    SELECT DISTINCT qci FROM QuickChoiceItem qci
                    LEFT JOIN FETCH qci.keyComponent kc
                    LEFT JOIN FETCH qci.keyFault kf
                    WHERE (CONCAT(qci.name, COALESCE(qci.fullText, '')) LIKE :search)
                    """, QuickChoiceItem.class)
                        .setParameter("search", searchString)
                        .setMaxResults(20)
                        .getResultList();

                // Slå sammen listene og fjern duplikater (hvis et element finnes i begge listene)
                // Bruker LinkedHashSet for å bevare rekkefølgen til en viss grad
                java.util.Set<QuickChoiceItem> resultSet = new java.util.LinkedHashSet<>();
                resultSet.addAll(componentQueryList);
                resultSet.addAll(qciQueryList);

                return new ArrayList<>(resultSet);
            } catch (Exception e) {
            System.out.println("Feil ved søk i quick choice items: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Kunne ikke søke i quick choice items", e);
        }
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }
    
}
