package no.softwarecontrol.idoc.web.csv;

import jakarta.ejb.EJB;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.softwarecontrol.idoc.data.entityobject.Observation;
import no.softwarecontrol.idoc.data.entityobject.Project;
import no.softwarecontrol.idoc.webservices.restapi.ProjectFacadeREST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "CsvReportDownloadServlet", urlPatterns = {"/csvreport"}, asyncSupported = true)
public class CsvReportDownloadServlet extends HttpServlet {


    // test link
    // http://localhost:8181/iDocWebServices/csvreport?projectId=AEB82EB0-2D2C-41EA-9AA9-329416D72761&companyId=DCC2CE47-71A7-4B30-831F-C7EE7B457357
    @EJB
    ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
    String paramReportType = "1";
    String path;
    String csv = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String projectId = request.getParameter("projectId");
        final String companyId = request.getParameter("companyId");
        paramReportType = request.getParameter("reportType");
        if (paramReportType == null) {
            paramReportType = "1";
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(360000);
        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent asyncEvent) throws IOException {
                // setting some response headers

                response.setHeader("Expires", "0");
                response.setHeader("Cache-Control",
                        "must-revalidate, post-check=0, pre-check=0");
                response.setHeader("Pragma", "public");
                response.setHeader("Content-Disposition", "attachment;filename=report" + projectId + ".csv");
                // setting the content type  "Content-Disposition: attachment"
                response.setContentType("text/csv");

                // write ByteArrayOutputStream to the ServletOutputStream
                OutputStream os = response.getOutputStream();

                try {
                    //response.setContentLength(baos.size());
                    response.setContentLength(csv.getBytes("UTF-8").length);
                    //String stringArray = baos.toString("UTF-8");
                    //baos.writeTo(os);

                    os.write(csv.getBytes("UTF-8"));
                    os.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    os.close();
                }
            }

            @Override
            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                System.out.println("CsvReportDownloadServlet.onTimeOut");
            }

            @Override
            public void onError(AsyncEvent asyncEvent) throws IOException {
                System.out.println("CsvReportDownloadServlet.onError");
            }

            @Override
            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                System.out.println("CsvReportDownloadServlet.onStartAsync");
            }
        });
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                Project project = projectFacadeREST.find(projectId);
                if (project != null) {
                    csv = generateCsv(project);
                    asyncContext.complete();
                }
            }
        });

    }

    private String generateCsv(Project project) {

        String csv = "";
        csv += "ID;Plassering;Beskrivelse;Kommentar;Kategori\n";
        for (Project child : project.getProjectList()) {
            List<Observation> observations = new ArrayList<>(child.getObservationList());
            Collections.sort(observations, (Observation o1, Observation o2) -> o1.getObservationNo().compareTo(o2.getObservationNo()));
            for (Observation observation : observations) {
                csv += observation.toCsvString() + "\n";
            }
        }
        List<Observation> observations = new ArrayList<>(project.getObservationList());
        Collections.sort(observations, (Observation o1, Observation o2) -> o1.getObservationNo().compareTo(o2.getObservationNo()));

        for (Observation observation : observations) {
            csv += observation.toCsvString() + "\n";
        }
        return csv;
            /*PrintStream printer = new PrintStream(baos);
            printer.println("ID;Plassering;Beskrivelse;Kommentar;Kategori");
            for(Project child:project.getProjectList()) {
                for(Observation observation:child.getObservationList()){
                    printer.println(observation.toCsvString());
                }
            }
            for(Observation observation:project.getObservationList()){
                printer.println(observation.toCsvString());
            }
            printer.close();
            baos.close();*/

    }

}
