package scripts

import org.apache.jackrabbit.core.id.NodeId
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager
import org.apache.jackrabbit.core.state.NodeState
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger logger = LoggerFactory.getLogger("checkJCRLanguageIntegrity.groovy");

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(scriptRunnerConfiguration);

logger.info("Checking default workspace bundles...");

IterablePersistenceManager defaultPM = (IterablePersistenceManager) jackrabbitHelper.getWorkspacePM("default");
checkLanguageIntegrity(defaultPM, jackrabbitHelper, logger);

logger.info("Checking live workspace bundles...");
IterablePersistenceManager livePM = (IterablePersistenceManager) jackrabbitHelper.getWorkspacePM("live");
if (livePM != null) {
    checkLanguageIntegrity(livePM, jackrabbitHelper, logger);
}

logger.info("Checking versioning bundles...");
IterablePersistenceManager versioningPM = (IterablePersistenceManager) jackrabbitHelper.getVersioningPM();
checkLanguageIntegrity(versioningPM, jackrabbitHelper, logger);

private void checkLanguageIntegrity(IterablePersistenceManager iterablePersistenceManager, JackrabbitHelper jackrabbitHelper, Logger logger) {
    Iterable<NodeId> allNodeIds = iterablePersistenceManager.getAllNodeIds(null, 0);
    Iterator<NodeId> allNodeIdIterator = allNodeIds.iterator();
    int count = 0;
    while (allNodeIdIterator.hasNext()) {
        NodeId nodeId = allNodeIdIterator.next();
        NodeState nodeState = iterablePersistenceManager.load(nodeId);
        if (jackrabbitHelper.isNodeType(nodeState, "http://www.jcp.org/jcr/mix/1.0", "language")) {
            if (!jackrabbitHelper.hasProperty(nodeState, "http://www.jcp.org/jcr/1.0", "language")) {
                logger.warn("Node " + nodeState.getId() + " is of type mix:language but is missing a jcr:language property !");
            }
        }
        count++;
    }
    logger.info("Loaded " + count + " node states");
}
