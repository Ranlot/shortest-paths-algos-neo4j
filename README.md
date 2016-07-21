# shortest-paths-algos-neo4j

## Abstract

The purpose of this project is to provide a very basic introduction into the world of 
graph databases / analytics using the popular Neo4j as an example.

The main class of the project is `ShortestPath.java`.

## Web UI

You can load the content of the database created by the `ShortestPath::fillDB` method to the Neo4j web server simply by pointing the database 
location to `$ROOT_OF_REPOSITORY/$DB_PATH` when starting the UI.  (In our case `$DB_PATH` is `connectedCitiesDB/`).

It is then possible to get a basic visualization of the entire graph connecting all the cities together 
by executing the basic `MATCH (n) RETURN n` Cypher query.

