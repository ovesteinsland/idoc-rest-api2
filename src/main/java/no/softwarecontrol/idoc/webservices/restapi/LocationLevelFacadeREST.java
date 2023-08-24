/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.AssetType;
import no.softwarecontrol.idoc.data.entityobject.LocationLevel;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.locationlevel")
@RolesAllowed({"ApplicationRole"})
public class LocationLevelFacadeREST extends AbstractFacade<LocationLevel> {

    public LocationLevelFacadeREST() {
        super(LocationLevel.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "LocationLevel.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void create(LocationLevel entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, LocationLevel entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @PUT
    @Path("linkToAssetType/{assetTypeId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToAssetType(@PathParam("assetTypeId") String assetTypeId, LocationLevel entity) {
        AssetTypeFacadeREST assetTypeFacadeREST = new AssetTypeFacadeREST();
        LocationLevel locationLevel = this.find(entity.getLocationLevelId());
        AssetType assetType = assetTypeFacadeREST.find(assetTypeId);
        if (assetType != null && locationLevel != null) {
            if (!assetType.getLocationLevelList().contains(locationLevel)) {
                assetType.getLocationLevelList().add(locationLevel);
                assetTypeFacadeREST.edit(assetType);
            }
            locationLevel.setAssetType(assetType);
            this.edit(locationLevel);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public LocationLevel find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LocationLevel> findAll() {
        return super.findAll();
    }

    @GET
    @Path("{from}/{to}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<LocationLevel> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

}
