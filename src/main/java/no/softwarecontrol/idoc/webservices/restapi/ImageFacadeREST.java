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
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.exception.UnsupportedMediaException;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author ovesteinsland
 */
@Stateless
@Path("no.softwarecontrol.idoc.entityobject.image")
@RolesAllowed({"ApplicationRole"})
public class ImageFacadeREST extends AbstractFacade<Media> {

    private static ImageFacadeREST instance;

    public ImageFacadeREST() {
        super(Media.class);
        instance = this;
    }

    public static ImageFacadeREST getInstance() {
        if (instance == null) {
            instance = new ImageFacadeREST();
        }
        return instance;
    }

    @Override
    protected String getSelectAllQuery() {
        return "Media.findAll";
    }

    @POST
    @Override
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(Media entity) {
        super.create(entity);
    }

    @POST
    @Path("createWithAsset/{assetId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithAsset(@PathParam("assetId") String assetId, Media entity) {
        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            Asset asset = AssetFacadeREST.getInstance().find(assetId);
            asset.getImageList().add(entity);
            entity.getAssetList().add(asset);
            AssetFacadeREST.getInstance().edit(asset);
        }
    }

    @POST
    @Path("createWithLocation/{locationId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithLocation(@PathParam("locationId") String assetId, Media entity) {

        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            Location location = LocationFacadeREST.getInstance().find(assetId);
            location.getImageList().add(entity);
            entity.getLocationList().add(location);
            LocationFacadeREST.getInstance().edit(location);
        }
    }

    @POST
    @Path("createWithProject/{projectId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithProject(@PathParam("projectId") String projectId, Media entity) {
        System.out.println("ImageFacadeREST.createWithProject: projectID = " + projectId + " imageId = " + entity.getMediaId());
        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            Project project = ProjectFacadeREST.getInstance().find(projectId);
            project.setModifiedDate(new Date());
            project.getImageList().add(entity);
            entity.getProjectList().add(project);
            ProjectFacadeREST.getInstance().editProjectOnly(project.getProjectId(), project);
        } else {

        }
        System.out.println("ImageFacadeREST.createWithProject: SUCCEEDED");
    }

    @POST
    @Path("createWithCompany/{companyId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCompany(@PathParam("companyId") String companyId, Media entity) {
        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            Company company = CompanyFacadeREST.getInstance().find(companyId);
            company.getImageList().add(entity);
            entity.getCompanyList().add(company);
            CompanyFacadeREST.getInstance().edit(company);
        }
    }

    @POST
    @Path("createWithCertificate/{certificateId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithCertificate(@PathParam("certificateId") String certificateId, Media entity) {

        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            Certificate certificate = CertificateFacadeREST.getInstance().find(certificateId);
            certificate.getImageList().add(entity);
            entity.getCertificateList().add(certificate);
            CertificateFacadeREST.getInstance().edit(certificate);
        }
        System.out.println("ImageFacadeREST.createWithCertificate: SUCCEEDED");
    }

    @POST
    @Path("createWithQuickChoiceGroup/{quickChoiceGroupId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithQuickChoiceGroup(@PathParam("quickChoiceGroupId") String quickChoiceGroupId, Media entity) {
        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            QuickChoiceGroup quickChoiceGroup = QuickChoiceGroupFacadeREST.getInstance().find(quickChoiceGroupId);
            quickChoiceGroup.getImageList().add(entity);
            entity.getQuickChoiceGroupList().add(quickChoiceGroup);
            QuickChoiceGroupFacadeREST.getInstance().edit(quickChoiceGroup);
        }
    }

    @POST
    @Path("createWithObservation/{observationId}")
    @Consumes({MediaType.APPLICATION_JSON})

    public void createWithObservation(@PathParam("observationId") String observationId, Media entity) throws Exception {
        Media media = find(entity.getMediaId());
        Observation observation = ObservationFacadeREST.getInstance().find(observationId);
        if (media == null) {
            if (observation != null) {
                super.create(entity);
                if (!observation.getImageList().contains(entity)) {
                    observation.getImageList().add(entity);
                    entity.getObservationList().add(observation);
                    ObservationFacadeREST.getInstance().edit(observation);
                    super.edit(entity);
                }
            } else {
                throw new UnsupportedMediaException("Observation for this image is not yet synchronized - Throw ERROR");
            }
        } else {
            if (observation != null) {
                if (!observation.getImageList().contains(media)) {
                    observation.getImageList().add(media);
                    media.getObservationList().add(observation);
                    try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
                        EntityTransaction tx = em.getTransaction();
                        tx.begin();
                        final int i = em.createNativeQuery(
                                        "INSERT INTO observation_has_image (observation, image)\n" +
                                                "VALUES (?, ?);"
                                ).setParameter(1, observation.getObservationId())
                                .setParameter(2, media.getMediaId())
                                .executeUpdate();
                        tx.commit();
                    } catch (Exception exp) {
                        System.out.println("Exception while inserting into observation_has_image: " + exp.getMessage());
                    }
                }
            }
        }
    }

    @POST
    @Path("createWithUser/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void createWithUser(@PathParam("userId") String userId, Media entity) {
        Media media = find(entity.getMediaId());
        if (media == null) {
            super.create(entity);
            User user = UserFacadeREST.getInstance().find(userId);
            user.getImageList().add(entity);
            entity.getUserList().add(user);
            UserFacadeREST.getInstance().edit(user);
            super.edit(entity);
        }
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Media entity) {
        Media image = find(id);
        if (image != null) {
            image.setOrderIndex(entity.getOrderIndex());
            image.setDeleted(entity.isDeleted());
            image.setUrlLarge(entity.getUrlLarge());
            image.setUrlMedium(entity.getUrlMedium());
            image.setUrlSmall(entity.getUrlSmall());
            image.setName(entity.getName());
            image.setDeleted(entity.isDeleted());
            image.setMediaPurpose(entity.getMediaPurpose());
            image.setFlirConfiguration(entity.getFlirConfiguration());
            image.setOriginalName(entity.getOriginalName());
            super.edit(image);
        }
    }

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        super.remove(super.find(id));
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Media find(@PathParam("id") String id) {
        return super.find(id);
    }

