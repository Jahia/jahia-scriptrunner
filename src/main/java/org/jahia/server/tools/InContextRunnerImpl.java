package org.jahia.server.tools;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The default implementation of the in-context classloader runner
 */
public class InContextRunnerImpl implements InContextRunner {

    private static final Logger logger = LoggerFactory.getLogger(InContextRunnerImpl.class);

    DatabaseConfiguration databaseConfiguration;
    Connection connection;
    File jahiaInstallLocationFile;

    public boolean run(File jahiaInstallLocationFile) {
        this.jahiaInstallLocationFile = jahiaInstallLocationFile;
        initialize();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void initialize() {
        loadDatabaseConfiguration();
        getJDBCConnection(databaseConfiguration);
    }

    public void runScripts() {

    }

    public void loadDatabaseConfiguration() {
        SAXBuilder saxBuilder = new SAXBuilder();
        InputStreamReader fileReader = null;
        try {
            fileReader = new InputStreamReader(new FileInputStream(new File(jahiaInstallLocationFile, "META-INF/context.xml")));
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();

            Element resource = (Element) XPath.newInstance("/Context/Resource").selectSingleNode(root);
            databaseConfiguration = new DatabaseConfiguration(resource.getAttributeValue("driverClassName"),
                    resource.getAttributeValue("url"),
                    resource.getAttributeValue("username"),
                    resource.getAttributeValue("password"));
        } catch (FileNotFoundException e) {
            logger.error("Error loading database configuration", e);
        } catch (JDOMException e) {
            logger.error("Error loading database configuration", e);
        } catch (IOException e) {
            logger.error("Error loading database configuration", e);
        }
    };

    public void getJDBCConnection(DatabaseConfiguration databaseConfiguration) {
        try {
            Class.forName(databaseConfiguration.getDriverName());
            connection = DriverManager.getConnection(databaseConfiguration.getConnectionURL(), databaseConfiguration.getUserName(), databaseConfiguration.getPassword());
        } catch (ClassNotFoundException e) {
            logger.error("Error loading database driver" + databaseConfiguration.getDriverName(), e);
        } catch (SQLException e) {
            logger.error("Error connecting to the database", e);
        }
    }

}
