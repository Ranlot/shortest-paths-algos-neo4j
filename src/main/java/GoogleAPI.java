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
        } catch (URISyntaxException e) {
            e.printStackTrace();
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
        //take only the first element of the response assuming this is indeed the correct city
        //as there may be multiple cities with the same name
        return jsonObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
    }



    public static <R> void main(String[] args) throws IOException, URISyntaxException {

        CloseableHttpClient client = HttpClients.createDefault();

        List<String> listOfCities = asList("Boston", "New-York", "Detroit");

        //make multi-threaded requests
        JSONObject pos1 = getLatLong(client, "Boston");
        JSONObject pos2 = getLatLong(client, "New-York");
        JSONObject pos3 = getLatLong(client, "Detroit");
        JSONObject pos4 = getLatLong(client, "Chicago");
        JSONObject pos5 = getLatLong(client, "Saint-Paul+MN");
        JSONObject pos6 = getLatLong(client, "Little-Rock+AR");

        System.out.println(pos1.get("lat") + "  " + pos1.get("lng"));
        System.out.println(pos2.get("lat") + "  " + pos2.get("lng"));
        System.out.println(pos3.get("lat") + "  " + pos3.get("lng"));
        System.out.println(pos4.get("lat") + "  " + pos4.get("lng"));
        System.out.println(pos5.get("lat") + "  " + pos5.get("lng"));
        System.out.println(pos6.get("lat") + "  " + pos6.get("lng"));

        double dis1 = Utils.geoDistance((Double) pos1.get("lat"), (Double) pos1.get("lng"), (Double) pos2.get("lat"), (Double) pos2.get("lng"), 'K');

        System.out.println(dis1);

        client.close();

    }
}
