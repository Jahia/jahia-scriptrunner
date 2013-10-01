package scripts

import org.apache.jackrabbit.core.id.NodeId
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager
import org.apache.jackrabbit.core.state.NodeState
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger logger = LoggerFactory.getLogger("checkJCRLanguageIntegrity.groovy");

DatabaseConfiguration dbConfiguration = (DatabaseConfiguration) databaseConfiguration;

File jahiaInstallLocation = (File) jahiaInstallLocationFile;

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(jahiaInstallLocation, dbConfiguration, false, false);

logger.info("Checking default workspace bundles...");

BundleDbPersistenceManager defaultDbPersistenceManager = jackrabbitHelper.getWorkspacePM("default");
checkLanguageIntegrity(defaultDbPersistenceManager, jackrabbitHelper, logger);

logger.info("Checking live workspace bundles...");
BundleDbPersistenceManager liveDbPersistenceManager = jackrabbitHelper.getWorkspacePM("live");
checkLanguageIntegrity(liveDbPersistenceManager, jackrabbitHelper, logger);

logger.info("Checking versioning bundles...");
BundleDbPersistenceManager versioningDbPersistenceManager = jackrabbitHelper.getVersioningPM();
checkLanguageIntegrity(versioningDbPersistenceManager, jackrabbitHelper, logger);

private void checkLanguageIntegrity(BundleDbPersistenceManager defaultDbPersistenceManager, JackrabbitHelper jackrabbitHelper, Logger logger) {
    Iterable<NodeId> allNodeIds = defaultDbPersistenceManager.getAllNodeIds(null, 0);
    Iterator<NodeId> allNodeIdIterator = allNodeIds.iterator();
    int count = 0;
    while (allNodeIdIterator.hasNext()) {
        NodeId nodeId = allNodeIdIterator.next();
        NodeState nodeState = defaultDbPersistenceManager.load(nodeId);
        if (jackrabbitHelper.isNodeType(nodeState, "http://www.jcp.org/jcr/mix/1.0", "language")) {
            if (!jackrabbitHelper.hasProperty(nodeState, "http://www.jcp.org/jcr/1.0", "language")) {
                logger.warn("Node " + nodeState.getId() + " is of type mix:language but is missing a jcr:language property !");
            }
        }
        count++;
    }
    logger.info("Loaded " + count + " node states");
}
