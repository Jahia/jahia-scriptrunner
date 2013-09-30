package scripts

import org.apache.commons.io.IOUtils
import org.apache.jackrabbit.core.NamespaceRegistryImpl
import org.apache.jackrabbit.core.data.FileDataStore
import org.apache.jackrabbit.core.fs.FileSystemException
import org.apache.jackrabbit.core.fs.FileSystemResource
import org.apache.jackrabbit.core.fs.db.DbFileSystem
import org.apache.jackrabbit.core.fs.local.LocalFileSystem
import org.apache.jackrabbit.core.id.NodeId
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry
import org.apache.jackrabbit.core.persistence.PMContext
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager
import org.apache.jackrabbit.core.state.NodeState
import org.apache.jackrabbit.core.util.db.ConnectionFactory
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.input.SAXBuilder
import org.jdom.xpath.XPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.RepositoryException
import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

Logger logger = LoggerFactory.getLogger("dumpJCRBundles.groovy");

Connection connection = (Connection) jdbcConnection;
DatabaseConfiguration dbConfiguration = (DatabaseConfiguration) databaseConfiguration;

// PreparedStatement preparedStatement = connection.prepareStatement("SELECT NODE_ID, BUNDLE_DATA FROM jr_default_bundle");

File jahiaInstallLocation = (File) jahiaInstallLocationFile;

File jackrabbitHomeDir = new File(jahiaInstallLocation, "WEB-INF"+File.separator+"var"+File.separator+"repository");

// ResultSet resultSet = preparedStatement.executeQuery();

Element repositoryXmlRootElement = getRepositoryXmlRootElement(jahiaInstallLocation);

ConnectionFactory connectionFactory = new ConnectionFactory();

DbFileSystem dbFileSystem = getDbFileSystem(repositoryXmlRootElement);
dbFileSystem.connectionFactory = connectionFactory;
dbFileSystem.driver = dbConfiguration.driverName;
dbFileSystem.url = dbConfiguration.connectionURL;
dbFileSystem.user = dbConfiguration.userName;
dbFileSystem.password = dbConfiguration.password;
dbFileSystem.init();

NamespaceRegistryImpl namespaceRegistry = new NamespaceRegistryImpl(dbFileSystem);

NodeTypeRegistry nodeTypeRegistry = new NodeTypeRegistry(namespaceRegistry, dbFileSystem);

FileDataStore fileDataStore = new FileDataStore();
fileDataStore.init(jackrabbitHomeDir.absolutePath);

LocalFileSystem localDefaultFileSystem = new LocalFileSystem();
localDefaultFileSystem.path = jackrabbitHomeDir.absolutePath + File.separator + "workspaces" + File.separator + "default";

