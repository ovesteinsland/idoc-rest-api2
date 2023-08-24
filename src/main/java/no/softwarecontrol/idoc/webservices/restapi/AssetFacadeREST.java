/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.asset")
@RolesAllowed({"ApplicationRole"})
public class AssetFacadeREST extends AbstractFacade<Asset> {

//    @PersistenceContext(unitName = "no.softwarecontrol_iDocWebServices_war_1.0-SNAPSHOTPU")
//    private EntityManager em;

    public AssetFacadeREST() {
        super(Asset.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return "Asset.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Asset entity) {
        for(Equipment equipment: entity.getEquipmentList()) {
            for(Equipment child: entity.getEquipmentList()) {
                child.setParent(equipment);
            }
        }
        super.create(entity);

    }

    @POST
    @Path("createWithAssetGroup/{assetGroupId}/")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAssetGroup(@PathParam("assetGroupId") String assetGroupId, Asset asset) {
        AssetGroupFacadeREST assetGroupFacadeREST = new AssetGroupFacadeREST();
        AssetGroup assetGroup = assetGroupFacadeREST.find(assetGroupId);
        fixLocationList(asset, asset.getLocationList());
        fixEquipmentList(asset, asset.getEquipmentList());
        if (asset != null && assetGroup != null) {
            asset.setAssetGroup(assetGroup);
            assetGroup.getAssetList().add(asset);
            super.create(asset);
            assetGroupFacadeREST.edit(assetGroup);
        }
    }

    @POST
    @Path("createWithAssetGroupAndLink/{assetGroupId}/{authorityId}/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAssetGroupAndLink(
            @PathParam("assetGroupId") String assetGroupId,
            @PathParam("authorityId") String authorityId,
            @PathParam("companyId") String companyId,
            Asset asset) {
        AssetGroupFacadeREST assetGroupFacadeREST = new AssetGroupFacadeREST();
        AssetGroup assetGroup = assetGroupFacadeREST.find(assetGroupId);
        fixLocationList(asset, asset.getLocationList());
        fixEquipmentList(asset, asset.getEquipmentList());
        if (asset != null && assetGroup != null) {
            asset.setAssetGroup(assetGroup);
            assetGroup.getAssetList().add(asset);
            super.create(asset);
            assetGroupFacadeREST.edit(assetGroup);

            // link to companies
            linkCompany(companyId,asset);
            linkCompany(authorityId, asset);
        }
    }

    private void saveAssetGroup(Asset asset) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            final int i = em.createNativeQuery(
                    "UPDATE asset SET asset_group = ?\n" +
                            "WHERE (asset_id = ?);"
            ).setParameter(1, asset.getAssetGroup().getAssetGroupId())
                    .setParameter(2, asset.getAssetId())
                    .executeUpdate();
            tx.commit();
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("------------------------------------------------");
            System.out.println("Exception while update asset.asset_group: " + exp.getMessage());
            System.out.println("**************************************************************");
        } finally {
            em.close();
        }
    }

    public void replaceCustomer(String oldCustomerId, String newCustomerId, Asset asset) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company newCustomer = companyFacadeREST.findNative(newCustomerId);
        List<AssetGroup> assetGroups = newCustomer.getAssetGroupList();
        if (!assetGroups.isEmpty()) {
            AssetGroup assetGroup = assetGroups.get(0);
            asset.setAssetGroup(assetGroup);
            saveAssetGroup(asset);
            unlinkCompany(oldCustomerId, asset);
            linkCompany(newCustomerId, asset);
        }
    }

    @PUT
    @Path("linkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkCompany(@PathParam("companyId") String companyId, Asset entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Query query = em.createNativeQuery("SELECT COUNT(*) FROM company_has_asset \n " +
                    " WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, entity.getAssetId());

            Number counter = (Number) query.getSingleResult();
            if (counter.intValue() == 0) {
                tx.begin();
                final int i = em.createNativeQuery(
                        "INSERT INTO company_has_asset (company_company_id, asset_asset_id)\n" +
                                "VALUES (?, ?);"
                ).setParameter(1, companyId)
                        .setParameter(2, entity.getAssetId())
                        .executeUpdate();
                tx.commit();
            } else {
                //System.out.println("No problem: company_has_project already exists");
            }
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_asset: " + exp.getMessage());
        } finally {
            em.close();
        }

