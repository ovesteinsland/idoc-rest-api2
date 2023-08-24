/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.web.images;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author ovesteinsland
 */
@WebServlet(name = "FileDownloadServlet2", urlPatterns = {"/download2"})
@RolesAllowed({"ApplicationRole"})
public class FileDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String file = request.getParameter("URL");
        try {
            if (file.endsWith(".jpg")) {
                response.setContentType("image/jpeg");
            } else if (file.endsWith(".png")) {
                response.setContentType("image/png");
            }

            String pathToWeb = ImageResource.getImageUrl() + File.separator;
            File f = new File(pathToWeb + file);
            BufferedImage bi = ImageIO.read(f);
            OutputStream out = response.getOutputStream();
            if (file.endsWith(".jpg")) {
                ImageIO.write(bi, "jpg", out);
            } else if (file.endsWith(".png")) {
                ImageIO.write(bi, "png", out);
            }
            out.close();
        } catch (IOException ioe) {

        }
    }
}
