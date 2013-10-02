jahia-scriptrunner
==================

A small command-line utility to perform database or other modifications without launching Jahia.

Why this tool ?
---------------

The goal of this tool is to be able to fix "chicken and egg" problems that may be encountered on
Jahia environment that are no longer accessible through normal Jahia startup. Such problems may
include :
- an incomplete namespace registry (possibly due to some invalid deployments, no longer possible
with recent version of Jackrabbit or Jahia)
- problems with database blobs than cannot be corrected through simple SQL queries

This tool is really good for all the problems that may involve serialized Java classes stored in
BLOBs since in theory it can load and modify these blobs using custom scripts. Since it uses a
class loader that gives you access to all the Jahia libraries and classes from the installed
product, the possibilities are endless as to what you may do (start a part of the system, re-use
model classes, etc...).

How it works
------------

The utility will first try to locate the Jahia installation using the specified directory or using
the current directory, it will setup a classloader with all the jars from WEB-INF/lib and WEB-INF/classes
from the Jahia installation.

From this classloader it will load the main core of the utility that will then load the database
configuration from the META-INF/context.xml file (so for the moment only Tomcat installations are
supported) and create a connection to the database.

Once all this is done it will then load and execute the Groovy script specified on the command line,
passing it the following bound variables :

    jdbcConnection - a java.sql.Connection class that contains the active connection to the database. IMPORTANT:
    The JDBC connection autocommit is NOT activated, remember to always use commits to make sure the modifications
    are properly sent to the database. This is done because PostgreSQL requires non-autocommit connections to work
    with BLOBs.

    jahiaInstallLocationFile - a java.util.File object that is the installation directory of Jahia

    classLoader - the ClassLoader used to load all the Jahia JARs and classes

    scriptOptions - a Properties object with all the options passed on the command line using the -X
    parameter

    databaseConfiguration - a org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
    object that contains the database connection configuration (driver, url, username and password)

How to use it
-------------

Command line format:

    usage: jahia-scriptrunner [options] script_to_launch
    -d,--installationDirectory <dir>     Jahia installation directory
    -h,--help                            Prints this help screen
    -l,--listScripts                     Outputs the list of built-in
                                          available scripts for this Jahia
                                          version
    -x,--scriptOptions <scriptOptions>   A comma separated list of key=value
                                          options to pass to the script

Here a sample command line we will describe :

    ./jahia-scriptrunner.sh -d /Users/loom/java/deployments/jahia-6.6/apache-tomcat-7.0.23/webapps/ROOT -x dumpXml=true dumpJCRFileSystem.groovy

In this example we launch the tool from the root directory of the tool's source code. The "-d"
 option allows you to specify in which directory it must look for all the librairies, classes and
 database configuration to load. The "-x" options allows to specify a comma separated list of key=value
 pairs that will be passed to the script as a Properties object and that may be used to modify the
 behavior of the script. The third main argument is the Groovy script to launch within the
 context of the setup classloader. The given script will dump the contents of the jr_fsg_fsentry table,
 a Jackrabbit table that contains BLOBs which themselves are file contents such as the serialized
 namespace mapping and indexes, or the custom nodetypes XML descriptor. The -x dumpXML=true flag is used
 to specify that we want to dump the XML file, by default XML files are not dumped.

Alternate database configuration
--------------------------------

Examples
--------

Dump the JCR database file system

Updating JCR namespace blobs

Here's another powerful example :

    ./jahia-scriptrunner.sh -d /Applications/Ent-Jahia_xCM_v6.6.0.0/tomcat/webapps/ROOT/ -x namespaceOperation=add,namespace=scriptTest:http://www.jahia.com/script-test updateJCRNamespaces.groovy

In this example we add the specified prefix and namespace uri to the DB file system namespace registry
file ns_reg.properties and ns_idx.properties

Checking JCR language integrity

Execute an SQL query/update statement

    ./jahia-scriptrunner-debug.sh -d /Users/loom/java/packages/Ent-Jahia_xCM_v6.6.1.6/tomcat/webapps/ROOT/ -x statement="select * from jahia_contenthistory",csvOutput=test.csv,csvSeparatorChar=";" sqlExecute.groovy

A script in detail
------------------

Here is the source code for the checkJCRLanguageIntegrity.groovy Groovy script, provided as an example of the power
offered by the tool. It uses a helper class called JackrabbitHelper that makes it easy to retrieve configuration and
persistence manager instances.

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
