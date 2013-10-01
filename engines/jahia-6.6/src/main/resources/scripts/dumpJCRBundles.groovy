package scripts

import org.apache.jackrabbit.core.id.NodeId
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager
import org.apache.jackrabbit.core.state.NodeState
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

Logger logger = LoggerFactory.getLogger("dumpJCRBundles.groovy");

Connection connection = (Connection) jdbcConnection;
DatabaseConfiguration dbConfiguration = (DatabaseConfiguration) databaseConfiguration;

File jahiaInstallLocation = (File) jahiaInstallLocationFile;

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(jahiaInstallLocation, dbConfiguration);

logger.info("Checking default workspace bundles...");

BundleDbPersistenceManager defaultDbPersistenceManager = jackrabbitHelper.getWorkspacePM("default");

Iterable<NodeId> allNodeIds = defaultDbPersistenceManager.getAllNodeIds(null, 0);
Iterator<NodeId> allNodeIdIterator = allNodeIds.iterator();
int count=0;
while (allNodeIdIterator.hasNext()) {
    NodeId nodeId = allNodeIdIterator.next();
    NodeState nodeState = defaultDbPersistenceManager.load(nodeId);
    count++;
}
logger.info("Loaded " + count + " node states");

logger.info("Checking live workspace bundles...");
jackrabbitHelper.getWorkspacePM("live");
logger.info("Checking versioning bundles...");
jackrabbitHelper.getVersioningPM();
