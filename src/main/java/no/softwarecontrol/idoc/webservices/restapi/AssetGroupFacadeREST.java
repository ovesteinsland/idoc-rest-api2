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
import no.softwarecontrol.idoc.data.entityobject.Asset;
import no.softwarecontrol.idoc.data.entityobject.AssetGroup;
import no.softwarecontrol.idoc.data.entityobject.Company;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.assetgroup")
@RolesAllowed({"ApplicationRole"})
public class AssetGroupFacadeREST extends AbstractFacade<AssetGroup> {

    private static AssetGroupFacadeREST instance;

    public AssetGroupFacadeREST() {
        super(AssetGroup.class);
    }

    public static AssetGroupFacadeREST getInstance() {
        if (instance == null) {
            instance = new AssetGroupFacadeREST();
        }
        return instance;
    }
    @Override
    protected String getSelectAllQuery(){
        return "AssetGroup.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(AssetGroup entity) {
        super.create(entity);
    }


    @GET
    @Path("loadByOwner/{ownerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AssetGroup> loadByOwner(@PathParam("ownerId") String ownerId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<AssetGroup> resultList = em.createNativeQuery(
                            "SELECT ag.* " +
                                    "FROM asset_group ag " +
                                    "JOIN company_has_asset_group chag ON chag.asset_group_asset_group_id = ag.asset_group_id " +
                                    "WHERE chag.company_company_id = ?1",
                            AssetGroup.class)
                    .setParameter(1, ownerId)
                    .getResultList();

            // Optimaliser resultatet ved å tømme lister
            for (AssetGroup assetGroup : resultList) {
                assetGroup.setAssetList(new ArrayList<>());
                assetGroup.setCompanyList(new ArrayList<>());
            }

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception in loadByOwner for Owner ID: " + ownerId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

    @PUT
    @Path("unlinkCompany/{assetId}/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("assetId") String assetId, @PathParam("companyId") String companyId, AssetGroup entity) {
        AssetGroup assetGroup = this.find(entity.getAssetGroupId());
        Asset asset = AssetFacadeREST.getInstance().find(assetId);
        if (asset != null && assetGroup != null) {
            AssetFacadeREST.getInstance().unlinkCompany(companyId, asset);
        }
        if (asset != null && assetGroup != null) {
            if (assetGroup.getAssetList().contains(asset)) {
                assetGroup.getAssetList().remove(asset);
                super.edit(assetGroup);
            }
        }

    }

    @PUT
    @Path("linkToCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("companyId") String companyId,
            AssetGroup entity) {

        AssetGroup assetGroup = this.find(entity.getAssetGroupId());
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        if (assetGroup != null && company != null) {
            if (!assetGroup.getCompanyList().contains(company)) {
                assetGroup.getCompanyList().add(company);
                this.edit(assetGroup);
            }
            if (!company.getAssetGroupList().contains(assetGroup)) {
                company.getAssetGroupList().add(assetGroup);
                CompanyFacadeREST.getInstance().edit(company);
            }
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, AssetGroup entity) {
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
    public AssetGroup find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Path("findCompanyIdList/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> findCompanyIdList(@PathParam("id") String id) {
        List<String> companyIds = new ArrayList<>();
        AssetGroup assetGroup = find(id);
        if(assetGroup != null){
            for(Company company:assetGroup.getCompanyList()){
                companyIds.add(company.getCompanyId());
            }
        }
        return companyIds;
    }

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<AssetGroup> findAll() {
//        return super.findAll();
//    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<AssetGroup> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


}
