package Main;

import FileIO.FileIOutils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
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

public class ShortestPath {

    private static final String DB_PATH = "connectedCitiesDB";

    public enum NodeTypes implements Label {
        CITY
    }

    public enum RelTypes implements RelationshipType {
        CONNECTED_TO
    }

    public static void main(final String[] args) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //https://github.com/neo4j/neo4j/tree/2.2.2/community/embedded-examples/src/main/java/org/neo4j/examples
        //https://searchcode.com/codesearch/view/15572561/
        //https://keyholesoftware.com/2013/01/28/mapping-shortest-routes-using-a-graph-database/
        //https://maxdemarzi.com/2015/09/04/flight-search-with-the-neo4j-traversal-api/

        //--------------------------------
        String fileName = "src\\main\\resources\\citySchedule.csv";

        List<CityConnector> cityConnectorArrayList = (new FileIOutils(fileName)).getAllConnections();  //get the list of possible routes
        //once we have this list, extract the list of relevant cities
        List<String> listOfCities = cityConnectorArrayList.stream().flatMap(connectedCities -> connectedCities.getConnectedCities().stream()).distinct().collect(Collectors.toList());

        /*for(Map.Entry<String, JSONObject> entry : cityInformation.entrySet()) {
            System.out.printf("%s ; %s\n", entry.getKey(), entry.getValue());
        }*/
        //--------------------------------

        FileUtils.deleteRecursively(new File(DB_PATH));
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        Neo4jDButils neo4jDButils = new Neo4jDButils(graphDb);

        CloseableHttpClient client = HttpClients.createDefault();
        MapAPI mapAPI = new MapAPI(client);
        Map<String, JSONObject> cityInformation = listOfCities.stream().collect(Collectors.toMap(city -> city, mapAPI::getLatLong));
        client.close();

        neo4jDButils.registerShutdownHook();

        fillDB(graphDb, cityInformation, cityConnectorArrayList);


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

    private static void fillDB(GraphDatabaseService graphDb, Map<String, JSONObject> cityInformation, List<CityConnector> cityConnectorArrayList) throws NoSuchMethodException {

        BiFunction<GeoLocation, GeoLocation, Double> distanceCalculator = new DistanceCalculatorFactory().getNeo4jDistanceCalculator();
        //BiFunction<Main.GeoLocation, Main.GeoLocation, Double> distanceCalculator = new Main.DistanceCalculatorFactory().getNaiveDistance();

        DBfiller dBfiller = new DBfiller(graphDb, distanceCalculator);
        dBfiller.createAllNodes(cityInformation);
        cityConnectorArrayList.stream().forEach(dBfiller::createRelationship);

    }

}