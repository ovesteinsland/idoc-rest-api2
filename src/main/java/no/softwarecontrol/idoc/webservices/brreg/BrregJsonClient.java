package no.softwarecontrol.idoc.webservices.brreg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import no.softwarecontrol.idoc.restclient.brreg.BrregResult;
import no.softwarecontrol.idoc.restclient.brreg.Enhet;
import no.softwarecontrol.idoc.webservices.base.AbstractJsonClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BrregJsonClient extends AbstractJsonClient {

    private static final String BASE_URI = "https://data.brreg.no/enhetsregisteret/";

    public static BrregResult queryBrreg(String queryString){
        final String uri = BASE_URI + "api/enheter/?navn=" + queryString;
        String json = getJsonString(uri);
        return toResultEntity(json);
    }

    public static Enhet findOrganizationNo(String orgNo){
        final String uri = BASE_URI + "api/enheter/" + orgNo;
        String json = getJsonString(uri);
        return toDataEntity(json);
    }

    //Covert json string to class object
    private static Enhet toDataEntity(String jsonString)
    {
        try{
            Gson gson = new GsonBuilder().create();
            Enhet enhet = gson.fromJson(jsonString, Enhet.class);
            return enhet;
        }
        catch(JsonSyntaxException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    //Covert json string to class object
    private static BrregResult toResultEntity(String jsonString)
    {
        try{
            Gson gson = new GsonBuilder().create();
            BrregResult brregResult = gson.fromJson(jsonString, BrregResult.class);
            return brregResult;
        }
        catch(JsonSyntaxException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

//    //Get the weather of the specific city
//    public static String getJsonString(String uri){
//
//        StringBuilder strBuf = new StringBuilder();
//
//        HttpURLConnection conn=null;
//        BufferedReader reader=null;
//        try{
//            //Declare the connection to weather api url
//            URL url = new URL(uri);
//            conn = (HttpURLConnection)url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Accept", "application/json");
//            //conn.setRequestProperty("apikey",apiKey);
//
//            if (conn.getResponseCode() != 200) {
//                throw new RuntimeException("HTTP GET Request Failed with Error code : "
//                        + conn.getResponseCode());
//            }
//
//            //Read the content from the defined connection
//            //Using IO Stream with Buffer raise highly the efficiency of IO
//            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
//            String output = null;
//            while ((output = reader.readLine()) != null)
//                strBuf.append(output);
//        }catch(MalformedURLException e) {
//            e.printStackTrace();
//        }catch(IOException e){
//            e.printStackTrace();
//        }
//        finally
//        {
//            if(reader!=null)
//            {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if(conn!=null)
//            {
//                conn.disconnect();
//            }
//        }
//
//        return strBuf.toString();
//    }
}
