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

/**
 * The default implementation of the in-context classloader runner
 */
public class InContextRunnerImpl implements InContextRunner {

    private static final Logger logger = LoggerFactory.getLogger(InContextRunnerImpl.class);

    DatabaseConfiguration databaseConfiguration;
    Connection connection;
    File jahiaInstallLocationFile;
    ClassLoader classLoader;

    public boolean run(File jahiaInstallLocationFile, File scriptFile, ClassLoader classLoader) {
        this.jahiaInstallLocationFile = jahiaInstallLocationFile;
        this.classLoader = classLoader;
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        initialize();
        runScript(scriptFile);
        Thread.currentThread().setContextClassLoader(classLoader);
        return true;
    }

    public void initialize() {
        loadDatabaseConfiguration();
        getJDBCConnection(databaseConfiguration);
    }

    public void runScript(File scriptFile) {
        // create a script engine manager
        ScriptEngineManager factory = new ScriptEngineManager();
        int lastDotPos = scriptFile.getName().lastIndexOf(".");
        String extension = null;
        if (lastDotPos > -1) {
            extension = scriptFile.getName().substring(lastDotPos+1);
        }
        ScriptEngine engine = factory.getEngineByExtension(extension);
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("jdbcConnection", connection);
            bindings.put("jahiaInstallLocationFile", jahiaInstallLocationFile);
            bindings.put("classLoader", classLoader);
            engine.eval(new FileReader(scriptFile), bindings);
        } catch (ScriptException e) {
            logger.error("Error executing script " + scriptFile, e);
        } catch (FileNotFoundException e) {
            logger.error("Error executing script " + scriptFile, e);
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
