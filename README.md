jahia-scriptrunner
==================

A small command-line utility to execute scripts to perform database or other low-level modifications without
launching Jahia or other Jackrabbit-based application.

Why this tool ?
---------------

The goal of this tool is to be able to fix "chicken and egg" problems that may be encountered on
Jackrabbit environment that are no longer accessible through normal Jackrabbit startup. Such problems may
include :
- an incomplete namespace registry (possibly due to some invalid deployments, no longer possible
with recent version of Jackrabbit or Jahia)
- problems with database blobs than cannot be corrected through simple SQL queries
- problems with JCR content integrity that may prevent proper indexing, which will also prevent proper repository
initialization
- other database issues related or not to Jackrabbit tables.

This tool is really good for all the problems that may involve serialized Java classes stored in
BLOBs since in theory it can load and modify these blobs using custom scripts. Since it uses a
class loader that gives you access to all libraries and classes from the installed
product, the possibilities are endless as to what you may do (start a part of the system, re-use
model classes, etc...). It can also be used to easily execute SQL statements against the database
using the configuration already setup in target product (such as a Jahia server installation)

Features
--------

The Jahia Script Runner has a lot of built-in features, which may be expanded easily by writing scripts to use it
with:
* Full native integration with the configuration of Jahia servers, retrieve database connection and Jackrabbit
  configuration from the deployed installation.
* Compatibility with other Jackrabbit-based applications by using standard Jackrabbit configuration files
* Compatibility with other JDBC applications by being able to specify the database configuration in the
  configuration file
* Groovy scripting is supported out of the box, with the possibility to support any JSR-223 language simply by
  adding it in the tool's script class loader
* Scripts execute in a dynamically setup classloader that is setup using the tool's configuration, using either
  custom properties or automatically resolved from a Jahia deployed installation (in the case of non-Jahia applications,
  you may simply override the default class loader property to whatever you need)
* Built-in script to update Jackrabbit namespaces without starting the full repository, to correct any inconsistency
  that could exist between declared namespaces and JCR content. This works for all Jackrabbit file systems, including
  file systems that are stored in database blobs.
* Built-in script to dump the contents of the Jackrabbit file system, regardless of the FS implementation used.
* Built-in script to perform language integrity checks on JCR contents node inside of a Jackrabbit persistence manager
  without needed to start the whole repository system. This is very useful if there is a problem with the Lucene indexes
  preventing a full startup of the repository.
* Helper classes to provide a JDBC connection or Jackrabbit persistence manager instances to easily access these
  backends. You can use this to for example directly load node states from a Jackrabbit persistence manager and perform
  checks or even modify entries without starting the repository !
* Build your own scripts to do much more !

How it works
------------

The tool will first try to load a configuration properties file either specified as a command line parameter (using
the -c parameter) or try to find a file called scriptRunner.properties in the current work property. If none could
be found it will use a built-in scriptRunner.properties that defaults all settings to work with a deployed Jahia
server installation.

Once the properties are loaded, it will use them to setup a class loader in which to execute the script. The classPath
property is used to specify the locations and JARs to include in the classloader.

From this classloader it will load the main core of the utility that will then load the database
configuration from the Tomcat-specific META-INF/context.xml file or from a manually specified JDBC configuration and
create a connection to the database (see the Alternate database configuration section below).

Once all this is done it will then load and execute the Groovy script specified on the command line,
passing it the following bound variables :

    jdbcConnection - a java.sql.Connection class that contains the active connection to the database. IMPORTANT:
    The JDBC connection autocommit is NOT activated, remember to always use commits to make sure the modifications
    are properly sent to the database. This is done because PostgreSQL requires non-autocommit connections to work
    with BLOBs.

    scriptRunnerConfiguration - a instance of the ScriptRunnerConfiguration class that contains all the configuration
    of the current script runner instance

    classLoader - the ClassLoader used to load all the Jahia JARs and classes

    scriptOptions - a Properties object with all the options passed on the command line using the -X
    parameter or coming from the scriptOptions property in the configuration file

How to use it
-------------

