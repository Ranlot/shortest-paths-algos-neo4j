import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public class URIfactory {

    private String cityName;

    public URIfactory(String cityName) {
        this.cityName = cityName;
    }

    public URI getRelevantURI() {
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
        return uri;
    }

}
