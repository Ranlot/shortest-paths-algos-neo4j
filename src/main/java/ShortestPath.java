import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ShortestPath {

    private static final List<String> LIST_OF_CITIES = asList(
            "Boston",
            "New-York",
            "Detroit",
            "Miami",
            "Los-Angeles",
            "San-Francisco",
            "Las-Vegas",
            "Seattle");

    private static final String DB_PATH = "target/neo4j-db";

    private enum NodeTypes implements Label {
        CITY
    }

    private enum RelTypes implements RelationshipType {
        CONNECTED_TO
    }


    private static void createRelationships(GraphDatabaseService graphDb, BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator, String... cityNames) {
        for (int i = 0; i < cityNames.length - 1; i++) {
            Node firstNode = graphDb.findNode(NodeTypes.CITY, "name", cityNames[i]);
            Node secondNode = graphDb.findNode(NodeTypes.CITY, "name", cityNames[i + 1]);
            Relationship relationship = firstNode.createRelationshipTo(secondNode, RelTypes.CONNECTED_TO);

            GeoLocation location1 = new GeoLocation((Double) firstNode.getProperty("lat"), (Double) firstNode.getProperty("lng"));
            GeoLocation location2 = new GeoLocation((Double) secondNode.getProperty("lat"), (Double) secondNode.getProperty("lng"));

            double cityDistance = distanceCalculator.apply(location1, location2);

            relationship.setProperty("distance", cityDistance);

            System.out.printf("%s ; %s ; %.2f km\n", relationship.getStartNode().getProperty("name"), relationship.getEndNode().getProperty("name"), (Double) relationship.getProperty("distance"));
        }
    }

    public static void main(final String[] args) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //https://github.com/neo4j/neo4j/tree/2.2.2/community/embedded-examples/src/main/java/org/neo4j/examples
        //https://searchcode.com/codesearch/view/15572561/
        //https://keyholesoftware.com/2013/01/28/mapping-shortest-routes-using-a-graph-database/
        //https://maxdemarzi.com/2015/09/04/flight-search-with-the-neo4j-traversal-api/

        FileUtils.deleteRecursively(new File(DB_PATH));
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        Neo4jDButils neo4jDButils = new Neo4jDButils(graphDb);

        CloseableHttpClient client = HttpClients.createDefault();
        MapAPI mapAPI = new MapAPI(client);
        Map<String, JSONObject> cityInformation = LIST_OF_CITIES.stream().collect(Collectors.toMap(city -> city, mapAPI::getLatLong));
        client.close();

        /*for(Map.Entry<String, JSONObject> entry : cityInformation.entrySet()) {
            System.out.printf("%s ; %s\n", entry.getKey(), entry.getValue());
        }*/


        neo4jDButils.registerShutdownHook();

        fillDB(graphDb, cityInformation);


        /*try (Transaction tx = graphDb.beginTx()) {

            PathFinder<Path> pathFinder = GraphAlgoFactory.shortestPath(PathExpanderBuilder.allTypesAndDirections().build(), 5);
            PathFinder<WeightedPath> pathFinder2 = GraphAlgoFactory.dijkstra(PathExpanderBuilder.allTypesAndDirections().build(), "distance");
            //Path path = pathFinder.findSinglePath(graphDb.findNode(NodeTypes.CITY, "name", "Boston"), graphDb.findNode(NodeTypes.CITY, "name", "Los-Angeles"));
            WeightedPath path = pathFinder2.findSinglePath(graphDb.findNode(NodeTypes.CITY, "name", "Boston"), graphDb.findNode(NodeTypes.CITY, "name", "Los-Angeles"));
            for(PropertyContainer prop : path) {
                System.out.println("here ;   " + prop.getAllProperties());
            }
            tx.success();
        }*/


        neo4jDButils.removeData();
        neo4jDButils.shutDown();

    }

    private static void createNode(GraphDatabaseService graphDb, String cityName, JSONObject cityData) {
        Node node = graphDb.createNode(NodeTypes.CITY);
        node.setProperty("name", cityName);
        node.setProperty("lat", cityData.get("lat"));
        node.setProperty("lng", cityData.get("lng"));
    }


    private static void fillDB(GraphDatabaseService graphDb, Map<String, JSONObject> cityInformation) throws NoSuchMethodException {

        BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator = new DistanceCalculatorFactory().getNeo4jDistanceCalculator();
        //BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator = new DistanceCalculatorFactory().getNaiveDistance();

        try (Transaction tx = graphDb.beginTx()) {

            cityInformation.keySet().stream().forEach(city -> createNode(graphDb, city, cityInformation.get(city)));

            //TODO: generalise the createRelationships with new class and currying

            createRelationships(graphDb, distanceCalculator, "Boston", "Detroit", "New-York");
            createRelationships(graphDb, distanceCalculator, "Boston", "New-York", "Las-Vegas", "Seattle");
            createRelationships(graphDb, distanceCalculator, "Detroit", "Miami", "Los-Angeles");
            createRelationships(graphDb, distanceCalculator, "Detroit", "San-Francisco", "Los-Angeles");
            createRelationships(graphDb, distanceCalculator, "Detroit", "Las-Vegas", "Los-Angeles");


            tx.success();
        }
    }

}