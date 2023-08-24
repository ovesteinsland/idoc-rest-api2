package no.softwarecontrol.idoc.web.signup;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.restclient.ImageClient;
import no.softwarecontrol.idoc.webservices.restapi.ImageFacadeREST;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



/**
 *
 * @author ovesteinsland
 */
public class ImageLoader {

    private static ImageLoader instance;
    private Map<String, BufferedImage> smallMap = new HashMap<>();
    private Map<String, BufferedImage> mediumMap = new HashMap<>();
    private ImageFacadeREST imageFacadeREST = new ImageFacadeREST();
    //private Map<String, UIImage> largeMap = new HashMap<>();
    public enum ImageSize {
        SMALL, MEDIUM, LARGE
    }

    private final static double SIZE_SMALL = 256;
    private final static double SIZE_MEDIUM = 1536;
    private final static double SIZE_LARGE = 2048;

    private ImageLoader() {
    }

    //private List<ImageLoaderListener> listeners = new ArrayList<>();

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    /*public void addListener(ImageLoaderListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }*/

    public BufferedImage scaleImage(BufferedImage originalImage, ImageSize imageSize) {
        double maxSize;
        switch (imageSize) {
            case LARGE:
                maxSize = SIZE_LARGE;
                break;
            case MEDIUM:
                maxSize = SIZE_MEDIUM;
                break;
            default:
                //Small
                maxSize = SIZE_SMALL;
                break;
        }

        double aspectRatio = (float) originalImage.getWidth() / (float) originalImage.getHeight();
        double maxHeigt;
        double maxWidth;
        if (aspectRatio >= 1) {
            maxWidth = maxSize;
            maxHeigt = maxSize / aspectRatio;
        } else {
            maxHeigt = maxSize;
            maxWidth = maxSize * aspectRatio;
        }
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        int maxWidthInt = Math.round((float) maxWidth);
        int maxHeightInt = Math.round((float) maxHeigt);

        BufferedImage resizedImage = new BufferedImage(maxWidthInt, maxHeightInt, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, maxWidthInt, maxHeightInt, null);
        g.dispose();

        return resizedImage;
    }

    private String getFolderName(String authorityId) {
        String authorityPart = "authority" + authorityId;
        return authorityPart;
    }

    private void addImage(Media image, BufferedImage uiImage, String folderName, String filename, String filetype, boolean isUploadExternal) {

        String fileExtension =".jpg";
        if(filetype.equals("image/png")){
            fileExtension = ".png";
        }

        String smallFilename = filename + "_SMALL" + fileExtension;
        String mediumFilename = filename + "_MEDIUM" + fileExtension;
        String largeFilename = filename + "_LARGE" + fileExtension;

        image.setUrlSmall(folderName + "/" + smallFilename);
        image.setUrlMedium(folderName + "/" + mediumFilename);
        image.setUrlLarge(folderName + "/" + largeFilename);

        imageFacadeREST.edit(image.getMediaId(),image);

        /*ImageClient client = new ImageClient();
        client.edit_JSON(image, image.getMediaId());
        client.close();*/

    }

    public Media createImage(Observation observation, int orderIndex) {
        Media image = new Media(UUID.randomUUID().toString());
        image.setOrderIndex(orderIndex);
        observation.getImageList().add(image);
        image.getObservationList().add(observation);

        ImageClient imageClient = new ImageClient();
        imageClient.createWithObservation_JSON(image, observation.getObservationId());
        imageClient.close();
        return image;
    }

    public Media addImage(BufferedImage uiImage, Media image, Observation observation, String filetype, boolean isUploadExternal, String authorityId, int orderIndex) throws Exception {
        boolean isNewImage = false;
        if (image == null) {
            image = new Media(UUID.randomUUID().toString());
            image.setOrderIndex(orderIndex);
            //observation.getImageList().add(image);
            //image.getObservationList().add(observation);
            isNewImage = true;
        }

        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);

