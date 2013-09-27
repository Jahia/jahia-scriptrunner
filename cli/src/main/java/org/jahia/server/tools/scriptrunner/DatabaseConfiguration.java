package org.jahia.server.tools.scriptrunner;

/**
 */
public class DatabaseConfiguration {

    private String driverName;
    private String connectionURL;
    private String userName;
    private String password;

    public DatabaseConfiguration(String driverName, String connectionURL, String userName, String password) {
        this.driverName = driverName;
        this.connectionURL = connectionURL;
        this.userName = userName;
        this.password = password;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
