package org.jahia.server.tools.scriptrunner.engines.common;

import org.jahia.server.tools.scriptrunner.common.InContextRunner;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.*;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The default implementation of the in-context classloader runner
 */
public class InContextRunnerImpl implements InContextRunner {

    private static final Logger logger = LoggerFactory.getLogger(InContextRunnerImpl.class);

    DatabaseConfiguration databaseConfiguration;
    Connection connection;
    File jahiaInstallLocationFile;
    ClassLoader classLoader;
    Properties scriptOptions;

    public boolean run(File jahiaInstallLocationFile, String scriptName, InputStream scriptStream, Properties scriptOptions, ClassLoader classLoader) {
        this.jahiaInstallLocationFile = jahiaInstallLocationFile;
        this.classLoader = classLoader;
        this.scriptOptions = scriptOptions;
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        initialize();
        runScript(scriptName, scriptStream);
        Thread.currentThread().setContextClassLoader(classLoader);
        return true;
    }

    public void initialize() {
        loadDatabaseConfiguration();
        getJDBCConnection(databaseConfiguration);
    }

    public void runScript(String scriptName, InputStream scriptStream) {
        // create a script engine manager
        ScriptEngineManager factory = new ScriptEngineManager();
        int lastDotPos = scriptName.lastIndexOf(".");
        String extension = null;
        if (lastDotPos > -1) {
            extension = scriptName.substring(lastDotPos+1);
        }
        ScriptEngine engine = factory.getEngineByExtension(extension);
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("jdbcConnection", connection);
            bindings.put("jahiaInstallLocationFile", jahiaInstallLocationFile);
            bindings.put("classLoader", classLoader);
            bindings.put("scriptOptions", scriptOptions);
            bindings.put("databaseConfiguration", databaseConfiguration);
            engine.eval(new InputStreamReader(scriptStream), bindings);
        } catch (ScriptException e) {
            logger.error("Error executing script " + scriptName, e);
        }

    }

    public void loadDatabaseConfiguration() {
        SAXBuilder saxBuilder = new SAXBuilder();
        InputStreamReader fileReader = null;
        try {
            fileReader = new InputStreamReader(new FileInputStream(new File(jahiaInstallLocationFile, "META-INF"+File.separator+"context.xml")));
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element root = jdomDocument.getRootElement();

            Element resource = (Element) XPath.newInstance("/Context/Resource").selectSingleNode(root);
            databaseConfiguration = new DatabaseConfiguration(resource.getAttributeValue("driverClassName"),
                    resource.getAttributeValue("url"),
                    resource.getAttributeValue("username"),
                    resource.getAttributeValue("password"));

            System.setProperty("derby.system.home", new File(jahiaInstallLocationFile, "WEB-INF" + File.separator + "var" + File.separator+ "dbdata").getAbsolutePath());
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
            Driver driver = (Driver) Class.forName(databaseConfiguration.getDriverName(), true, classLoader).newInstance();
            DriverManager.registerDriver(driver);

            connection = DriverManager.getConnection(databaseConfiguration.getConnectionURL(), databaseConfiguration.getUserName(), databaseConfiguration.getPassword());
            connection.setAutoCommit(false);
            logger.info("Connection to database established using driver="+databaseConfiguration.getDriverName()+" url=" + databaseConfiguration.getConnectionURL() + " and user=" + databaseConfiguration.getUserName());
        } catch (ClassNotFoundException e) {
            logger.error("Error loading database driver " + databaseConfiguration.getDriverName(), e);
        } catch (SQLException e) {
            logger.error("Error connecting to the database", e);
        } catch (InstantiationException e) {
            logger.error("Error loading database driver" + databaseConfiguration.getDriverName(), e);
        } catch (IllegalAccessException e) {
            logger.error("Error loading database driver" + databaseConfiguration.getDriverName(), e);
        }
    }

}
