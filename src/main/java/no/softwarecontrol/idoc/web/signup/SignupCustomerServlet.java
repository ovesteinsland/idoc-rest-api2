package no.softwarecontrol.idoc.web.signup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.softwarecontrol.idoc.data.entityhelper.CustomerData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "SignupCustomerServlet", urlPatterns = {"/signup"}, asyncSupported = true)
public class SignupCustomerServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        //final String projectId = request.getParameter("projectId");

        StringBuilder sb = new StringBuilder();
        final ServletInputStream inputStream = request.getInputStream();
        int total = 0;
        int count;

        byte[] b=new byte[request.getContentLength()];
        while ((count=inputStream.read(b)) != -1) {
            total+=count;
        }
        String str = new String(b, StandardCharsets.UTF_8);
        CustomerData customerData = parseCustomerData(str);
        createCustomer(request,response,customerData);

        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout(10 * 60 * 1000);
        if(customerData != null) {
            SignupDispatcher.addRemoteClient(new SignupClient(asyncContext, customerData));
        }
    }

    private void createCustomer(HttpServletRequest request, HttpServletResponse response, CustomerData customerData){

    }

    private static CustomerData parseCustomerData(String jsonString)
    {
        try{
            Gson gson = new GsonBuilder().create();
            CustomerData customerData = gson.fromJson(jsonString, CustomerData.class);
            return customerData;
        }
        catch(JsonSyntaxException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

}
