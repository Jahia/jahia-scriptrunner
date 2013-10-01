package scripts

import org.apache.jackrabbit.core.NamespaceRegistryImpl
import org.apache.jackrabbit.core.data.FileDataStore
import org.apache.jackrabbit.core.fs.db.DbFileSystem
import org.apache.jackrabbit.core.fs.local.LocalFileSystem
import org.apache.jackrabbit.core.id.NodeId
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry
import org.apache.jackrabbit.core.persistence.PMContext
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager
import org.apache.jackrabbit.core.state.NodeState
import org.apache.jackrabbit.core.util.db.ConnectionFactory
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.jdom.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

Logger logger = LoggerFactory.getLogger("dumpJCRBundles.groovy");

Connection connection = (Connection) jdbcConnection;
DatabaseConfiguration dbConfiguration = (DatabaseConfiguration) databaseConfiguration;

// PreparedStatement preparedStatement = connection.prepareStatement("SELECT NODE_ID, BUNDLE_DATA FROM jr_default_bundle");

File jahiaInstallLocation = (File) jahiaInstallLocationFile;

File jackrabbitHomeDir = new File(jahiaInstallLocation, "WEB-INF"+File.separator+"var"+File.separator+"repository");

// ResultSet resultSet = preparedStatement.executeQuery();

Element repositoryXmlRootElement = JackrabbitHelper.getRepositoryXmlRootElement(jahiaInstallLocation);

ConnectionFactory connectionFactory = new ConnectionFactory();

DbFileSystem dbFileSystem = JackrabbitHelper.getDbFileSystem(repositoryXmlRootElement);
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

PMContext defaultPMContext = new PMContext(jackrabbitHomeDir, localDefaultFileSystem, JackrabbitHelper.loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking default workspace bundles...");

BundleDbPersistenceManager defaultDbPersistenceManager = JackrabbitHelper.getPersistenceManagerClass(repositoryXmlRootElement);
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
PMContext livePMContext = new PMContext(jackrabbitHomeDir, localLiveFileSystem, JackrabbitHelper.loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking live workspace bundles...");

BundleDbPersistenceManager liveDbPersistenceManager = JackrabbitHelper.getPersistenceManagerClass(repositoryXmlRootElement);
liveDbPersistenceManager.connectionFactory = connectionFactory;
liveDbPersistenceManager.driver = dbConfiguration.driverName;
liveDbPersistenceManager.url = dbConfiguration.connectionURL;
liveDbPersistenceManager.user = dbConfiguration.userName;
liveDbPersistenceManager.consistencyCheck = "true";
liveDbPersistenceManager.schemaObjectPrefix = "jr_live_";
liveDbPersistenceManager.init(livePMContext);

LocalFileSystem localVersioningFileSystem = new LocalFileSystem();
localVersioningFileSystem.path = jackrabbitHomeDir.absolutePath + File.separator + "version";

PMContext versioningPmContext = new PMContext(jackrabbitHomeDir, localVersioningFileSystem, JackrabbitHelper.loadRootNodeId(dbFileSystem), namespaceRegistry, nodeTypeRegistry, fileDataStore);

logger.info("Checking versioning bundles...");

BundleDbPersistenceManager versioningDbPersistenceManager = JackrabbitHelper.getPersistenceManagerClass(repositoryXmlRootElement);
versioningDbPersistenceManager.connectionFactory = connectionFactory;
versioningDbPersistenceManager.driver = dbConfiguration.driverName;
versioningDbPersistenceManager.url = dbConfiguration.connectionURL;
versioningDbPersistenceManager.user = dbConfiguration.userName;
versioningDbPersistenceManager.consistencyCheck = "true";
versioningDbPersistenceManager.schemaObjectPrefix = "jr_v_";
versioningDbPersistenceManager.init(versioningPmContext);
