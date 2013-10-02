package org.jahia.server.tools.scriptrunner.engines.jahia66;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.db.DbFileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
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
import javax.sql.DataSource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A collection of helper methods to make it quicker to develop scripts that manipulate Jackrabbit data structure
 */
public class JackrabbitHelper {

    public static final NodeId ROOT_NODE_ID = NodeId.valueOf("cafebabe-cafe-babe-cafe-babecafebabe");

    private static final Logger logger = LoggerFactory.getLogger(JackrabbitHelper.class);

    private File jahiaInstallLocationFile;
    private File jackrabbitHomeDir;
    private DatabaseConfiguration databaseConfiguration;
    private ConnectionFactory connectionFactory = new ConnectionFactory();

    private DbFileSystem dbFileSystem = null;

    private NamespaceRegistryImpl namespaceRegistry = null;

    private NodeTypeRegistry nodeTypeRegistry;
    private Element repositoryXmlRootElement;
    private FileDataStore fileDataStore;
    private NodeId rootNodeId;
    private DataSource dataSource;

    private BundleDbPersistenceManager versioningPM = null;
    private Map<String, BundleDbPersistenceManager> workspacePMs = new HashMap<String, BundleDbPersistenceManager>();

    private boolean consistencyCheck = false;
    private boolean consistencyFix = false;

    public JackrabbitHelper(File jahiaInstallLocationFile, DatabaseConfiguration databaseConfiguration, boolean consistencyCheck, boolean consistencyFix) throws RepositoryException {
        this.jahiaInstallLocationFile = jahiaInstallLocationFile;
        this.databaseConfiguration = databaseConfiguration;
        this.consistencyCheck = consistencyCheck;
        this.consistencyFix = consistencyFix;
        this.jackrabbitHomeDir = new File(jahiaInstallLocationFile, "WEB-INF" + File.separator + "var" + File.separator + "repository");
        this.repositoryXmlRootElement = getRepositoryXmlRootElement(jahiaInstallLocationFile);
        this.dbFileSystem = getDbFileSystem(repositoryXmlRootElement);
        this.fileDataStore = new FileDataStore();
        fileDataStore.init(jackrabbitHomeDir.getAbsolutePath());
        this.rootNodeId = loadRootNodeId(dbFileSystem);
    }