Command line format:

    usage: jahia-scriptrunner [options] script_to_launch
     -c,--configFile <file>               The configuration file to use to
                                          setup the Jahia script runner
     -d,--baseDirectory <dir>             Target base directory
     -h,--help                            Prints this help screen
     -l,--listScripts                     Outputs the list of built-in
                                          available scripts for this Jahia
                                          version
     -v,--targetVersion <version>         Overrides the automatic version
                                          detection and specify a version
                                          using this command line option
     -x,--scriptOptions <scriptOptions>   A comma separated list of key=value
                                          options to pass to the script

Here a sample command line we will describe :

    ./jahia-scriptrunner.sh -d /Users/loom/java/deployments/jahia-6.6/apache-tomcat-7.0.23/webapps/ROOT -x dumpXml=true dumpJCRFileSystem.groovy

The "-d" option allows you to specify in the base directory it will use to resolve all other paths specified in the
configuration file. The "-x" options allows to specify a comma separated list of key=value
 pairs that will be passed to the script as a Properties object and that may be used to modify the
 behavior of the script. The third main argument is the Groovy script to launch within the
 context of the setup classloader. The given script will dump the contents of the Jackrabbit repository file system
 which includes files such as the serialized namespace mapping and indexes, or the custom nodetypes XML descriptor.
 The -x dumpXML=true flag is used to specify that we want to dump the XML file, by default XML files are not dumped.
 This script is really interesting in the case of a DatabaseFileSystem configuration, since in that case the contents
 of the files are serialized within BLOBS in the database, making their access difficult without a tool such as this
 one.

Alternate database configuration
--------------------------------

By default the tool will try to retrieve the database configuration from Jahia's META-INF/context.xml, but in some
cases this might not work because either the file was not configured or it is not available as in the case of another
Jackrabbit-based application . To handle cases like these,
it is possible to provide a database configuration in a custom Script Runner configuration that may either be specifed
on the command line using a -c parameter or simply named scriptRunner.properties and located in the current work
directory. The contents should at a minimum look like this :

    # The dbConfigurationSource property points to a Tomcat-specific context.xml descriptor since this is where Jahia
    # installations store the database configuration, even when deployed on non-Tomcat application servers such as
    # WebSphere. If you prefer to specify the database configuration manually, make sure this setting is empty as it will
    # always take precedence over the manual settings below.
    dbConfigurationSource=
    # The dbDerbySystemHome setting is used to initialize the derby.system.home property if it was not specified directly
    # on the JVM start command line (usually not the case with the default shell scripts). This setting is only used if
    # the script runner detects a "derby" sub-string in the database connection URL (either from the dbConfigurationSource
    # Tomcat descriptor of the dbUrl setting below)
    dbDerbySystemHome=${baseDirectory}/WEB-INF/var/dbdata
    # The following properties make it possible to setup a manual JDBC connection to the database, useful if you cannot use
    # the Tomcat-specific driver. Make sure you blank the dbConfigurationSource setting as it will otherwise take
    # precedence.
    dbDriverClassName=com.mysql.jdbc.Driver
    dbUrl=jdbc:mysql://localhost/jahia-6.6?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=false
    dbUserName=jahia
    dbPassword=jahia
    dbDatabaseType=mysql

The schema is the name of the databaseType that is configured in the Jackrabbit repository.xml configuration file
in the following section (located at WEB-INF/etc/repository/jackrabbit/repository.xml in Jahia installations) :

      <DataSources>
        <DataSource name="jahiaDS">
          <param name="driver" value="javax.naming.InitialContext" />
          <param name="url" value="java:comp/env/jdbc/jahia" />
          <param name="databaseType" value="mysql" />
        </DataSource>
      </DataSources>

