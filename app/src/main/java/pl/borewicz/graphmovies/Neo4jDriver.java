package pl.borewicz.graphmovies;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

/**
 * Created by Piotr on 2017-05-31.
 */

public final class Neo4jDriver {
    private static volatile Neo4jDriver instance = null;
    private Driver driver;

    public static Neo4jDriver getInstance() {
        if (instance == null) {
            synchronized (Neo4jDriver.class) {
                if (instance == null) {
                    instance = new Neo4jDriver();
                }
            }
        }
        return instance;
    }
    private Neo4jDriver() {
    }

    public void InitializeConnection(String serverAddress, String username, String password)
    {
        //bolt://localhost:7687
        //neo4j
        //neo4j
        driver = GraphDatabase.driver(serverAddress, AuthTokens.basic(username, password));
    }

    public Driver getDriver()
    {
        return driver;
    }

}
