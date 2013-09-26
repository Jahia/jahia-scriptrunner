package org.jahia.server.tools;

import java.sql.Connection;

/**
 * The default implementation of the in-context classloader runner
 */
public class InContextRunnerImpl implements InContextRunner {

    DatabaseConfiguration databaseConfiguration;
    Connection connection;

    public boolean run() {
        initialize();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void initialize() {
        loadDatabaseConfiguration();
        getJDBCConnection(databaseConfiguration);
    }

    public void runScripts() {

    }

    public DatabaseConfiguration loadDatabaseConfiguration() {
        return null;
    }

    public void getJDBCConnection(DatabaseConfiguration databaseConfiguration) {
    }

}