Also, if you need to provide another database driver version or JAR, you will need to modify the classPath property to
include the driver's JAR path:

    # The classPath setting is used to build a class loader in which the script will be executed. You might want to customize
    # this setting if you are bringing your own database driver and want to automatically include it in the script class
    # loader.
    classPath=mysql-connector-java-5.1.26.jar,${baseDirectory}/WEB-INF/classes,${baseDirectory}/WEB-INF/lib/*.jar

Examples
--------

### Dump the JCR database file system

The following example will dump the contents of the database file system configured for the JCR repository:

    ./jahia-scriptrunner.sh -d /Users/loom/java/deployments/jahia-6.6/apache-tomcat-7.0.23/webapps/ROOT -x dumpXml=true dumpJCRFileSystem.groovy

### Updating JCR namespace blobs

In this example we add the specified prefix and namespace uri to the DB file system namespace registry
file ns_reg.properties and ns_idx.properties

    ./jahia-scriptrunner.sh -d /Applications/Ent-Jahia_xCM_v6.6.0.0/tomcat/webapps/ROOT/ -x namespaceOperation=add,namespace=scriptTest:http://www.jahia.com/script-test updateJCRNamespaces.groovy


### Checking JCR language integrity

The following example will check the JCR to make sure that all mix:language nodes have a property named jcr:language

    ./jahia-scriptrunner.sh -d /Applications/Ent-Jahia_xCM_v6.6.0.0/tomcat/webapps/ROOT/ checkJCRLanguageIntegrity.groovy

### Execute an SQL query/update statement or an SQL script file

In the following example we show an example of how to execute an SQL query on the database configured in Jahia.

    ./jahia-scriptrunner.sh -d /Users/loom/java/packages/Ent-Jahia_xCM_v6.6.1.6/tomcat/webapps/ROOT/ -x statement="select * from jahia_contenthistory",csvOutput=test.csv,csvSeparatorChar=";" sqlExecute.groovy

If you prefer to use an SQL script file to execute many statements at once, you can do so by using the scriptFile
script parameter as in the following example :

    ./jahia-scriptrunner.sh -d /Users/loom/java/deployments/jahia-6.6/apache-tomcat-6.0.20/webapps/ROOT -x scriptFile=/Users/loom/java/deployments/jahia-6.6/apache-tomcat-6.0.20/webapps/ROOT/WEB-INF/var/db/sql/schema/mysql/jackrabbit-schema.sql sqlExecute.groovy

Be careful when using the script options to always put double-quotes around the statement and the separator char to make
sure it doesn't get interpreted wrong.

Settings
--------

The tool will first try to load a configuration properties file either specified as a command line parameter (using
the -c parameter) or try to find a file called scriptRunner.properties in the current work property. If none could
be found it will use a built-in scriptRunner.properties that defaults all settings to work with a deployed Jahia
server installation.

Here are the contents of the default scriptRunner.properties which includes documentation for all the most important
properties:

    # The baseDirectory is used to specify a base directory that will then be used for all runtime execution, mostly by
    # using a property reference in the properties below. This is an optional parameter since by default it will have
    # the value of the current work directory or the value specified using the -d command line parameter.
    # baseDirectory=

    # The scriptOptions property is used to pass options to the script being executed. The format is a comma-seperated list
    # key=value pairs that will be converted to a Properties object passed to the executing script. It is also possible
    # to specify script options directly from the command line using the -x parameter, and this is usually the most
    # practical way of using them.
    # scriptOptions=namespaceOperation=remove,namespace=test2:http://localhost/test2

    # The engineName is used to resolve the engine to be used inside the script runner. The engine will be inserted in the
    # class loader setup for script execution.
    engineName=jahia
    # The engineDisplayName is the name of the engine that will be used in user interface screen
    engineDisplayName=Jahia
    # The engineDefault version will be used if no engine version can be resolved. See the versionDetection settings for
    # more details on how the version is resolved.
    engineDefaultVersion=6.6

    # The classPath setting is used to build a class loader in which the script will be executed. You might want to customize
    # this setting if you are bringing your own database driver and want to automatically include it in the script class
    # loader.
    classPath=${baseDirectory}/WEB-INF/classes,${baseDirectory}/WEB-INF/lib/*.jar

    # The tempDirectory is used to specify the temporary directory in which some built-in JARs will be copied to be able
    # to add them to the script class loader, as it is unfortunately not possible to add direct references to embedded JARs
    # in a class loader (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4735639 for more details)
    tempDirectory=${user.home}/.scriptrunner/temp

    # The versionDetectionJar setting is used to specify the JAR in which the version will be resolved using the MANIFEST
    # entries. The versionDetectionVersionAttributeName and versionDetectionBuildAttributeName are used to specify the
    # name of the attributes in the MANIFEST that will contain the version and build number information.
    versionDetectionJar=${baseDirectory}/WEB-INF/lib/jahia-impl-*.jar
    versionDetectionVersionAttributeName=Implementation-Version
    versionDetectionBuildAttributeName=Implementation-Build

    # The following Jackrabbit properties are used to setup the JackrabbitHelper class and are rather self-speaking. The
    # consistency check and fix boolean will indicate whether consistency checks and fixes should be run when initializing
    # access to the repository content.
    jackrabbitConfigFile=${baseDirectory}/WEB-INF/etc/repository/jackrabbit/repository.xml
    jackrabbitHomeDirectory=${baseDirectory}/WEB-INF/var/repository
    jackrabbitConsistencyCheck=false
    jackrabbitConsistencyFix=false

    # The dbConfigurationSource property points to a Tomcat-specific context.xml descriptor since this is where Jahia
    # installations store the database configuration, even when deployed on non-Tomcat application servers such as
    # WebSphere. If you prefer to specify the database configuration manually, make sure this setting is empty as it will
    # always take precedence over the manual settings below.
    dbConfigurationSource=${baseDirectory}/META-INF/context.xml
    # The dbDerbySystemHome setting is used to initialize the derby.system.home property if it was not specified directly
    # on the JVM start command line (usually not the case with the default shell scripts). This setting is only used if
    # the script runner detects a "derby" sub-string in the database connection URL (either from the dbConfigurationSource
    # Tomcat descriptor of the dbUrl setting below)
    dbDerbySystemHome=${baseDirectory}/WEB-INF/var/dbdata
    # The following properties make it possible to setup a manual JDBC connection to the database, useful if you cannot use
    # the Tomcat-specific driver. Make sure you blank the dbConfigurationSource setting as it will otherwise take
    # precedence.
    # dbDriverClassName=
    # dbUrl=
    # dbUserName=
    # dbPassword=
    # dbDatabaseType=

A script in detail
------------------

Here is the source code for the checkJCRLanguageIntegrity.groovy Groovy script, provided as an example of the power
offered by the tool. It uses a helper class called JackrabbitHelper that makes it easy to retrieve configuration and
persistence manager instances.

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
    checkLanguageIntegrity(livePM, jackrabbitHelper, logger);

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

As you can see the code is quite straight-forward and it should be relatively easy to expand on it to perform other
integrity checks or even perform modifications on the node states.

Engines
-------

Engines are sub-projects of the Jahia Script Runner tool that help develop scripts by including dependencies on a
specific project. For example, the Jahia 6.6 engine sub-project has a dependency on Jahia 6.6's core Maven project,
making it easier to use code completion when writing scripts or helper classes that match the correct Jahia version.

Engines also contain built-in scripts for a specific version of an application. You can use the -l command line
parameter to list the available built-in scripts for a specific version.

Upon startup, the Jahia Script Runner will try to resolve the engineName and engineVersion to be used by using the
versionDetection properties. By default for Jahia it will look in the WEB-INF/lib/jahia-impl-*.jar MANIFEST attributes
to resolve the version of Jahia that is installed. It will then look for a script runner embedded JAR with a name
that matches the following pattern :

    libs/jahia-scriptrunner-engines-ENGINE_NAME-ENGINE_VERSION-SCRIPT_RUNNER_VERSION.jar

Note that the version resolution is smart enough to look up version numbers. So if the product version is 6.6.2.1 it
will first look for an engine version with 6.6.2.1, then try 6.6.2, then 6.6 and finally 6. If none could be found it
will use the default engine version version property value.

If you want to support another version of Jahia, or provide a built-in engine for another application, you can simply
define a new engine sub-project that will be matched at execution time using this resolution mechanism.
