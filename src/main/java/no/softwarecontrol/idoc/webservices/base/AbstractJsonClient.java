package no.softwarecontrol.idoc.webservices.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AbstractJsonClient {

    public static String getJsonString(String uri){

        StringBuilder strBuf = new StringBuilder();

        HttpURLConnection conn=null;
        BufferedReader reader=null;
        try{
            //Declare the connection to weather api url
            URL url = new URL(uri);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            //conn.setRequestProperty("apikey",apiKey);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("HTTP GET Request Failed with Error code : "
                        + conn.getResponseCode());
            }

            //Read the content from the defined connection
            //Using IO Stream with Buffer raise highly the efficiency of IO
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));
            String output = null;
            while ((output = reader.readLine()) != null)
                strBuf.append(output);
        }catch(MalformedURLException e) {
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
        finally
        {
            if(reader!=null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(conn!=null)
            {
                conn.disconnect();
            }
        }

        return strBuf.toString();
    }
}
