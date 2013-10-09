package org.jahia.server.tools.scriptrunner.engines.jahia66;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.config.DataSourceConfig;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A collection of helper methods to make it quicker to develop scripts that manipulate Jackrabbit data structure
 */
public class JackrabbitHelper {

    public static final NodeId ROOT_NODE_ID = NodeId.valueOf("cafebabe-cafe-babe-cafe-babecafebabe");

    private static final Logger logger = LoggerFactory.getLogger(JackrabbitHelper.class);

    private File jackrabbitConfigFile;
    private File jackrabbitHomeDir;
    private DatabaseConfiguration databaseConfiguration;
    private ConnectionFactory connectionFactory = new ConnectionFactory();

    private FileSystem repositoryFileSystem = null;

    private NamespaceRegistryImpl namespaceRegistry = null;

    private NodeTypeRegistry nodeTypeRegistry;
    private Element repositoryXmlRootElement;
    private DataStore dataStore;
    private NodeId rootNodeId;

    private PersistenceManager versioningPM = null;
    private Map<String, PersistenceManager> workspacePMs = new HashMap<String, PersistenceManager>();

    private boolean consistencyCheck = false;
    private boolean consistencyFix = false;

    private String workspacesRootPath = null;
    private File workspacesRootFile = null;

    private Properties jackrabbitProperties = new Properties();

    public JackrabbitHelper(File jackrabbitConfigFile, File jackrabbitHomeDir, DatabaseConfiguration databaseConfiguration, boolean consistencyCheck, boolean consistencyFix) throws RepositoryException, JDOMException {
        this.jackrabbitConfigFile = jackrabbitConfigFile;
        this.jackrabbitHomeDir = jackrabbitHomeDir;
        this.databaseConfiguration = databaseConfiguration;
        this.consistencyCheck = consistencyCheck;
        this.consistencyFix = consistencyFix;
        this.repositoryXmlRootElement = getRepositoryXmlRootElement();
        Element dataSourceElement = (Element) XPath.newInstance("/Repository/DataSources/DataSource").selectSingleNode(repositoryXmlRootElement);
        String dataSourceName = dataSourceElement.getAttributeValue("name");
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty(DataSourceConfig.DRIVER, databaseConfiguration.getDriverClassName());
        dataSourceProperties.setProperty(DataSourceConfig.URL, databaseConfiguration.getConnectionURL());
        dataSourceProperties.setProperty(DataSourceConfig.USER, databaseConfiguration.getUserName());
        dataSourceProperties.setProperty(DataSourceConfig.PASSWORD, databaseConfiguration.getPassword());
        dataSourceProperties.setProperty(DataSourceConfig.DB_TYPE, databaseConfiguration.getDatabaseType());
        dataSourceConfig.addDataSourceDefinition(dataSourceName, dataSourceProperties);
        Element workspacesElement = (Element) XPath.newInstance("/Repository/Workspaces").selectSingleNode(repositoryXmlRootElement);
        jackrabbitProperties.setProperty("rep.home", jackrabbitHomeDir.getAbsolutePath());
        jackrabbitProperties.setProperty("jahia.jackrabbit.consistencyCheck", Boolean.toString(consistencyCheck));
        jackrabbitProperties.setProperty("jahia.jackrabbit.consistencyFix", Boolean.toString(consistencyFix));
        workspacesRootPath = getRealValue(jackrabbitProperties, new Properties(), workspacesElement.getAttributeValue("rootPath"));
        workspacesRootFile = new File(workspacesRootPath);
        connectionFactory.registerDataSources(dataSourceConfig);
        this.repositoryFileSystem = getFileSystem(repositoryXmlRootElement, "/Repository/FileSystem", jackrabbitProperties);
        this.dataStore = getDataStoreInstance(repositoryXmlRootElement, jackrabbitProperties);
        if (dataStore instanceof DatabaseAware) {
            ((DatabaseAware) dataStore).setConnectionFactory(connectionFactory);
        }
        dataStore.init(jackrabbitHomeDir.getAbsolutePath());
        this.rootNodeId = loadRootNodeId(repositoryFileSystem);
    }