        addImage(image, uiImage, folderName, filename,filetype, isUploadExternal);
        if (isNewImage) {
            try {
                imageFacadeREST.createWithObservation(observation.getObservationId(),image);
            } catch (Exception exp) {
                throw exp;
            }
        } else {
            imageFacadeREST.edit(image.getMediaId(),image);
        }
        return image;
    }

    public Media addImage(BufferedImage uiImage, Company company, String filetype, boolean isUploadExternal, String authorityId) {
        boolean isNewImage = false;
        Media image;
        if (company.getImageList().isEmpty()) {
            image = new Media(UUID.randomUUID().toString());
            image.setOrderIndex(0);
            company.getImageList().add(image);
            image.getCompanyList().add(company);
            isNewImage = true;
        } else {
            image = company.getImageList().get(0);
        }
        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);

        addImage(image, uiImage, folderName, filename, filetype, isUploadExternal);
        if (isNewImage) {
            imageFacadeREST.createWithCompany(company.getCompanyId(),image);
        }
        return image;
    }

    public Media addImage(BufferedImage uiImage, Project project, String filetype, boolean isUploadExternal, String authorityId) {
        boolean isNewImage = false;
        Media image;
        if (project.getImageList().isEmpty()) {
            image = new Media(UUID.randomUUID().toString());
            project.getImageList().add(image);
            image.getProjectList().add(project);
            isNewImage = true;
        } else {
            image = project.getImageList().get(0);
        }
        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);
        addImage(image, uiImage, folderName, filename, filetype, isUploadExternal);
        if (isNewImage) {
            ImageClient imageClient = new ImageClient();
            imageClient.createWithProject_JSON(image, project.getProjectId());
            imageClient.close();
        }
        return image;
    }

    public Media addImage(BufferedImage uiImage, Asset asset, String filetype, boolean isUploadExternal, String authorityId) {
        boolean isNewImage = false;
        Media image;
        if (asset.getImageList().isEmpty()) {
            image = new Media(UUID.randomUUID().toString());
            image.setOrderIndex(0);
            asset.getImageList().add(image);
            image.getAssetList().add(asset);
            isNewImage = true;
        } else {
            image = asset.getImageList().get(0);
        }
        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);
        addImage(image, uiImage, folderName, filename, filetype, isUploadExternal);
        if (isNewImage) {
            imageFacadeREST.createWithAsset(asset.getAssetId(),image);
        }
        return image;
    }

    public Media addImage(BufferedImage uiImage, User user, String filetype, boolean isUploadExternal, String authorityId) {
        boolean isNewImage = false;
        Media image;
        if (user.getImageList().isEmpty()) {
            image = new Media(UUID.randomUUID().toString());
            user.getImageList().add(image);
            image.getUserList().add(user);
            isNewImage = true;
        } else {
            image = user.getImageList().get(0);
        }
        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);
        addImage(image, uiImage, folderName, filename, filetype, isUploadExternal);
        if (isNewImage) {
            ImageClient imageClient = new ImageClient();
            imageClient.createWithUser_JSON(image, user.getUserId());
            imageClient.close();
        }
        return image;
    }

    public Media addImage(BufferedImage uiImage, QuickChoiceGroup quickChoiceGroup, String filetype, boolean isUploadExternal, String authorityId) {
        boolean isNewImage = false;
        Media image;
        if (quickChoiceGroup.getImageList().isEmpty()) {
            image = new Media(UUID.randomUUID().toString());
            quickChoiceGroup.getImageList().add(image);
            image.getQuickChoiceGroupList().add(quickChoiceGroup);
            isNewImage = true;
        } else {
            image = quickChoiceGroup.getImageList().get(0);
        }
        String filename = "img" + image.getMediaId();
        String folderName = getFolderName(authorityId);
        addImage(image, uiImage, folderName, filename, filetype, isUploadExternal);
        if (isNewImage) {
            ImageClient imageClient = new ImageClient();
            imageClient.createWithQuickChoiceGroup_JSON(image, quickChoiceGroup.getQuickChoiceGroupId());
            imageClient.close();
        }
        return image;
    }

    public Map<String, BufferedImage> getMediumMap() {
        return mediumMap;
    }

    public Map<String, BufferedImage> getSmallMap() {
        return smallMap;
    }

}
