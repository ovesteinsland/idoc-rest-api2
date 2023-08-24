/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.web.images;

import java.io.File;

/**
 *
 * @author ovesteinsland
 */
public class ImageResource {

    private static String IMAGE_URL = "/Users/ovesteinsland/uploads";
    //public static String IMAGE_URL = "/usr/local/glassfish/uploads";

    public static String getImageUrl() {
        //return "/Users/ovesteinsland/uploads/";
        return "/usr/local/glassfish/uploads/";    
    }

    public static String getServiceUrl() {
        File imagePath = new File("/Users/ovesteinsland/uploads/");
        if (imagePath.exists()) {
            //return "http://localhost:8080/iDocWebServices/";
            return "http://webservices.idoc.no/iDocWebServices/";
        } else {
            return "http://webservices.idoc.no/iDocWebServices/";
            //return "/usr/local/glassfish/uploads";
        }
    }

}
