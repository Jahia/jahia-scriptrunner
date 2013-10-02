package org.jahia.server.tools.scriptrunner.engines.common;

/**
 */
public class DatabaseConfiguration {

    private String driverClassName;
    private String connectionURL;
    private String userName;
    private String password;
    private String schema;

    // Empty constructor to use as a JavaBean by commons-beanutils
    public DatabaseConfiguration() {
    }

    public DatabaseConfiguration(String driverClassName, String connectionURL, String userName, String password) {
        this.driverClassName = driverClassName;
        this.connectionURL = connectionURL;
        this.userName = userName;
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
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

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
