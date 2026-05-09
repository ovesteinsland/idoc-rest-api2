/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
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
import java.util.stream.Collectors;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.asset")
@RolesAllowed({"ApplicationRole"})
public class AssetFacadeREST extends AbstractFacade<Asset> {

    public static AssetFacadeREST instance;

    public AssetFacadeREST() {
        super(Asset.class);
        instance = this;
    }

    public static AssetFacadeREST getInstance() {
        if (instance == null) {
            instance = new AssetFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return "Asset.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Asset entity) {
        for (Equipment equipment : entity.getEquipmentList()) {
            for (Equipment child : entity.getEquipmentList()) {
                child.setParent(equipment);
            }
        }
        super.create(entity);

    }

    @POST
    @Path("createWithAssetGroup/{assetGroupId}/")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAssetGroup(@PathParam("assetGroupId") String assetGroupId, Asset asset) {
        AssetGroup assetGroup = AssetGroupFacadeREST.getInstance().find(assetGroupId);
        fixLocationList(asset, asset.getLocationList());
        fixEquipmentList(asset, asset.getEquipmentList());
        if (asset != null && assetGroup != null) {
            asset.setAssetGroup(assetGroup);
            assetGroup.getAssetList().add(asset);
            super.create(asset);
            AssetGroupFacadeREST.getInstance().edit(assetGroup);
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
        AssetGroup assetGroup = AssetGroupFacadeREST.getInstance().find(assetGroupId);
        fixLocationList(asset, asset.getLocationList());
        fixEquipmentList(asset, asset.getEquipmentList());
        if (asset != null && assetGroup != null) {
            asset.setAssetGroup(assetGroup);
            assetGroup.getAssetList().add(asset);
            super.create(asset);
            AssetGroupFacadeREST.getInstance().edit(assetGroup);

            // link to companies
            linkCompany(companyId, asset);
            linkCompany(authorityId, asset);
        }
    }

    private void saveAssetGroup(Asset asset) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.createNativeQuery(
                                "UPDATE asset SET asset_group = ? " +
                                        "WHERE asset_id = ?"
                        ).setParameter(1, asset.getAssetGroup().getAssetGroupId())
                        .setParameter(2, asset.getAssetId())
                        .executeUpdate();
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("------------------------------------------------");
                System.out.println("Exception while updating asset.asset_group: " + e.getMessage());
                System.out.println("Asset ID: " + asset.getAssetId());
                System.out.println("------------------------------------------------");
                throw new RuntimeException("Failed to update asset group", e);
            }
        } // EntityManager lukkes automatisk her
    }


    public void replaceCustomer(String oldCustomerId, String newCustomerId, Asset asset) {
        Company newCustomer = CompanyFacadeREST.getInstance().findNative(newCustomerId);
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
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // Sjekk om linken allerede eksisterer
            Query query = em.createNativeQuery(
                            "SELECT COUNT(*) FROM company_has_asset " +
                                    "WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, entity.getAssetId());

            Number counter = (Number) query.getSingleResult();

            if (counter.intValue() == 0) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    em.createNativeQuery(
                                    "INSERT INTO company_has_asset (company_company_id, asset_asset_id) " +
                                            "VALUES (?, ?)")
                            .setParameter(1, companyId)
                            .setParameter(2, entity.getAssetId())
                            .executeUpdate();
                    tx.commit();
                } catch (Exception e) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting into company_has_asset: " + e.getMessage());
                    System.out.println("Company ID: " + companyId + ", Asset ID: " + entity.getAssetId());
                    //throw new RuntimeException("Failed to link company to asset", e);
                }
            }
            // Link eksisterer allerede - ingen feilmelding nødvendig
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("linkCompanyOptimized/{companyId}/{assetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer linkCompanyOptimized(@PathParam("companyId") String companyId, @PathParam("assetId") String assetId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // Sjekk om linken allerede eksisterer
            Query query = em.createNativeQuery(
                            "SELECT COUNT(*) FROM company_has_asset " +
                                    "WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                    .setParameter(1, companyId)
                    .setParameter(2, assetId);

            Number counter = (Number) query.getSingleResult();

            if (counter.intValue() == 0) {
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    int rowsAffected = em.createNativeQuery(
                                    "INSERT INTO company_has_asset (company_company_id, asset_asset_id) " +
                                            "VALUES (?, ?)")
                            .setParameter(1, companyId)
                            .setParameter(2, assetId)
                            .executeUpdate();
                    tx.commit();
                    return rowsAffected;
                } catch (Exception e) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    System.out.println("Exception while inserting into company_has_asset: " + e.getMessage());
                    System.out.println("Company ID: " + companyId + ", Asset ID: " + assetId);
                    return 0;
                }
            }
            // Link eksisterer allerede - returner 0
            return 0;
        } // EntityManager lukkes automatisk her
    }


    @PUT
    @Path("unlinkCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCompany(@PathParam("companyId") String companyId, Asset entity) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                int deletedRows = em.createNativeQuery(
                                "DELETE FROM company_has_asset " +
                                        "WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                        .setParameter(1, companyId)
                        .setParameter(2, entity.getAssetId())
                        .executeUpdate();
                tx.commit();

                if (deletedRows == 1) {
                    System.out.println("Successfully deleted company_has_asset link: " +
                            "Company ID: " + companyId + ", Asset ID: " + entity.getAssetId());
                } else if (deletedRows == 0) {
                    System.out.println("No company_has_asset link found to delete: " +
                            "Company ID: " + companyId + ", Asset ID: " + entity.getAssetId());
                }
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while deleting from company_has_asset: " + e.getMessage());
                System.out.println("Company ID: " + companyId + ", Asset ID: " + entity.getAssetId());
                throw new RuntimeException("Failed to unlink company from asset", e);
            }
        } // EntityManager lukkes automatisk her
    }


    @GET
    @Path("unlinkCompanyOptimized/{companyId}/{assetId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Integer unlinkCompanyOptimized(@PathParam("companyId") String companyId, @PathParam("assetId") String assetId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                int deletedRows = em.createNativeQuery(
                                "DELETE FROM company_has_asset " +
                                        "WHERE company_company_id = ?1 AND asset_asset_id = ?2")
                        .setParameter(1, companyId)
                        .setParameter(2, assetId)
                        .executeUpdate();
                tx.commit();
                return deletedRows;
            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("Exception while deleting from company_has_asset: " + e.getMessage());
                System.out.println("Company ID: " + companyId + ", Asset ID: " + assetId);
                return 0;
            }
        } // EntityManager lukkes automatisk her
    }


    @PUT
    @Path("linkCheckList/{checkListId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkCheckList(@PathParam("checkListId") String checkListId, Asset entity) {
        Asset asset = this.find(entity.getAssetId());
        CheckList checkList = CheckListFacadeREST.getInstance().find(checkListId);
        if (asset != null && checkList != null) {
            if (!asset.getCheckListList().contains(checkList)) {
                asset.getCheckListList().add(checkList);
                this.edit(asset);
            }
            if (!checkList.getAssetList().contains(asset)) {
                checkList.getAssetList().add(asset);
                CheckListFacadeREST.getInstance().edit(checkList);
            }
        }
    }

    @PUT
    @Path("unlinkCheckList/{checkListId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void unlinkCheckList(@PathParam("checkListId") String checkListId, Asset entity) {
        Asset asset = this.find(entity.getAssetId());
        CheckList checkList = CheckListFacadeREST.getInstance().find(checkListId);
        if (asset != null && checkList != null) {
            if (asset.getCheckListList().contains(checkList)) {
                asset.getCheckListList().remove(checkList);
                this.edit(asset);
            }
            if (checkList.getAssetList().contains(asset)) {
                checkList.getAssetList().remove(asset);
                CheckListFacadeREST.getInstance().edit(checkList);
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
        //asset.setLocationList(new ArrayList<>());
        return asset;
    }

    @GET
    @Path("loadOptimizedTEXT/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public String loadOptimizedTEXT(@PathParam("id") String id) throws JsonProcessingException {
        Asset asset = findNative(id);
        //asset.getEquipmentList().clear();
        asset.setEquipmentList(new ArrayList<>());
        for (Media media : asset.getImageList()) {
            media.getAssetList().clear();
        }
        for (Location location : asset.getLocationList()) {
            location.setAsset(null);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule());

        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);

        String assetAsString = null;

        assetAsString = objectMapper.writeValueAsString(asset);

        //asset.setLocationList(new ArrayList<>());
        return assetAsString;
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Asset find(@PathParam("id") String id) {
        return findNative(id);
    }

    public Asset findNative(String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Asset> resultList = em.createNativeQuery(
                            "SELECT * FROM asset c " +
                                    "WHERE c.asset_id = ?1",
                            Asset.class)
                    .setParameter(1, id)
                    .getResultList();

            if (resultList.isEmpty()) {
                return null;
            }

            Asset asset = resultList.get(0);

            // Sett AssetGroup ID hvis det finnes
            if (asset.getAssetGroup() != null) {
                asset.setAssetGroupId(asset.getAssetGroup().getAssetGroupId());
            }

            // EMERGENCY-OPTIMIZING: Added for loop Dec. 4th, 2023
            // Optimaliser Equipment-listen
            for (Equipment equipment : asset.getEquipmentList()) {
                equipment.setMeasurementList(new ArrayList<>());
                equipment.setNameString(equipment.getFullName());
            }

            return asset;
        } catch (Exception e) {
            System.out.println("Exception in findNative for Asset ID: " + id);
            System.out.println("Error: " + e.getMessage());
            throw new InternalServerErrorException("Failed to find asset: " + e.getMessage(), e);
        } // EntityManager lukkes automatisk her
    }

    @GET
    @Path("loadByAuthority/{ownerId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByAuthority(@PathParam("ownerId") String ownerId, @PathParam("authorityId") String authorityId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery(
                            "SELECT asset.* " +
                                    "FROM asset asset " +
                                    "JOIN asset_group ag ON ag.asset_group_id = asset.asset_group " +
                                    "JOIN company_has_asset_group chag ON chag.asset_group_asset_group_id = ag.asset_group_id " +
                                    "JOIN company_has_asset cha ON cha.asset_asset_id = asset.asset_id " +
                                    "WHERE chag.company_company_id = ?1 AND cha.company_company_id = ?2",
                            Asset.class)
                    .setParameter(1, ownerId)
                    .setParameter(2, authorityId)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByAuthority");
            System.out.println("Owner ID: " + ownerId + ", Authority ID: " + authorityId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }
    @GET
    @Path("loadByAuthorityOptimized/{ownerId}/{authorityId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByAuthorityOptimized(@PathParam("ownerId") String ownerId, @PathParam("authorityId") String authorityId) {

        List<Asset> resultList = loadByAuthority(ownerId, authorityId);
        for (Asset asset : resultList) {
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

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Asset> findAll() {
//        return super.findAll();
//    }
//
//    @GET
//    @Path("{from}/{to}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Asset> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
//        return super.findRange(new int[]{from, to});
//    }

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
        Company company = CompanyFacadeREST.getInstance().find(companyId);
        List<Asset> assets = company.getAssetList();
        return assets;
    }

    @GET
    @Path("loadByOwner/{ownerId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByOwner(@PathParam("ownerId") String ownerId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Asset> resultList = em.createNativeQuery(
                            "SELECT asset.* " +
                                    "FROM asset asset " +
                                    "JOIN company_has_asset cha ON cha.asset_asset_id = asset.asset_id " +
                                    "WHERE cha.company_company_id = ?1",
                            Asset.class)
                    .setParameter(1, ownerId)
                    .getResultList();

            // Optimaliser resultatet ved å tømme lister
            for (Asset asset : resultList) {
                asset.setEquipmentList(new ArrayList<>());
                asset.setLocationList(new ArrayList<>());
            }

            return resultList;
        } catch (Exception e) {
            System.out.println("Exception in loadByOwner for Owner ID: " + ownerId);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

    @PUT
    @Path("loadByIntegrations/{service}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByIntegrations(@PathParam("service") String service, List<String> primaryKeys) {
        // Håndter tomme eller null lister
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            return new ArrayList<>();
        }

        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            // Bygg SQL med riktig antall placeholders
            String placeholders = primaryKeys.stream()
                    .map(k -> "?")
                    .collect(Collectors.joining(","));

            String sql = "SELECT a.* " +
                    "FROM asset a " +
                    "JOIN asset_has_integration ahi ON ahi.asset_asset_id = a.asset_id " +
                    "JOIN integration i ON i.integration_id = ahi.integration_integration_id " +
                    "WHERE i.keyy = 'PRIMARY_KEY' AND i.service = ? AND i.valuee IN (" + placeholders + ")";

            jakarta.persistence.Query query = em.createNativeQuery(sql, Asset.class);

            // Sett service som første parameter
            query.setParameter(1, service);

            // Sett alle primaryKeys som påfølgende parametere
            int paramIndex = 2;
            for (String primaryKey : primaryKeys) {
                query.setParameter(paramIndex++, primaryKey);
            }

            return query.getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByIntegrations");
            System.out.println("Service: " + service + ", Primary keys count: " + primaryKeys.size());
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @GET
    @Path("loadByIntegration/{service}/{primaryKey}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Asset> loadByIntegration(@PathParam("service") String service, @PathParam("primaryKey") String primaryKey) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            return em.createNativeQuery(
                            "SELECT a.* " +
                                    "FROM asset a " +
                                    "JOIN asset_has_integration ahi ON ahi.asset_asset_id = a.asset_id " +
                                    "JOIN integration i ON i.integration_id = ahi.integration_integration_id " +
                                    "WHERE i.keyy = 'PRIMARY_KEY' AND i.service = ?1 AND i.valuee = ?2",
                            Asset.class)
                    .setParameter(1, service)
                    .setParameter(2, primaryKey)
                    .getResultList();
        } catch (Exception e) {
            System.out.println("Exception in loadByIntegration");
            System.out.println("Service: " + service + ", Primary key: " + primaryKey);
            System.out.println("Error: " + e.getMessage());
            return new ArrayList<>();
        } // EntityManager lukkes automatisk her
    }

}
