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

    public AssetGroupFacadeREST() {
        super(AssetGroup.class);
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

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<AssetGroup> resultList = (List<AssetGroup>) em.createNativeQuery("SELECT * \n"
                                + " FROM asset_group ag\n "
                                + " join company_has_asset_group chag on chag.asset_group_asset_group_id = ag.asset_group_id\n"
                                + " WHERE chag.company_company_id = ?1 ",
                        AssetGroup.class)
                .setParameter(1, ownerId)
                .getResultList();
        em.close();
        for(AssetGroup assetGroup : resultList) {
            assetGroup.setAssetList(new ArrayList<>());
            assetGroup.setCompanyList(new ArrayList<>());
        }
        return resultList;
    }
    /*@PUT
    @Path("linkToCompanyAssetType/{companyId}/{assetTypeId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompanyAssetType(
            @PathParam("companyId") String companyId,
            @PathParam("assetTypeId") String assetTypeId,
            AssetGroup entity) {

        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        AssetTypeFacadeREST assetTypeFacadeREST = new AssetTypeFacadeREST();

        AssetGroup assetGroup = this.find(entity.getAssetGroupId());
        AssetType assetType = assetTypeFacadeREST.find(assetTypeId);
        Company company = companyFacadeREST.find(companyId);
        if (assetGroup != null && company != null && assetType != null) {
            assetGroup.setAssetType(assetType);
            this.edit(assetGroup);
            if (!assetGroup.getCompanyList().contains(company)) {
                assetGroup.getCompanyList().add(company);
            }
            if (!company.getAssetGroupList().contains(assetGroup)) {
                company.getAssetGroupList().add(assetGroup);
                companyFacadeREST.edit(company);
            }
            if (!assetType.getAssetGroupList().contains(assetGroup)) {
                assetType.getAssetGroupList().add(assetGroup);
            }
        }
    }*/

    @PUT
    @Path("unlinkCompany/{assetId}/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("assetId") String assetId, @PathParam("companyId") String companyId, AssetGroup entity) {
        AssetGroup assetGroup = this.find(entity.getAssetGroupId());
        AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
        Asset asset = assetFacadeREST.find(assetId);
        if (asset != null && assetGroup != null) {
            assetFacadeREST.unlinkCompany(companyId, asset);
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
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();

        AssetGroup assetGroup = this.find(entity.getAssetGroupId());
        Company company = companyFacadeREST.find(companyId);
        if (assetGroup != null && company != null) {
            if (!assetGroup.getCompanyList().contains(company)) {
                assetGroup.getCompanyList().add(company);
                this.edit(assetGroup);
            }
            if (!company.getAssetGroupList().contains(assetGroup)) {
                company.getAssetGroupList().add(assetGroup);
                companyFacadeREST.edit(company);
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

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<AssetGroup> findAll() {
        return super.findAll();
    }

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
