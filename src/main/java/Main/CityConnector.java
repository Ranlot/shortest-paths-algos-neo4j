package Main;

public class CityConnector {

    private String[] connectedCities;

    public CityConnector(String... connectedCities) {
        this.connectedCities = connectedCities;
    }

    public int getNumberOfCities() {
        return connectedCities.length;
    }

    public String getCity(int index) {
        return connectedCities[index];
    }

}