//    @GET
//    @Override
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Media> findAll() {
//        return super.findAll();
//    }
//
//    @GET
//    @Path("{from}/{to}")
//    @Produces({MediaType.APPLICATION_JSON})
//    public List<Media> findRange(@PathParam("from") Integer from, @PathParam("to") Integer to) {
//        return super.findRange(new int[]{from, to});
//    }

    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countREST() {
        return String.valueOf(super.count());
    }

    @GET
    @Path("projectimages/{projectId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Media> findProjectImages(@PathParam("projectId") String projectId) {
        Project project = ProjectFacadeREST.getInstance().find(projectId);
        List<Media> images = project.getImageList();
        return images;
    }

    @GET
    @Path("observationimages/{observationId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Media> findObservationImages(@PathParam("observationId") String observationId) {
        return findObservationImagesNative(observationId);
    }

    private List<Media> findObservationImagesJPA(String observationId) {
        Observation observation = ObservationFacadeREST.getInstance().find(observationId);
        List<Media> images = observation.getImageList();
        return images;
    }

    private List<Media> findObservationImagesNative(String observationId) {
        try (EntityManager em = LocalEntityManagerFactory.createEntityManager()) {
            List<Media> resultList = (List<Media>) em.createNativeQuery("""
                            SELECT * FROM image i
                            JOIN observation_has_image ohi
                            	ON ohi.image = i.image_id
                            WHERE ohi.observation = ?1
                            """,
                            Media.class)
                    .setParameter(1, observationId)
                    .getResultList();
            return resultList;
        } catch (Exception e) {
            System.out.println("Exception while fetching observation images for observationId: " + observationId + " - " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to fetch observation images", e);
        }
    }

}
