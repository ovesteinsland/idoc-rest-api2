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

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.assettype")
@RolesAllowed({"ApplicationRole"})
public class AssetTypeFacadeREST extends AbstractFacade<AssetType> {

    public AssetTypeFacadeREST() {
        super(AssetType.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "AssetType.findAll";
    }

    @POST
    @Override
    @Consumes({ MediaType.APPLICATION_JSON})
    public void create(AssetType entity) {
        super.create(entity);
    }

    @PUT
    @Path("createWithParent/{parentId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void createWithParent(@PathParam("parentId") String parentId, AssetType assetType) {
        AssetType parent = this.find(parentId);
        assetType.setParent(parent);
        parent.getAssetTypeList().add(assetType);
        this.create(assetType);
        this.edit(parent);
    }

//    @PUT
//    @Path("linkCompany/{companyId}")
//    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
//    public void linkToCompany(@PathParam("companyId") String companyId, AssetType entity) {
//        AssetType assetType = this.find(entity.getAssetTypeId());
//        Company company = companyFacadeREST.find(companyId);
//        if (assetType != null && company != null) {
//            if (!assetType.getCompanyList().contains(company)) {
//                assetType.getCompanyList().add(company);
//                this.edit(assetType);
//            }
//            if (!company.getAssetGroupList().contains(assetType)) {
//                company.getAssetGroupList().add(assetType);
//                companyFacadeREST.edit(company);
//            }
//        }
//    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, AssetType entity) {
        super.edit(entity);
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON})
    public AssetType find(@PathParam("id") String id) {
        return super.find(id);
    }

    @GET
    @Override
    @Produces({ MediaType.APPLICATION_JSON})
    public List<AssetType> findAll() {
        return super.findAll();
    }

    @GET
    @Path("findAllRoot")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<AssetType> findAllRoot() {
        List<AssetType> assetTypes = super.findAll();
        List<AssetType> roots = assetTypes.stream().filter(r -> r.getParent() == null).collect(Collectors.toList());
        return roots;
    }

    @GET
    @Path("{from}/{to}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<AssetType> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
        return super.findRange(new int[]{from, to});
    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }


}
