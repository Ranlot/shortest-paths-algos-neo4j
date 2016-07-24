package Main;

import java.util.List;

import static java.util.Arrays.asList;

public class CityConnector {

    private String[] connectedCities;

    public CityConnector(String... connectedCities) {
        this.connectedCities = connectedCities;
    }

    public List<String> getConnectedCities() {
        return asList(connectedCities);
    }

    public int getNumberOfCities() {
        return connectedCities.length;
    }

    public String getCity(int index) {
        return connectedCities[index];
    }

}
