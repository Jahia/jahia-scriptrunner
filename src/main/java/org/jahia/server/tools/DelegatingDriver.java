package org.jahia.server.tools;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Delegating JDBC driver to get around class loader issues
 */
public class DelegatingDriver implements Driver {
    private final Driver driver;

    public DelegatingDriver(Driver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver must not be null.");
        }
        this.driver = driver;
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return driver.connect(url, info);
    }

    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }
}