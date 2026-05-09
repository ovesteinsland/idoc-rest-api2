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
import no.softwarecontrol.idoc.data.entityobject.Equipment;
import no.softwarecontrol.idoc.data.entityobject.Location;
import no.softwarecontrol.idoc.webservices.exception.UnsupportedMediaException;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.location")
@RolesAllowed({"ApplicationRole"})
public class LocationFacadeREST extends AbstractFacade<Location> {

    public static LocationFacadeREST instance;

    public LocationFacadeREST() {
        super(Location.class);
        instance = this;
    }

    public static LocationFacadeREST getInstance() {
        if (instance == null) {
            instance = new LocationFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery(){
        return "Location.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Location entity) {
        super.create(entity);
    }

    @POST
    @Path("createWithAsset/{assetId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAsset(@PathParam("assetId") String assetId, Location entity) {
        Location existing = this.find(entity.getLocationId());
        Asset asset = AssetFacadeREST.getInstance().find(assetId);
        entity.setAsset(asset);
        if( existing == null) {
            for(Location loc:entity.getLocationList()){
                reconnectParent(entity,loc);
            }
            super.create(entity);
            if(!asset.getLocationList().contains(entity)) {
                asset.getLocationList().add(entity);
                AssetFacadeREST.getInstance().edit(asset.getAssetId(),asset);
            }
        } else {
            edit(entity.getLocationId(),entity);
        }
    }

    @POST
    @Path("createWithLocation/{locationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithLocation(@PathParam("locationId") String locationId, Location entity) throws Exception {
        Location existing = this.find(entity.getLocationId());
        if( existing == null) {
            Location parent = this.find(locationId);
            if(parent != null){
                entity.setParent(parent);
                parent.getLocationList().add(entity);
                for(Location loc:entity.getLocationList()){
                    reconnectParent(entity,loc);
                }
                super.create(entity);
                this.edit(parent);
            }else {
                throw new UnsupportedMediaException("Parent location not yet synchronized - Throw ERROR");
            }
        } else {
            edit(entity.getLocationId(),entity);
        }
    }

    private void reconnectParent(Location parent, Location location){
        location.setParent(parent);
        for(Location child:location.getLocationList()){
            reconnectParent(location,child);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Location entity) {
        Location existing = this.find(entity.getLocationId());

        if(existing != null) {
            if (existing.getAsset() != null) {
                Asset asset = AssetFacadeREST.getInstance().find(existing.getAsset().getAssetId());
                entity.setAsset(asset);
            } else {
                if (existing.getParent() != null) {
                    Location existingParent = this.find(existing.getParent().getLocationId());
                    entity.setParent(existingParent);
                }
            }
            for (Location loc : entity.getLocationList()) {
                reconnectParent(entity, loc);
            }
            super.edit(entity);
        }
    }

    @GET
    @Path("findByAsset/{assetId}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Location> findByAsset(@PathParam("assetId") String assetId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Location> locations = (List<Location>) em.createNativeQuery("""
                            SELECT * FROM location p
                            WHERE asset = ?1
                            """,
                            Location.class)
                    .setParameter(1, assetId)
                    .getResultList();
            return locations;
        } catch (Exception e) {
            System.out.println("Exception while finding locations for assetId: " + assetId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find locations by asset", e);
        }
    }


    @GET
    @Path("findByAssetFlatten/{assetId}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<Location> findByAssetFlatten(@PathParam("assetId") String assetId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Object[]> results = em.createNativeQuery("""
                    SELECT l1.location_id, l1.name, l2.location_id, l2.name 
                    FROM iDocDatabase.location l1
                    JOIN location l2 ON l2.parent = l1.location_id
                    WHERE l1.asset = ?1
                    """)
                    .setParameter(1, assetId)
                    .getResultList();

            List<Location> flattens = new ArrayList<>();
            Location locationL1 = null;
            String previousL1Id = null;
            for (int i = 0; i < results.size(); i++) {
                if (locationL1 == null) {
                    locationL1 = new Location();
                    locationL1.setLocationId((String) results.get(i)[0]);
                    locationL1.setName((String) results.get(i)[1]);
                    flattens.add(locationL1);
                    previousL1Id = locationL1.getLocationId();
                }
                if (!previousL1Id.equalsIgnoreCase((String)results.get(i)[0])) {
                    locationL1 = new Location();
                    locationL1.setLocationId((String) results.get(i)[0]);
                    locationL1.setName((String) results.get(i)[1]);
                    flattens.add(locationL1);
                    previousL1Id = locationL1.getLocationId();
                }
                Location locationL2 = new Location();
                locationL2.setLocationId((String) results.get(i)[2]);
                locationL2.setName((String) results.get(i)[3]);
                flattens.add(locationL2);
            }

            return flattens;
        } catch (Exception e) {
            System.out.println("Exception while finding flattened locations for assetId: " + assetId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find flattened locations by asset", e);
        }
    }


    @POST
    @Path("synchronizeWithAsset/{assetId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void synchronizeWithAsset(@PathParam("assetId") String assetId, Location entity) {

        Asset asset = AssetFacadeREST.getInstance().find(assetId);
        for(Location loc:entity.getLocationList()){
            reconnectParent(entity,loc);
        }
        entity.setAsset(asset);
        // Check if location already exists
        Location exist = this.find(entity.getLocationId());
        if (exist == null) {
            asset.getLocationList().add(entity);
            super.create(entity);
        } else {
            super.edit(entity);
        }
        AssetFacadeREST.getInstance().edit(asset);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Location find(@PathParam("id") String id) {
        //return super.find(id);
        return findNative(id);
    }

    public Location findNative(String id) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Location> resultList = (List<Location>) em.createNativeQuery("""
                            SELECT * FROM location loc
                            WHERE loc.location_id = ?1
                            """,
                            Location.class)
                    .setParameter(1, id)
                    .getResultList();
            if (resultList.isEmpty()) {
                return null;
            } else {
                return resultList.get(0);
            }
        } catch (Exception e) {
            System.out.println("Exception while finding location by id: " + id + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to find location", e);
        }
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_JSON})
    public List<Location> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Location> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("testCharset/{testString}")
    @Consumes({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    public String testCharset(@PathParam("testString") String testString) {
        System.out.println("Blåbærsyltetøy = " + testString);
        return testString;
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

}