    private NodeId loadRootNodeId(org.apache.jackrabbit.core.fs.FileSystem fileSystem) throws RepositoryException {
        try {
            FileSystemResource uuidFile = new FileSystemResource(
                    fileSystem, "/meta/rootUUID");
            if (uuidFile.exists()) {
                // Load uuid of the repository's root node. It is stored in
                // text format (36 characters) for better readability.
                InputStream inputStream = uuidFile.getInputStream();
                try {
                    return NodeId.valueOf(IOUtils.toString(inputStream, "US-ASCII"));
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            } else {
                // Use hard-coded uuid for root node rather than generating
                // a different uuid per repository instance; using a
                // hard-coded uuid makes it easier to copy/move entire
                // workspaces from one repository instance to another.
                uuidFile.makeParentDirs();
                OutputStream out = uuidFile.getOutputStream();
                try {
                    out.write(ROOT_NODE_ID.toString().getBytes("US-ASCII"));
                    return ROOT_NODE_ID;
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        } catch (IOException e) {
            throw new RepositoryException(
                    "Failed to load or persist the root node identifier", e);
        } catch (FileSystemException fse) {
            throw new RepositoryException(
                    "Failed to access the root node identifier", fse);
        }
    }

    private Element getRepositoryXmlRootElement() throws JDOMException {
        Element rootElement = getXmlRootElement(jackrabbitConfigFile);
        if (databaseConfiguration.getDatabaseType() == null) {
            Element databaseTypeElement = (Element) XPath.newInstance("/Repository/DataSources/DataSource/param[@name='databaseType']").selectSingleNode(rootElement);
            databaseConfiguration.setDatabaseType(databaseTypeElement.getAttributeValue("value"));
        }
        return rootElement;
    }

    private Element getWorkspaceXmlRootElement(String workspaceName) {
        File workspaceXmlConfigFile = new File(workspacesRootFile, workspaceName + File.separator + "workspace.xml");
        return getXmlRootElement(workspaceXmlConfigFile);
    }

    private Element getXmlRootElement(File xmlFile) {
        FileReader fileReader = null;
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);
        try {
            fileReader = new FileReader(xmlFile);
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element rootElement = jdomDocument.getRootElement();
            return rootElement;
        } catch (FileNotFoundException e) {
            logger.error("Error loading database configuration", e);
        } catch (JDOMException e) {
            logger.error("Error loading database configuration", e);
        } catch (IOException e) {
            logger.error("Error loading database configuration", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
        }
        return null;
    }

    private Properties getParameters(Element parentElement, Properties existingProperties) {
        List<Element> paramElements = parentElement.getChildren("param");
        Properties parameters = new Properties();
        for (Element paramElement : paramElements) {
            String paramName = paramElement.getAttributeValue("name");
            String paramValue = paramElement.getAttributeValue("value");
            paramValue = getRealValue(existingProperties, parameters, paramValue);
            parameters.put(paramName, paramValue);
        }
        return parameters;
    }

    private String getRealValue(Properties existingProperties, Properties parameters, String paramValue) {
        String propertyName = null;
        while ((propertyName = getPropertyReference(paramValue)) != null) {
            if (existingProperties.getProperty(propertyName) != null) {
                paramValue = paramValue.replace("${" + propertyName + "}", existingProperties.getProperty(propertyName));
            } else if (parameters.getProperty(propertyName) != null) {
                paramValue = paramValue.replace("${" + propertyName + "}", parameters.getProperty(propertyName));
            } else {
                logger.warn("Couldn't find a property name " + propertyName + ", aborting property resolving...");
                break;
            }
        }
        return paramValue;
    }

    private String getPropertyReference(String value) {
        int markerStart = value.indexOf("${");
        if (markerStart < 0) {
            return null;
        }
        int markerEnd = value.indexOf("}", markerStart+"${".length());
        if (markerEnd < 0) {
            return null;
        }
        return value.substring(markerStart+"${".length(), markerEnd);
    }

    private PersistenceManager getWorkspacePMInstance(Element rootElement, Properties existingProperties) {
        return getPersistenceManagerInstance(rootElement, "/Workspace/PersistenceManager", existingProperties);
    }

    private PersistenceManager getVersioningPMInstance(Element rootElement, Properties existingProperties) {
        return getPersistenceManagerInstance(rootElement, "/Repository/Versioning/PersistenceManager", existingProperties);
    }

    private PersistenceManager getPersistenceManagerInstance(Element rootElement, String xPathQuery, Properties existingProperties) {
        Element persistenceManagerElement = null;
        Properties parameters = null;
        try {
            persistenceManagerElement = (Element) XPath.newInstance(xPathQuery).selectSingleNode(rootElement);
            parameters = getParameters(persistenceManagerElement, existingProperties);
        } catch (JDOMException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
            return null;
        }
        String persistenceManagerClassName = persistenceManagerElement.getAttributeValue("class");
        return (PersistenceManager) getClassInstance(persistenceManagerClassName, parameters);
    }

    private FileSystem getFileSystemInstance(Element rootElement, String xPathQuery, Properties existingProperties) {
        Element fileSystemElement = null;
        Properties parameters = null;
        try {
            fileSystemElement = (Element) XPath.newInstance(xPathQuery).selectSingleNode(rootElement);
            parameters = getParameters(fileSystemElement, existingProperties);
        } catch (JDOMException e) {
            logger.error("Error retrieving file system class from Jackrabbit repository configuration", e);
            return null;
        }
        String fileSystemClassName = fileSystemElement.getAttributeValue("class");
        return (FileSystem) getClassInstance(fileSystemClassName, parameters);
    }

    private Object getClassInstance(String className, Properties parameters) {
        try {
            logger.info("Loading class " + className + "...");
            Class fileSystemClass = JackrabbitHelper.class.getClassLoader().loadClass(className);
            Object instance = fileSystemClass.newInstance();
            if (parameters != null) {
                BeanUtils.populate(instance, parameters);
            }
            return instance;
        } catch (ClassNotFoundException e) {
            logger.error("Error retrieving class from Jackrabbit repository configuration", e);
        } catch (InstantiationException e) {
            logger.error("Error retrieving class from Jackrabbit repository configuration", e);
        } catch (IllegalAccessException e) {
            logger.error("Error retrieving class from Jackrabbit repository configuration", e);
        } catch (InvocationTargetException e) {
            logger.error("Error retrieving class from Jackrabbit repository configuration", e);
        }
        return null;
    }

    private FileSystem getFileSystem(Element rootElement, String xPathQuery, Properties existingProperties) {
        FileSystem fileSystem = null;
        try {
            fileSystem = getFileSystemInstance(rootElement, xPathQuery, existingProperties);
            if (fileSystem instanceof DatabaseAware) {
                ((DatabaseAware)fileSystem).setConnectionFactory(connectionFactory);
            }
            fileSystem.init();
        } catch (FileSystemException e) {
            logger.error("Error initialiting db file system", e);
        }
        return fileSystem;
    }

    public PersistenceManager getWorkspacePM(String workspaceName) throws Exception {
        if (workspacePMs.containsKey(workspaceName)) {
            return workspacePMs.get(workspaceName);
        }

        Properties workspaceProperties = new Properties(jackrabbitProperties);
        workspaceProperties.setProperty("wsp.name", workspaceName);
        workspaceProperties.setProperty("wsp.home", new File(workspacesRootFile, workspaceName).getAbsolutePath());

        Element workspaceXmlRootElement = getWorkspaceXmlRootElement(workspaceName);

        FileSystem workspaceFileSystem = getFileSystem(workspaceXmlRootElement, "/Workspace/FileSystem", workspaceProperties);
        PMContext livePMContext = new PMContext(jackrabbitHomeDir, workspaceFileSystem, rootNodeId, getNamespaceRegistry(), getNodeTypeRegistry(), dataStore);

        PersistenceManager workspacePM = getWorkspacePMInstance(workspaceXmlRootElement, workspaceProperties);
        if (workspacePM instanceof DatabaseAware) {
            ((DatabaseAware)workspacePM).setConnectionFactory(connectionFactory);
        }
        workspacePM.init(livePMContext);

        workspacePMs.put(workspaceName, workspacePM);

        return workspacePM;

    }

    public PersistenceManager getVersioningPM() throws Exception {
        if (versioningPM != null) {
            return versioningPM;
        }

        Properties existingProperties = new Properties(jackrabbitProperties);

        FileSystem versioningFileSystem = getFileSystem(repositoryXmlRootElement, "/Repository/Versioning/FileSystem", existingProperties);

        PMContext versioningPmContext = new PMContext(jackrabbitHomeDir,
                versioningFileSystem,
                rootNodeId,
                getNamespaceRegistry(),
                getNodeTypeRegistry(),
                dataStore);

        versioningPM = getVersioningPMInstance(repositoryXmlRootElement, existingProperties);
        if (versioningPM instanceof DatabaseAware) {
            ((DatabaseAware)versioningPM).setConnectionFactory(connectionFactory);
        }
        versioningPM.init(versioningPmContext);
        return versioningPM;
    }

    public DataStore getDataStoreInstance(Element rootElement, Properties existingProperties) {
        Element dataStoreElement = null;
        Properties parameters = null;
        try {
            dataStoreElement = (Element) XPath.newInstance("/Repository/DataStore").selectSingleNode(rootElement);
            parameters = getParameters(dataStoreElement, existingProperties);
        } catch (JDOMException e) {
            logger.error("Error retrieving data store class from Jackrabbit repository configuration", e);
            return null;
        }
        String dataStoreClassName = dataStoreElement.getAttributeValue("class");
        return (DataStore) getClassInstance(dataStoreClassName, parameters);

    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException {
        if (namespaceRegistry != null) {
            return namespaceRegistry;
        }
        namespaceRegistry = new NamespaceRegistryImpl(repositoryFileSystem);
        return namespaceRegistry;
    }

    public NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
        if (nodeTypeRegistry != null) {
            return nodeTypeRegistry;
        }
        nodeTypeRegistry = new NodeTypeRegistry(namespaceRegistry, repositoryFileSystem);
        return nodeTypeRegistry;
    }

    public boolean isNodeType(NodeState nodeState, String namespaceUri, String nodeTypeLocalName) throws RepositoryException, NodeTypeConflictException {
        Name nodeTypeNameToMatch = NameFactoryImpl.getInstance().create(namespaceUri, nodeTypeLocalName);
        Name primary = nodeState.getNodeTypeName();
        if (nodeTypeNameToMatch.equals(nodeState.getNodeTypeName())) {
            return true;
        }
        Set<Name> mixins = nodeState.getMixinTypeNames();
        if (nodeState.getMixinTypeNames().contains(nodeTypeNameToMatch)) {
            return true;
        }
        EffectiveNodeType type =
                getNodeTypeRegistry().getEffectiveNodeType(primary, mixins);
        return type.includesNodeType(nodeTypeNameToMatch);
    }

    public boolean hasProperty(NodeState nodeState, String namespaceUri, String localPropertyName) {
        Name propertyNameToMatch = NameFactoryImpl.getInstance().create(namespaceUri, localPropertyName);
        if (nodeState.getPropertyNames().contains(propertyNameToMatch)) {
            return true;
        } else {
            return false;
        }
    }

    public FileSystem getRepositoryFileSystem() {
        return repositoryFileSystem;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }
}
