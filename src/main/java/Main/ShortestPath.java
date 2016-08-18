package Main;

import DistanceFunctions.DistanceCalculator;
import DistanceFunctions.NaiveGeometricDistance;
import DistanceFunctions.ReflectedNeo4jDistance;
import GraphProperties.NodeTypes;
import GraphProperties.RelationshipTypes;
import Utils.FileIOutils;
import Utils.Neo4jDButils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.neo4j.graphalgo.GraphAlgoFactory.dijkstra;

public class ShortestPath {

    private static final String DB_PATH = "connectedCitiesDB";
    public static final String DISTANCE_PROP = "distance";
    public static final String NODE_NAME = "name";


    public static void main(final String[] args) throws IOException, NoSuchMethodException {

        //https://github.com/neo4j/neo4j/tree/2.2.2/community/embedded-examples/src/main/java/org/neo4j/examples
        //https://searchcode.com/codesearch/view/15572561/
        //https://keyholesoftware.com/2013/01/28/mapping-shortest-routes-using-a-graph-database/
        //https://maxdemarzi.com/2015/09/04/flight-search-with-the-neo4j-traversal-api/

        //--------------------------------
        String fileName = "src\\main\\resources\\citySchedule.csv";
        boolean useNaiveDistance = false;

        //get the list of possible routes; once we have this list, extract the list of relevant cities
        List<CityConnector> cityConnectorArrayList = (new FileIOutils(fileName)).getAllConnections();
        List<String> listOfCities = cityConnectorArrayList.stream().flatMap(connectedCities -> connectedCities.getConnectedCities().stream()).distinct().collect(Collectors.toList());

        //--------------------------------

        FileUtils.deleteRecursively(new File(DB_PATH));
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        Neo4jDButils neo4jDButils = new Neo4jDButils(graphDb);

        CloseableHttpClient client = HttpClients.createDefault();
        MapAPI mapAPI = new MapAPI(client);
        Map<String, JSONObject> cityInformation = listOfCities.stream().collect(Collectors.toMap(city -> city, mapAPI::getLatLong));
        client.close();

        neo4jDButils.registerShutdownHook();

        //decide on which distance function to use
        DistanceCalculator distanceCalculator = useNaiveDistance ? new NaiveGeometricDistance() : new ReflectedNeo4jDistance();

        fillDB(graphDb, cityInformation, cityConnectorArrayList, distanceCalculator);

        try (Transaction tx = graphDb.beginTx()) {

            //TraversalDescription traversal = graphDb.traversalDescription().relationships(RelationshipTypes.GOING_TO, Direction.OUTGOING);

            PathExpanderBuilder pathExpanderBuilder = PathExpanderBuilder.empty().add(RelationshipTypes.GOING_TO, Direction.OUTGOING);
            PathExpander<Object> pathExpander = pathExpanderBuilder.build();

            PathFinder<WeightedPath> pathFinder = dijkstra(pathExpander, DISTANCE_PROP);

            //PathFinder<Path> pathFinder = allSimplePaths(pathExpander, 3);
            //Iterable<Path> paths = pathFinder.findAllPaths(graphDb.findNode(NodeTypes.CITY, "name", "Boston"), graphDb.findNode(NodeTypes.CITY, "name", "Los-Angeles"));

            Path path = pathFinder.findSinglePath(graphDb.findNode(NodeTypes.CITY, "name", "Boston"), graphDb.findNode(NodeTypes.CITY, "name", "Los-Angeles"));

            String pathString = StreamSupport.stream(path.spliterator(), false).filter(x -> x instanceof Node).map(x -> (String)((Node) x).getProperty(NODE_NAME) ).collect(Collectors.joining(" ; "));
            Double pathWeight = ((WeightedPath) path).weight();

            System.out.println(pathString + " ; " + String.format("%.2f km", pathWeight));

            tx.success();
        }

        neo4jDButils.removeData();
        neo4jDButils.shutDown();

    }

    private static void fillDB(GraphDatabaseService graphDb, Map<String, JSONObject> cityInformation, List<CityConnector> cityConnectorArrayList, DistanceCalculator distanceCalculator) {

        DBfiller dBfiller = new DBfiller(graphDb, distanceCalculator.getDistanceCalculator());
        dBfiller.createAllNodes(cityInformation);
        cityConnectorArrayList.stream().forEach(dBfiller::createRelationship);

    }

}