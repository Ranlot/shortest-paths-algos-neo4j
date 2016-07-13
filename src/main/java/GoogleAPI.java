import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static java.util.Arrays.asList;

public class GoogleAPI {

    public static JSONObject getLatLong(CloseableHttpClient closeableHttpClient, String cityName) {

        URI uri = null;

        try {
            uri = new URIBuilder()
                        .setScheme("http")
                        .setHost("maps.googleapis.com")
                        .setPath("/maps/api/geocode/json")
                        .setParameter("address", cityName)
                        .setParameter("sensor", "false")
                        .build();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

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

        String jsonString = Utils.convertStreamToString(instream);
        JSONObject jsonObject = new JSONObject(jsonString);

        //take only the first element of the response assuming this is indeed the correct city as there may be multiple cities with the same name
        return jsonObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
    }

}
