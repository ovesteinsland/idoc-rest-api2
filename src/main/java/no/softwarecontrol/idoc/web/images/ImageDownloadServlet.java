package no.softwarecontrol.idoc.web.images;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.softwarecontrol.idoc.storage.MediaStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;


@WebServlet(name = "ImageDownloadServlet", urlPatterns = {"/downloadimage"})
//@RolesAllowed({"ApplicationRole"})
public class ImageDownloadServlet extends HttpServlet {

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

            file = "software-control/"+file;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (MediaStorage.download(file, baos)) {

                OutputStream os = response.getOutputStream();
                response.setContentLength(baos.size());
                baos.writeTo(os);
                os.flush();

                baos.close();
            }
        } catch (IOException | GeneralSecurityException ioe) {
            System.out.println("EXEPTION");
        }
    }

}
