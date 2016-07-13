import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.GeoEstimateEvaluator;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class EmbeddedNeo4j {

    private static final List<String> LIST_OF_CITIES = asList(
            "Boston",
            "New-York",
            "Detroit",
            "Miami",
            "Los-Angeles",
            "San-Francisco",
            "Las-Vegas",
            "Seattle");

    private static final String DB_PATH = "target/neo4j-hello-db";

    private enum NodeTypes implements Label {
        CITY
    }

    private enum RelTypes implements RelationshipType {
        CONNECTED_TO
    }

    private static double getDistanceUsingNeo4jReflection(Method reflectedMethod, GeoEstimateEvaluator geoEstimateEvaluator,
                                                          double lat1,
                                                          double lng1,
                                                          double lat2,
                                                          double lng2) throws InvocationTargetException, IllegalAccessException {
        double distance = (double) reflectedMethod.invoke(geoEstimateEvaluator, lat1, lng1, lat2, lng2);
        return distance;
    }


    /*public static final BiFunction<GeoEstimateEvaluator, Method, Function> makeDis = new BiFunction<GeoEstimateEvaluator, Method, Double>() {
        @Override
        public Double apply(GeoEstimateEvaluator geoEstimateEvaluator, Method method) {
            return null;
        }
    };*/

    private void createRelationships(GraphDatabaseService graphDb, GeoEstimateEvaluator geoEstimateEvaluator, Method method, String... cityNames) throws InvocationTargetException, IllegalAccessException {
        for (int i = 0; i < cityNames.length - 1; i++) {
            Node firstNode = graphDb.findNode(NodeTypes.CITY, "name", cityNames[i]);
            Node secondNode = graphDb.findNode(NodeTypes.CITY, "name", cityNames[i + 1]);
            Relationship relationship = firstNode.createRelationshipTo(secondNode, RelTypes.CONNECTED_TO);

            double cityDistance = Utils.geoDistance(
                    (Double) firstNode.getProperty("lat"),
                    (Double) firstNode.getProperty("lng"),
                    (Double) secondNode.getProperty("lat"),
                    (Double) secondNode.getProperty("lng"),
                    'K');


            double distance2 = getDistanceUsingNeo4jReflection(method, geoEstimateEvaluator, (Double) firstNode.getProperty("lat"), (Double) firstNode.getProperty("lng"), (Double) secondNode.getProperty("lat"), (Double) secondNode.getProperty("lng"));

            System.out.println(cityDistance + " ; " + distance2 / 1000.);

            relationship.setProperty("distance", cityDistance);
            /*System.out.println(firstNode.getAllProperties());
            System.out.println(secondNode.getAllProperties());*/
        }
    }

    public static void main(final String[] args) throws IOException, URISyntaxException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //https://github.com/neo4j/neo4j/tree/2.2.2/community/embedded-examples/src/main/java/org/neo4j/examples

        //https://searchcode.com/codesearch/view/15572561/

        //https://keyholesoftware.com/2013/01/28/mapping-shortest-routes-using-a-graph-database/

        //https://maxdemarzi.com/2015/09/04/flight-search-with-the-neo4j-traversal-api/

        FileUtils.deleteRecursively(new File(DB_PATH));

        CloseableHttpClient client = HttpClients.createDefault();
        Map<String, JSONObject> cityInformation = LIST_OF_CITIES.stream().collect(Collectors.toMap(city -> city, city -> GoogleAPI.getLatLong(client, city)));
        client.close();

        EmbeddedNeo4j hello = new EmbeddedNeo4j();

        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        hello.registerShutdownHook(graphDb);

        hello.fillDB(graphDb, cityInformation);

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

        hello.removeData(graphDb);
        hello.shutDown(graphDb);

    }

    private static void createNode(GraphDatabaseService graphDb, String cityName, JSONObject cityData) {
        Node node = graphDb.createNode(NodeTypes.CITY);
        node.setProperty("name", cityName);
        node.setProperty("lat", cityData.get("lat"));
        node.setProperty("lng", cityData.get("lng"));
    }


    private void fillDB(GraphDatabaseService graphDb, Map<String, JSONObject> cityInformation) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        GeoEstimateEvaluator geoEstimateEvaluator = new GeoEstimateEvaluator("latKey", "lngKey");
        Method method = geoEstimateEvaluator.getClass().getDeclaredMethod("distance", double.class, double.class, double.class, double.class);
        method.setAccessible(true);

        try (Transaction tx = graphDb.beginTx()) {

            //System.out.println("hi");

            cityInformation.keySet().stream().forEach(city -> createNode(graphDb, city, cityInformation.get(city)));

            createRelationships(graphDb, geoEstimateEvaluator, method, "Boston", "Detroit", "New-York");
            createRelationships(graphDb, geoEstimateEvaluator, method, "Boston", "New-York", "Las-Vegas", "Seattle");
            createRelationships(graphDb, geoEstimateEvaluator, method, "Detroit", "Miami", "Los-Angeles");
            createRelationships(graphDb, geoEstimateEvaluator, method, "Detroit", "San-Francisco", "Los-Angeles");
            createRelationships(graphDb, geoEstimateEvaluator, method, "Detroit", "Las-Vegas", "Los-Angeles");

            //Relationship relationship = firstNode.createRelationshipTo(secondNode, RelTypes.CONNECTED_TO);
            //relationship.setProperty("distance", 152);

            tx.success();
        }
    }

    private void removeData(GraphDatabaseService graphDb) {
        try (Transaction tx = graphDb.beginTx()) {

            //delete the relationships before the nodes
            ResourceIterable<Relationship> allRelationships = graphDb.getAllRelationships();
            List<Relationship> lstAllRelationships = allRelationships.stream().collect(Collectors.toList());
            for (Relationship relationship : lstAllRelationships) {
                //System.out.println("Deleting " + relationship.getAllProperties() + " ; " + relationship.getStartNode().getProperty("name") + " ; " + relationship.getEndNode().getProperty("name"));
                relationship.delete();
            }

            ResourceIterable<Node> allNodes = graphDb.getAllNodes();
            List<Node> lstAllNodes = allNodes.stream().collect(Collectors.toList());
            for (Node node : lstAllNodes) {
                //System.out.println("Deleting " + node.getAllProperties());
                node.delete();
            }

            tx.success();
        }
    }

    private void shutDown(GraphDatabaseService graphDb) {
        System.out.println();
        System.out.println("Shutting down database ...");
        graphDb.shutdown();
    }

    // Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits (even if you "Ctrl-C" the running application).
    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        //System.out.println("registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }


}