    public DataSource getDataSource() throws Exception {
        if (dataSource != null) {
            return dataSource;
        }
        dataSource = connectionFactory.getDataSource(databaseConfiguration.getDriverClassName(), databaseConfiguration.getConnectionURL(), databaseConfiguration.getUserName(), databaseConfiguration.getPassword());
        return dataSource;
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

    private Element getRepositoryXmlRootElement(File jahiaInstallLocation) {
        File jackrabbitRepositoryXmlFile = new File(jahiaInstallLocation, "WEB-INF" + File.separator + "etc" + File.separator + "repository" + File.separator + "jackrabbit" + File.separator + "repository.xml");
        return getXmlRootElement(jackrabbitRepositoryXmlFile);
    }

    private Element getWorkspaceXmlRootElement(File jahiaInstallLocation, String workspaceName) {
        File jackrabbitRepositoryXmlFile = new File(jahiaInstallLocation, "WEB-INF" + File.separator + "var" + File.separator + "repository" + File.separator + "workspaces" + File.separator + workspaceName + File.separator + "workspace.xml");
        return getXmlRootElement(jackrabbitRepositoryXmlFile);
    }

    private Element getXmlRootElement(File xmlFile) {
        FileReader fileReader = null;
        SAXBuilder saxBuilder = new SAXBuilder();
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

    private BundleDbPersistenceManager getPersistenceManagerClass(Element rootElement) {
        Element persistenceManagerElement = null;
        try {
            persistenceManagerElement = (Element) XPath.newInstance("/Repository/Workspace/PersistenceManager").selectSingleNode(rootElement);
        } catch (JDOMException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
            return null;
        }
        String persistenceManagerClassName = persistenceManagerElement.getAttributeValue("class");
        Class persistenceManagerClass = null;
        BundleDbPersistenceManager bundleDbPersistenceManager = null;
        try {
            persistenceManagerClass = JackrabbitHelper.class.getClassLoader().loadClass(persistenceManagerClassName);
            bundleDbPersistenceManager = (BundleDbPersistenceManager) persistenceManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        } catch (InstantiationException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        } catch (IllegalAccessException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        }
        return bundleDbPersistenceManager;
    }

    private DbFileSystem getDbFileSystem(Element rootElement) {
        Element fileSystemElement = null;
        try {
            fileSystemElement = (Element) XPath.newInstance("/Repository/FileSystem").selectSingleNode(rootElement);
            Element databaseTypeElement = (Element) XPath.newInstance("/Repository/DataSources/DataSource/param[@name='databaseType']").selectSingleNode(rootElement);
            databaseConfiguration.setSchema(databaseTypeElement.getAttributeValue("value"));
        } catch (JDOMException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        }
        String fileSystemClassName = fileSystemElement.getAttributeValue("class");
        Class fileSystemClass = null;
        try {
            logger.info("Loading DB file system class " + fileSystemClassName + "...");
            fileSystemClass = JackrabbitHelper.class.getClassLoader().loadClass(fileSystemClassName);
            dbFileSystem = (DbFileSystem) fileSystemClass.newInstance();
            dbFileSystem.setConnectionFactory(connectionFactory);
            dbFileSystem.setDriver(databaseConfiguration.getDriverClassName());
            dbFileSystem.setUrl(databaseConfiguration.getConnectionURL());
            dbFileSystem.setUser(databaseConfiguration.getUserName());
            dbFileSystem.setPassword(databaseConfiguration.getPassword());
            dbFileSystem.setSchema(databaseConfiguration.getSchema());
            dbFileSystem.setSchemaObjectPrefix("jr_fsg_");
            dbFileSystem.init();
        } catch (ClassNotFoundException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        } catch (InstantiationException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        } catch (IllegalAccessException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        } catch (FileSystemException e) {
            logger.error("Error initialiting db file system", e);
        }
        return dbFileSystem;
    }

    public BundleDbPersistenceManager getWorkspacePM(String workspaceName) throws Exception {
        if (workspacePMs.containsKey(workspaceName)) {
            return workspacePMs.get(workspaceName);
        }

        Element workspaceXmlRootElement = getWorkspaceXmlRootElement(jahiaInstallLocationFile, workspaceName);
        Element fileSystemElement = (Element) XPath.newInstance("/Workspace/FileSystem").selectSingleNode(workspaceXmlRootElement);


        LocalFileSystem localLiveFileSystem = new LocalFileSystem();
        localLiveFileSystem.setPath(jackrabbitHomeDir.getAbsolutePath() + File.separator + "workspaces" + File.separator + workspaceName);
        PMContext livePMContext = new PMContext(jackrabbitHomeDir, localLiveFileSystem, rootNodeId, getNamespaceRegistry(), getNodeTypeRegistry(), fileDataStore);

        BundleDbPersistenceManager workspaceDbPersistenceManager = getPersistenceManagerClass(repositoryXmlRootElement);
        workspaceDbPersistenceManager.setConnectionFactory(connectionFactory);
        workspaceDbPersistenceManager.setDriver(databaseConfiguration.getDriverClassName());
        workspaceDbPersistenceManager.setUrl(databaseConfiguration.getConnectionURL());
        workspaceDbPersistenceManager.setUser(databaseConfiguration.getUserName());
        workspaceDbPersistenceManager.setPassword(databaseConfiguration.getPassword());
        workspaceDbPersistenceManager.setConsistencyCheck(Boolean.toString(consistencyCheck));
        workspaceDbPersistenceManager.setConsistencyFix(Boolean.toString(consistencyFix));
        workspaceDbPersistenceManager.setSchemaObjectPrefix("jr_" + workspaceName + "_");
        if (workspaceDbPersistenceManager.getDatabaseType() == null) {
            workspaceDbPersistenceManager.setDatabaseType(databaseConfiguration.getSchema());
        }
        workspaceDbPersistenceManager.init(livePMContext);

        workspacePMs.put(workspaceName, workspaceDbPersistenceManager);

        return workspaceDbPersistenceManager;

    }

    public BundleDbPersistenceManager getVersioningPM() throws Exception {
        if (versioningPM != null) {
            return versioningPM;
        }
        LocalFileSystem localVersioningFileSystem = new LocalFileSystem();
        localVersioningFileSystem.setPath(jackrabbitHomeDir.getAbsolutePath() + File.separator + "version");

        PMContext versioningPmContext = new PMContext(jackrabbitHomeDir,
                localVersioningFileSystem,
                rootNodeId,
                getNamespaceRegistry(),
                getNodeTypeRegistry(),
                fileDataStore);

        BundleDbPersistenceManager versioningDbPersistenceManager = getPersistenceManagerClass(repositoryXmlRootElement);
        versioningDbPersistenceManager.setConnectionFactory(connectionFactory);
        versioningDbPersistenceManager.setDriver(databaseConfiguration.getDriverClassName());
        versioningDbPersistenceManager.setUrl(databaseConfiguration.getConnectionURL());
        versioningDbPersistenceManager.setUser(databaseConfiguration.getUserName());
        versioningDbPersistenceManager.setPassword(databaseConfiguration.getPassword());
        versioningDbPersistenceManager.setConsistencyCheck(Boolean.toString(consistencyCheck));
        versioningDbPersistenceManager.setConsistencyFix(Boolean.toString(consistencyFix));
        if (versioningDbPersistenceManager.getDatabaseType() == null) {
            versioningDbPersistenceManager.setDatabaseType(databaseConfiguration.getSchema());
        }
        versioningDbPersistenceManager.setSchemaObjectPrefix("jr_v_");
        versioningDbPersistenceManager.init(versioningPmContext);

        versioningPM = versioningDbPersistenceManager;
        return versioningPM;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException {
        if (namespaceRegistry != null) {
            return namespaceRegistry;
        }
        namespaceRegistry = new NamespaceRegistryImpl(dbFileSystem);
        return namespaceRegistry;
    }

    public NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
        if (nodeTypeRegistry != null) {
            return nodeTypeRegistry;
        }
        nodeTypeRegistry = new NodeTypeRegistry(namespaceRegistry, dbFileSystem);
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
}
