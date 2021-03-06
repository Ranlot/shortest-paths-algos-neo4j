package Main;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class MapAPI {

    private CloseableHttpClient closeableHttpClient;

    public MapAPI(CloseableHttpClient closeableHttpClient) {
        this.closeableHttpClient = closeableHttpClient;
    }

    public JSONObject getLatLong(String cityName) {

        URI uri = new URIfactory(cityName).getRelevantURI();
        HttpGet request = new HttpGet(uri);

        CloseableHttpResponse response = null;

        try {
            response = closeableHttpClient.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpEntity entity = response.getEntity();
        InputStream instream = null;
        try {
            instream = entity.getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String jsonString = convertStreamToString(instream);
        JSONObject jsonObject = new JSONObject(jsonString);
        //take only the first element of the response assuming this is indeed the correct city
        //as there may be multiple cities with the same name
        return jsonObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

    }


    private String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