//        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
//        Asset asset = this.find(entity.getAssetId());
//        Company company = companyFacadeREST.find(companyId);
//        if (company == null){
//            System.out.println("Company == null: " + companyId);
//        } else {
//            System.out.println("Linker asset: " + company.getName());
//        }
//        if (asset != null && company != null) {
//            if (!asset.getCompanyList().contains(company)) {
//                asset.getCompanyList().add(company);
//                this.edit(asset);
//            }
//            if (!company.getAssetList().contains(asset)) {
//                company.getAssetList().add(asset);
//                companyFacadeREST.edit(company);
//            }
//        }
    }

    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("companyId") String companyId, Asset entity) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createNativeQuery("DELETE FROM company_has_asset \n " +
                    " WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, entity.getAssetId());

            Number counter = (Number) query.executeUpdate();
            if (counter.intValue() == 1) {
                System.out.println("DELETED company_has_asset SUCCEEDED");
            }
            tx.commit();
        } catch (Exception exp) {
            tx.rollback();
            System.out.println("Exception while inserting into company_has_asset: " + exp.getMessage());
        } finally {
            em.close();
        }

//        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
//        Asset asset = this.find(entity.getAssetId());
//        Company company = companyFacadeREST.find(companyId);
//        if (asset != null && company != null) {
//            asset.setDeleted(true);
//            this.edit(asset);
//            AssetGroup assetGroup = asset.getAssetGroup();
//            if (assetGroup != null) {
//
//            }
//            if (asset.getCompanyList().contains(company)) {
//                asset.getCompanyList().remove(company);
//            }
//            if (company.getAssetList().contains(asset)) {
//                company.getAssetList().remove(asset);
//                companyFacadeREST.edit(company);
//            }
//        }
    }

    @PUT
    @Path("linkCheckList/{checkListId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkCheckList(@PathParam("checkListId") String checkListId, Asset entity) {
        CheckListFacadeREST checkListFacadeREST = new CheckListFacadeREST();
        Asset asset = this.find(entity.getAssetId());
        CheckList checkList = checkListFacadeREST.find(checkListId);
        if (asset != null && checkList != null) {
            if (!asset.getCheckListList().contains(checkList)) {
                asset.getCheckListList().add(checkList);
                this.edit(asset);
            }
            if (!checkList.getAssetList().contains(asset)) {
                checkList.getAssetList().add(asset);
                checkListFacadeREST.edit(checkList);
            }
        }
    }

    @PUT
    @Path("unlinkCheckList/{checkListId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCheckList(@PathParam("checkListId") String checkListId, Asset entity) {
        CheckListFacadeREST checkListFacadeREST = new CheckListFacadeREST();
        Asset asset = this.find(entity.getAssetId());
        CheckList checkList = checkListFacadeREST.find(checkListId);
        if (asset != null && checkList != null) {
            if (asset.getCheckListList().contains(checkList)) {
                asset.getCheckListList().remove(checkList);
                this.edit(asset);
            }
            if (checkList.getAssetList().contains(asset)) {
                checkList.getAssetList().remove(asset);
                checkListFacadeREST.edit(checkList);
            }
        }
    }

    private void fixParentLocation(Location parent, List<Location> locations) {
        for (Location location : locations) {
            location.setParent(parent);
            fixParentLocation(location, location.getLocationList());
        }
    }

    private void fixLocationList(Asset asset, List<Location> locations) {
        for (Location location : locations) {
            location.setAsset(asset);
            fixParentLocation(location, location.getLocationList());
        }
    }

    private void fixEquipmentList(Asset asset, List<Equipment> equipments) {
        for (Equipment equipment : equipments) {
            equipment.setAsset(asset);
            //fixParentLocation(location, location.getLocationList());
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Asset entity) {
        Asset asset = find(id);
        asset.setDeleted(entity.isDeleted());
        asset.setAddress(entity.getAddress());
        asset.setPlace(entity.getPlace());
        asset.setAssetNo(entity.getAssetNo());
        asset.setBnr(entity.getBnr());
        asset.setBuildingId(entity.getBuildingId());
        asset.setCommuneNumber(entity.getCommuneNumber());
        asset.setGnr(entity.getGnr());
        asset.setLatitude(entity.getLatitude());
        asset.setLongitude(entity.getLongitude());
        asset.setName(entity.getName());
        asset.setSection(entity.getSection());
        asset.setEquipmentList(entity.getEquipmentList());
        asset.setZoomLevel(entity.getZoomLevel());
        fixLocationList(asset, entity.getLocationList());
        asset.setLocationList(entity.getLocationList());
        fixEquipmentList(asset, entity.getEquipmentList());
        asset.setEquipmentList(entity.getEquipmentList());
        asset.setIntegrationList(entity.getIntegrationList());
        super.edit(asset);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("searchMatrikkelen/{searchString}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> searchMatrikkelen(@PathParam("searchString") String searchString) {
        List<Asset> assetList = new ArrayList<>();
        //searchString = searchString.replaceAll("\\+", "%20");
        Asset assetXY = new Asset(UUID.randomUUID().toString());
        assetXY.setAddress("Lyngneset 9"); // Kiwi Sagvåg
        assetXY.setName("Kiwi Sagvåg"); // Kiwi Sagvåg
        assetXY.setLatitude(59.775240);
        assetXY.setLongitude(5.392986);
        assetXY.setGnr(56);
        assetXY.setBnr(118);
        assetXY.setSection(1);
        assetXY.setCommuneNumber(1224);
        assetList.add(assetXY);

        Asset assetSpv = new Asset(UUID.randomUUID().toString());
        assetSpv.setAddress("Borggata 8"); // Sparebanken vest

        assetSpv.setLatitude(59.781175);
        assetSpv.setLongitude(5.501132);
        assetXY.setGnr(58);
        assetXY.setBnr(127);
        assetXY.setSection(1);
        assetXY.setCommuneNumber(1224);
        assetList.add(assetSpv);

        return assetList;
    }

    @GET
    @Path("loadOptimized/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Asset loadOptimized(@PathParam("id") String id) {
        Asset asset = findNative(id);
        //asset.getEquipmentList().clear();
        asset.setEquipmentList(new ArrayList<>());
        return  asset;
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Asset find(@PathParam("id") String id) {
        return findNative(id);
    }

    private Asset findNative(String id) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT "
                        + "* FROM asset c\n"
                        + "WHERE c.asset_id = ?1",
                Asset.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (resultList.isEmpty()) {
            return null;
        } else {
            for(Asset asset: resultList) {
                if(asset.getAssetGroup() != null) {
                    asset.setAssetGroupId(asset.getAssetGroup().getAssetGroupId());
                }
            }
            return resultList.get(0);
        }
    }

    private Asset findNative2(String id) {
        Asset asset = null;
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT "
                        + "* FROM asset a\n"
                        + "WHERE a.asset_id = ?1",
                Asset.class)
                .setParameter(1, id)
                .getResultList();
        em.close();
        if (resultList.isEmpty()) {
            return null;
        } else {
            return resultList.get(0);
        }
    }

    @GET
    @Path("loadByAuthority/{ownerId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByAuthority(@PathParam("ownerId") String ownerId, @PathParam("authorityId") String authorityId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT asset.* \n"
                        + " FROM asset asset\n "
                        + " join asset_group ag on ag.asset_group_id = asset.asset_group\n "
                        + " join company_has_asset_group chag on chag.asset_group_asset_group_id = ag.asset_group_id\n"
                        + " join company_has_asset cha on cha.asset_asset_id = asset.asset_id\n"
                        + " WHERE chag.company_company_id = ?1 and cha.company_company_id =?2",
                Asset.class)
                .setParameter(1, ownerId)
                .setParameter(2, authorityId)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadByAuthorityOptimized/{ownerId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByAuthorityOptimized(@PathParam("ownerId") String ownerId, @PathParam("authorityId") String authorityId) {

        List<Asset> resultList = loadByAuthority(ownerId,authorityId);
        for(Asset asset: resultList) {
            asset.setEquipmentList(new ArrayList<>());
            asset.setLocationList(new ArrayList<>());
        }
        return resultList;
    }

    //    select * from asset asset
//    join asset_group ag on ag.asset_group_id = asset.asset_group
//    join company_has_asset_group chag on chag.asset_group_asset_group_id = ag.asset_group_id
//    join company_has_asset cha on cha.asset_asset_id = asset.asset_id
//    where chag.company_company_id = "606bf8e9-3768-498a-86a2-e73045eacce6" and cha.company_company_id = "E07121A7-024A-4D0E-8B58-A064F0BC4A22"
    @GET
    @Path("loadCheckLists/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<CheckList> loadCheckLists(@PathParam("assetId") String assetId) {
        Asset asset = super.find(assetId);
        return asset.getCheckListList();
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


    @Deprecated
    @GET
    @Path("findByCompany/{companyid}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> findByCompany(@PathParam("companyid") String companyId) {
        CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
        Company company = companyFacadeREST.find(companyId);
        List<Asset> assets = company.getAssetList();
        return assets;
    }

    @GET
    @Path("loadByOwner/{ownerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByOwner(@PathParam("ownerId") String ownerId) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
//        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT asset.* \n"
//                        + " FROM asset asset\n "
//                        + " join asset_group ag on ag.asset_group_id = asset.asset_group\n "
//                        + " join company_has_asset_group chag on chag.asset_group_asset_group_id = ag.asset_group_id\n"
//                        + " WHERE chag.company_company_id = ?1",
//                Asset.class)
//                .setParameter(1, ownerId)
//                .getResultList();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT asset.* \n"
                        + " FROM asset asset\n "
                        + " join company_has_asset cha on cha.asset_asset_id = asset.asset_id\n "
                        + " WHERE cha.company_company_id = ?1",
                Asset.class)
                .setParameter(1, ownerId)
                .getResultList();
        em.close();
        for(Asset asset: resultList) {
            asset.setEquipmentList(new ArrayList<>());
            asset.setLocationList(new ArrayList<>());
        }
        return resultList;
    }

    @PUT
    @Path("loadByIntegrations/{service}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByIntegrations(@PathParam("service") String service, List<String> primaryKeys) {

        String assetIdsString = "(";
        for (String assetId : primaryKeys) {
            assetIdsString += "'" + assetId + "',";
        }
        assetIdsString = assetIdsString.substring(0, assetIdsString.length() - 1);
        assetIdsString += ")";

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT "
                                + "* FROM asset a\n"
                                + "JOIN asset_has_integration ahi on ahi.asset_asset_id = a.asset_id \n"
                                + "JOIN integration i on i.integration_id = ahi.integration_integration_id \n"
                                + "WHERE i.keyy = 'PRIMARY_KEY' AND  i.service = ?1 AND i.valuee IN " + assetIdsString + "\n",
                        Asset.class)
                .setParameter(1, service)
                .setParameter(2, assetIdsString)
                .getResultList();
        em.close();
        return resultList;
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByIntegration(@PathParam("service") String service,@PathParam("primaryKey") String primaryKey) {

        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List<Asset> resultList = (List<Asset>) em.createNativeQuery("SELECT "
                                + "* FROM asset a\n"
                                + "JOIN asset_has_integration ahi on ahi.asset_asset_id = a.asset_id \n"
                                + "JOIN integration i on i.integration_id = chi.integration_integration_id \n"
                                + "WHERE i.keyy = 'PRIMARY_KEY' AND  i.service = ?1 AND i.valuee = ?2",
                        Company.class)
                .setParameter(1, service)
                .setParameter(2, primaryKey)
                .getResultList();
        em.close();
        return resultList;
    }

}
