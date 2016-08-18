package Main;

import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Map;
import java.util.function.BiFunction;

import static GraphProperties.NodeTypes.CITY;
import static GraphProperties.RelationshipTypes.GOING_TO;
import static Main.ShortestPath.DISTANCE_PROP;
import static Main.ShortestPath.NODE_NAME;

public class DBfiller {

    private static final String LATITUDE = "lat";
    private static final String LONGITUDE = "lng";

    private GraphDatabaseService graphDb;
    private BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator;

    public DBfiller(GraphDatabaseService graphDb, BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator) {
        this.graphDb = graphDb;
        this.distanceCalculator = distanceCalculator;
    }

    private void relationshipCreater(CityConnector connectionLine) {

        for (int i = 0; i < connectionLine.getNumberOfCities() - 1; i++) {

            Node firstNode = graphDb.findNode(CITY, NODE_NAME, connectionLine.getCity(i));
            Node secondNode = graphDb.findNode(CITY, NODE_NAME, connectionLine.getCity(i + 1));
            Relationship relationship = firstNode.createRelationshipTo(secondNode, GOING_TO);

            GeoLocation location1 = new GeoLocation((Double) firstNode.getProperty(LATITUDE), (Double) firstNode.getProperty(LONGITUDE));
            GeoLocation location2 = new GeoLocation((Double) secondNode.getProperty(LATITUDE), (Double) secondNode.getProperty(LONGITUDE));

            double cityDistance = distanceCalculator.apply(location1, location2);

            relationship.setProperty(DISTANCE_PROP, cityDistance);

            System.out.printf("%s ; %s ; %.2f km\n", relationship.getStartNode().getProperty(NODE_NAME), relationship.getEndNode().getProperty(NODE_NAME), (Double) relationship.getProperty(DISTANCE_PROP));

        }
    }

    private void createNode(String cityName, JSONObject cityData) {
        Node node = graphDb.createNode(CITY);
        node.setProperty(NODE_NAME, cityName);
        node.setProperty(LATITUDE, cityData.get(LATITUDE));
        node.setProperty(LONGITUDE, cityData.get(LONGITUDE));
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
