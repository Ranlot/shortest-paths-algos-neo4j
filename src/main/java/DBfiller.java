import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Map;
import java.util.function.BiFunction;

public class DBfiller {

    private GraphDatabaseService graphDb;
    private BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator;

    public DBfiller(GraphDatabaseService graphDb, BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator) {
        this.graphDb = graphDb;
        this.distanceCalculator = distanceCalculator;
    }

    private void relationshipCreater(CityConnector connectionLine) {

        for (int i = 0; i < connectionLine.getNumberOfCities() - 1; i++) {

            Node firstNode = graphDb.findNode(ShortestPath.NodeTypes.CITY, "name", connectionLine.getCity(i));
            Node secondNode = graphDb.findNode(ShortestPath.NodeTypes.CITY, "name", connectionLine.getCity(i + 1));
            Relationship relationship = firstNode.createRelationshipTo(secondNode, ShortestPath.RelTypes.CONNECTED_TO);

            GeoLocation location1 = new GeoLocation((Double) firstNode.getProperty("lat"), (Double) firstNode.getProperty("lng"));
            GeoLocation location2 = new GeoLocation((Double) secondNode.getProperty("lat"), (Double) secondNode.getProperty("lng"));

            double cityDistance = distanceCalculator.apply(location1, location2);

            relationship.setProperty("distance", cityDistance);

            System.out.printf("%s ; %s ; %.2f km\n", relationship.getStartNode().getProperty("name"), relationship.getEndNode().getProperty("name"), (Double) relationship.getProperty("distance"));

        }
    }

    private void createNode(String cityName, JSONObject cityData) {
        Node node = graphDb.createNode(ShortestPath.NodeTypes.CITY);
        node.setProperty("name", cityName);
        node.setProperty("lat", cityData.get("lat"));
        node.setProperty("lng", cityData.get("lng"));
    }

    public void createAllNodes(Map<String, JSONObject> cityInformation) {
        try (Transaction tx = graphDb.beginTx()) {
            cityInformation.keySet().stream().forEach(city -> createNode(city, cityInformation.get(city)));
            tx.success();
        }
    }

    public void createRelationship(CityConnector cityConnector) {
        try (Transaction tx = graphDb.beginTx()) {
            relationshipCreater(cityConnector);
            tx.success();
        }
    }

}