PMContext defaultPMContext = new PMContext(jackrabbitHomeDir, localDefaultFileSystem, loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking default workspace bundles...");

BundleDbPersistenceManager defaultDbPersistenceManager = getPersistenceManagerClass(repositoryXmlRootElement);
defaultDbPersistenceManager.connectionFactory = connectionFactory;
defaultDbPersistenceManager.driver = dbConfiguration.driverName;
defaultDbPersistenceManager.url = dbConfiguration.connectionURL;
defaultDbPersistenceManager.user = dbConfiguration.userName;
defaultDbPersistenceManager.consistencyCheck = "true";
defaultDbPersistenceManager.schemaObjectPrefix = "jr_default_";
defaultDbPersistenceManager.init(defaultPMContext);

Iterable<NodeId> allNodeIds = defaultDbPersistenceManager.getAllNodeIds(null, 0);
Iterator<NodeId> allNodeIdIterator = allNodeIds.iterator();
int count=0;
while (allNodeIdIterator.hasNext()) {
    NodeId nodeId = allNodeIdIterator.next();
    NodeState nodeState = defaultDbPersistenceManager.load(nodeId);
    count++;
}
logger.info("Loaded " + count + " node states");

LocalFileSystem localLiveFileSystem = new LocalFileSystem();
localLiveFileSystem.path = jackrabbitHomeDir.absolutePath + File.separator + "workspaces" + File.separator + "live";
PMContext livePMContext = new PMContext(jackrabbitHomeDir, localLiveFileSystem, loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking live workspace bundles...");

BundleDbPersistenceManager liveDbPersistenceManager = getPersistenceManagerClass(repositoryXmlRootElement);
liveDbPersistenceManager.connectionFactory = connectionFactory;
liveDbPersistenceManager.driver = dbConfiguration.driverName;
liveDbPersistenceManager.url = dbConfiguration.connectionURL;
liveDbPersistenceManager.user = dbConfiguration.userName;
liveDbPersistenceManager.consistencyCheck = "true";
liveDbPersistenceManager.schemaObjectPrefix = "jr_live_";
liveDbPersistenceManager.init(livePMContext);

LocalFileSystem localVersioningFileSystem = new LocalFileSystem();
localVersioningFileSystem.path = jackrabbitHomeDir.absolutePath + File.separator + "version";

PMContext versioningPmContext = new PMContext(jackrabbitHomeDir, localVersioningFileSystem, loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking versioning bundles...");

BundleDbPersistenceManager versioningDbPersistenceManager = getPersistenceManagerClass(repositoryXmlRootElement);
versioningDbPersistenceManager.connectionFactory = connectionFactory;
versioningDbPersistenceManager.driver = dbConfiguration.driverName;
versioningDbPersistenceManager.url = dbConfiguration.connectionURL;
versioningDbPersistenceManager.user = dbConfiguration.userName;
versioningDbPersistenceManager.consistencyCheck = "true";
versioningDbPersistenceManager.schemaObjectPrefix = "jr_v_";
versioningDbPersistenceManager.init(versioningPmContext);

private DataSource getDataSource(ConnectionFactory connectionFactory, String driver, String url, String user, String password) throws Exception {
    return connectionFactory.getDataSource(driver, url, user, password);
}

private NodeId loadRootNodeId(org.apache.jackrabbit.core.fs.FileSystem fileSystem) throws RepositoryException {
    final NodeId ROOT_NODE_ID = NodeId.valueOf("cafebabe-cafe-babe-cafe-babecafebabe");

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

public Element getRepositoryXmlRootElement(File jahiaInstallLocation) {
    SAXBuilder saxBuilder = new SAXBuilder();
    try {
        fileReader = new InputStreamReader(new FileInputStream(new File(jahiaInstallLocation, "WEB-INF"+File.separator+"etc" + File.separator + "repository" + File.separator + "jackrabbit" + File.separator + "repository.xml")));
        org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
        Element rootElement = jdomDocument.getRootElement();
        return rootElement;
    } catch (FileNotFoundException e) {
        logger.error("Error loading database configuration", e);
    } catch (JDOMException e) {
        logger.error("Error loading database configuration", e);
    } catch (IOException e) {
        logger.error("Error loading database configuration", e);
    }
};

public BundleDbPersistenceManager getPersistenceManagerClass(Element rootElement) {
    Element persistenceManagerElement = (Element) XPath.newInstance("/Repository/Workspace/PersistenceManager").selectSingleNode(rootElement);
    String persistenceManagerClassName = persistenceManagerElement.getAttributeValue("class");
    Class persistenceManagerClass = this.getClass().getClassLoader().loadClass(persistenceManagerClassName);
    BundleDbPersistenceManager bundleDbPersistenceManager = (BundleDbPersistenceManager) persistenceManagerClass.newInstance();
    return bundleDbPersistenceManager;
}

public DbFileSystem getDbFileSystem(Element rootElement) {
    Element fileSystemElement = (Element) XPath.newInstance("/Repository/FileSystem").selectSingleNode(rootElement);
    String fileSystemClassName = fileSystemElement.getAttributeValue("class");
    Class fileSystemClass = this.getClass().getClassLoader().loadClass(fileSystemClassName);
    DbFileSystem dbFileSystem = (DbFileSystem) fileSystemClass.newInstance();
    return dbFileSystem;
}
