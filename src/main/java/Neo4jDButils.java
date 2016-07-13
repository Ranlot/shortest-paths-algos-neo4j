import org.neo4j.graphdb.*;

import java.util.List;
import java.util.stream.Collectors;

public class Neo4jDButils {

    private GraphDatabaseService graphDb;

    public Neo4jDButils(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    // Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits (even if you "Ctrl-C" the running application).
    public void registerShutdownHook() {
        System.out.println("registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public void removeData() {
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

    public void shutDown() {
        System.out.println();
        System.out.println("Shutting down database ...");
        graphDb.shutdown();
    }

}
