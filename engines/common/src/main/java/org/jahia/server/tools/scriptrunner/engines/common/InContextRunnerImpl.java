package org.jahia.server.tools.scriptrunner.engines.common;

import org.jahia.server.tools.scriptrunner.common.InContextRunner;
import org.jahia.server.tools.scriptrunner.common.ScriptRunnerConfiguration;
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

    Connection connection;
    ScriptRunnerConfiguration scriptRunnerConfiguration;
    ClassLoader classLoader;
    Properties scriptOptions;

    public boolean run(ScriptRunnerConfiguration scriptRunnerConfiguration, String scriptName, InputStream scriptStream, ClassLoader classLoader) {
        this.scriptRunnerConfiguration = scriptRunnerConfiguration;
        this.classLoader = classLoader;
        this.scriptOptions = getScriptOptions(scriptRunnerConfiguration.getScriptOptions());
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        initialize();
        runScript(scriptName, scriptStream);
        Thread.currentThread().setContextClassLoader(classLoader);
        return true;
    }

    public void initialize() {
        loadDatabaseConfiguration();
        getJDBCConnection(scriptRunnerConfiguration);
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
        if (engine == null) {
            logger.error("Couln't find a script engine for script extension ." + extension);
            return;
        }
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("jdbcConnection", connection);
            bindings.put("scriptRunnerConfiguration", scriptRunnerConfiguration);
            bindings.put("classLoader", classLoader);
            bindings.put("scriptOptions", scriptOptions);
            engine.eval(new InputStreamReader(scriptStream), bindings);
        } catch (ScriptException e) {
            logger.error("Error executing script " + scriptName, e);
        }

    }

    public void loadDatabaseConfiguration() {
        File targetDBConfigurationSource = new File(scriptRunnerConfiguration.getDbConfigurationSource());
        if (targetDBConfigurationSource.exists()) {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(new File(scriptRunnerConfiguration.getDbConfigurationSource()));
                org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
                Element root = jdomDocument.getRootElement();

                Element resource = (Element) XPath.newInstance("/Context/Resource").selectSingleNode(root);
                scriptRunnerConfiguration.setDbDriverClassName(resource.getAttributeValue("driverClassName"));
                scriptRunnerConfiguration.setDbUrl(resource.getAttributeValue("url"));
                scriptRunnerConfiguration.setDbUserName(resource.getAttributeValue("username"));
                scriptRunnerConfiguration.setDbPassword(resource.getAttributeValue("password"));

            } catch (FileNotFoundException e) {
                logger.error("Error loading database configuration", e);
            } catch (JDOMException e) {
                logger.error("Error loading database configuration", e);
            } catch (IOException e) {
                logger.error("Error loading database configuration", e);
            } finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                        // we simply ignore the close exception
                    }
                }
            }
        }

        if (System.getProperty("derby.system.home") == null &&
                scriptRunnerConfiguration.getDbUrl() != null &&
                scriptRunnerConfiguration.getDbUrl().toLowerCase().contains("derby")) {
            System.setProperty("derby.system.home", new File(scriptRunnerConfiguration.getDbDerbySystemHome()).getAbsolutePath());
            logger.info("Setting system property derby.system.home to value:" + System.getProperty("derby.system.home"));
            logger.info("If you need to initialize it to a different location please specify the value on the JVM command line with a -Dderby.system.home=PATH parameter.");
        }
    };

    public void getJDBCConnection(ScriptRunnerConfiguration scriptRunnerConfiguration) {
        if (scriptRunnerConfiguration.getDbDriverClassName() == null ||
            scriptRunnerConfiguration.getDbDriverClassName().trim().length() == 0) {
            logger.warn("No database driver class name specified, will not create database connection.");
            return;
        }
        try {
            Driver driver = (Driver) Class.forName(scriptRunnerConfiguration.getDbDriverClassName(), true, classLoader).newInstance();
            DriverManager.registerDriver(driver);

            connection = DriverManager.getConnection(scriptRunnerConfiguration.getDbUrl(), scriptRunnerConfiguration.getDbUserName(), scriptRunnerConfiguration.getDbPassword());
            connection.setAutoCommit(false);
            logger.info("Connection to database established using driver="+scriptRunnerConfiguration.getDbDriverClassName()+" url=" + scriptRunnerConfiguration.getDbUrl() + " and user=" + scriptRunnerConfiguration.getDbUserName());
        } catch (ClassNotFoundException e) {
            logger.error("Error loading database driver " + scriptRunnerConfiguration.getDbDriverClassName(), e);
        } catch (SQLException e) {
            logger.error("Error connecting to the database", e);
        } catch (InstantiationException e) {
            logger.error("Error loading database driver" + scriptRunnerConfiguration.getDbDriverClassName(), e);
        } catch (IllegalAccessException e) {
            logger.error("Error loading database driver" + scriptRunnerConfiguration.getDbDriverClassName(), e);
        }
    }

    private Properties getScriptOptions(String scriptOptionList) {
        Properties scriptOptions = new Properties();
        if (scriptOptionList == null) {
            return scriptOptions;
        }
        String[] scriptOptionArray = scriptOptionList.split(",");
        for (String scriptOption : scriptOptionArray) {
            int equalsPos = scriptOption.indexOf("=");
            if (equalsPos > -1) {
                String key = scriptOption.substring(0, equalsPos);
                String value = scriptOption.substring(equalsPos + 1);
                scriptOptions.put(key, value);
            } else {
                logger.error("Found invalid key-pair value: " + scriptOption + ", will ignore it!");
            }
        }
        return scriptOptions;
    }

}
