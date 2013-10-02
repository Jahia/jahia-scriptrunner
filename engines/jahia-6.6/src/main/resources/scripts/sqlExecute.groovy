package scripts

import org.jahia.server.tools.scriptrunner.engines.common.DatabaseHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

Logger logger = LoggerFactory.getLogger("sqlExecute.groovy");

Connection connection = (Connection) jdbcConnection;

Properties groovyScriptOptions = (Properties) scriptOptions;
if (!groovyScriptOptions.containsKey("statement")) {
    logger.error("Missing statement parameter, please specify an SQL statement using the -x statement=\"SQL_STATEMENT\" parameter");
    return;
}

File csvOutputFile = null;
char csvSeparatorChar = ',';
if (groovyScriptOptions.containsKey("csvOutput")) {
    csvOutputFile = new File(groovyScriptOptions.getProperty("csvOutput"));
    if (groovyScriptOptions.containsKey("csvSeparatorChar")) {
        csvSeparatorChar = groovyScriptOptions.getProperty("csvSeparatorChar").charAt(0);
    }
}

String sqlStatement = groovyScriptOptions.getProperty("statement").trim();
DatabaseHelper.executeSql(sqlStatement, connection, csvOutputFile, csvSeparatorChar);

