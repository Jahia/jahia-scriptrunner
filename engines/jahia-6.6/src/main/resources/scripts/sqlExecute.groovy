package scripts

import org.jahia.server.tools.scriptrunner.common.Utils
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

Logger logger = LoggerFactory.getLogger("sqlExecute.groovy");

Connection connection = (Connection) jdbcConnection;

Properties groovyScriptOptions = (Properties) scriptOptions;
if (!groovyScriptOptions.containsKey("statement") && !groovyScriptOptions.containsKey("scriptFile")) {
    logger.error("Missing statement parameter, please specify an SQL statement using the -x statement=\"SQL_STATEMENT\" parameter or -x scriptFile=SCRIPT_PATH");
    logger.error("Note that wildcards are allowed in the SCRIPT_PATH value in order to execute multiple scripts at once.");
    return;
}

File csvOutputFile = null;
char csvSeparatorChar = ',';
if (groovyScriptOptions.getProperty("csvOutput") != null) {
    csvOutputFile = new File(groovyScriptOptions.getProperty("csvOutput"));
    if (groovyScriptOptions.getProperty("csvSeparatorChar") != null) {
        csvSeparatorChar = groovyScriptOptions.getProperty("csvSeparatorChar").charAt(0);
    }
}

if (groovyScriptOptions.getProperty("statement") != null) {
    String sqlStatement = groovyScriptOptions.getProperty("statement").trim();
    DatabaseHelper.executeSql(sqlStatement, connection, csvOutputFile, csvSeparatorChar);
} else if (groovyScriptOptions.getProperty("scriptFile") != null) {
    String scriptFilePath = groovyScriptOptions.getProperty("scriptFile").trim();
    File[] scriptFiles = Utils.getMatchingFiles(scriptFilePath);
    for (File scriptFile : scriptFiles) {
        if (!scriptFile.exists()) {
            logger.error("Couldn't find script file " + scriptFile);
            return;
        }
        logger.info("Executing SQL statements from script file " + scriptFile);
        List<String> scriptStatements = DatabaseHelper.getScriptFileStatements(scriptFile);
        for (String sqlStatement : scriptStatements) {
            DatabaseHelper.executeSql(sqlStatement, connection, csvOutputFile, csvSeparatorChar);
        }
    }
}