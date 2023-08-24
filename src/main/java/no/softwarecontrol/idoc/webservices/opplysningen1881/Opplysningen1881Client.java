package no.softwarecontrol.idoc.webservices.opplysningen1881;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import no.softwarecontrol.idoc.webservices.base.AbstractJsonClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;

public class Opplysningen1881Client extends AbstractJsonClient {

    private String serviceURL = "https://services.api1881.no/";
    private String apiKey = "3c5c788e125d4185adb2b2466c70e852";

    public Opplysningen1881Result search(String queryString) {
        HttpClient httpclient = HttpClients.createDefault();
        try
        {
            URIBuilder builder = new URIBuilder(serviceURL + "search/person");

            builder.setParameter("query", queryString);
            builder.setParameter("page", "0");
            builder.setParameter("limit", "10");

            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.setHeader("Ocp-Apim-Subscription-Key", apiKey);


            // Request body
            StringEntity reqEntity = new StringEntity("{body}");
            //request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null)
            {
                String jsonString = EntityUtils.toString(entity);
                Opplysningen1881Result opplysningen1881Result = toResultEntity(jsonString);
                return opplysningen1881Result;
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public Opplysningen1881Result lookupPhoneNumber(String phoneNumber)
    {
        HttpClient httpclient = HttpClients.createDefault();

        try
        {
            URIBuilder builder = new URIBuilder(serviceURL + "lookup/phonenumber/" + phoneNumber);


            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.setHeader("Ocp-Apim-Subscription-Key", apiKey);


            // Request body
            StringEntity reqEntity = new StringEntity("{body}");
            //request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null)
            {
                String jsonString = EntityUtils.toString(entity);
                Opplysningen1881Result opplysningen1881Result = toResultEntity(jsonString);
               return opplysningen1881Result;
            } else {
                return null;
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public Opplysningen1881Result lookupId(String id)
    {
        HttpClient httpclient = HttpClients.createDefault();

        try
        {
            URIBuilder builder = new URIBuilder(serviceURL + "lookup/id/" + id);


            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.setHeader("Ocp-Apim-Subscription-Key", apiKey);


            // Request body
            StringEntity reqEntity = new StringEntity("{body}");
            //request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null)
            {
                String jsonString = EntityUtils.toString(entity);
                Opplysningen1881Result opplysningen1881Result = toResultEntity(jsonString);
                return opplysningen1881Result;
            } else {
                return null;
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }


    //Covert json string to class object
    private Opplysningen1881Result toResultEntity(String jsonString)
    {
        try{
            Gson gson = new GsonBuilder().create();
            Opplysningen1881Result result = gson.fromJson(jsonString, Opplysningen1881Result.class);
            return result;
        }
        catch(JsonSyntaxException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
}
