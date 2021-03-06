package org.jahia.server.tools.scriptrunner.engines.common;

import au.com.bytecode.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to simply database handling scripts.
 */
public class DatabaseHelper {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);

    public static String padToLength(String input, int totalLength, String paddingChar) {
        int remainingLength = totalLength - input.length();
        if (remainingLength < 1) {
            return input;
        }
        if (totalLength > 1000) {
            remainingLength = 1000 - input.length();
        }
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < remainingLength; i++) {
            padding.append(paddingChar);
        }
        return input + padding.toString();
    }

    public static boolean isBinary(int sqlType) {
        return sqlType == Types.BINARY ||
                sqlType == Types.BLOB ||
                sqlType == Types.LONGVARBINARY ||
                sqlType == Types.VARBINARY;
    }

    public static String getColumnValue(ResultSet rs, int column) throws SQLException {
        if (isBinary(rs.getMetaData().getColumnType(column))) {
            return rs.getMetaData().getColumnTypeName(column);
        }
        Object object = rs.getObject(column);
        if (object != null) {
            return object.toString();
        }
        return "NULL";
    }

    public static String getColumnName(ResultSet rs, int column) throws SQLException {
        String columnName = rs.getMetaData().getColumnLabel(column);
        if (columnName != null && columnName.length() > 0) {
            return columnName;
        }
        return rs.getMetaData().getColumnName(column);
    }

    public static void executeSqlQuery(String sqlQuery, Connection connection, File csvOutputFile, char separatorChar) throws SQLException, IOException {
        long startTime = System.currentTimeMillis();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sqlQuery);
        ResultSetMetaData rsMetaData = rs.getMetaData();
        int resultCount = 0;
        int columnCount = rsMetaData.getColumnCount();
        int[] columnWidths = new int[columnCount];
        List<List<String>> lines = new ArrayList<List<String>>();
        List<String> columns = new ArrayList<String>();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = getColumnName(rs, i);
            columnWidths[i-1] = columnName.length();
            columns.add(columnName);
        }
        lines.add(columns);
        columns = new ArrayList<String>();
        for (int i = 1; i <= columnCount; i++) {
            String columnTypeName = rsMetaData.getColumnTypeName(i) + "(" + rsMetaData.getColumnDisplaySize(i) + ")";
            if (columnWidths[i-1] < columnTypeName.length()) {
                columnWidths[i-1] = columnTypeName.length();
            }
            columns.add(columnTypeName);
        }
        lines.add(columns);
        while (rs.next() && resultCount < 100) {
            columns = new ArrayList<String>();
            for (int i = 1; i <= columnCount; i++) {
                String columnValue = getColumnValue(rs, i);
                if (columnWidths[i-1] < columnValue.length()) {
                    columnWidths[i-1] = columnValue.length();
                }
                columns.add(columnValue);
            }
            lines.add(columns);
            resultCount++;
            if (resultCount == 100) {
                logger.info("Only retrieving first 100 results.");
            }
        }
        rs.close();
        statement.close();
        connection.commit();
        long queryTime = System.currentTimeMillis() - startTime;
        outputResultSet(columnWidths, lines);
        if (csvOutputFile != null) {
            CSVWriter writer = new CSVWriter(new FileWriter(csvOutputFile), separatorChar);
            for (List<String> currentLine : lines) {
                 writer.writeNext(currentLine.toArray(new String[currentLine.size()]));
            }
            writer.close();
            logger.info("Wrote CSV output to file " + csvOutputFile);
        }
        logger.info("Query " + sqlQuery + " returned " + resultCount + " rows and executed in " + queryTime + "ms.");
    }

    private static void outputResultSet(int[] columnWidths, List<List<String>> lines) {
        String line = getDisplayLine(columnWidths, lines.get(0));
        int totalLineLength = line.length();
        logger.info(line.toString());
        line = getDisplayLine(columnWidths, lines.get(1));
        logger.info(line.toString());
        logger.info(padToLength("", totalLineLength, "-"));
        for (int i=2; i < lines.size(); i++) {
            line = getDisplayLine(columnWidths, lines.get(i));
            logger.info(line.toString());
        }
        logger.info(padToLength("", totalLineLength, "-"));
    }

    private static String getDisplayLine(int[] columnWidths, List<String> line) {
        StringBuilder lineBuilder = new StringBuilder();
        int i=0;
        for (String column : line) {
            lineBuilder.append(padToLength(column, columnWidths[i], " "));
            lineBuilder.append("|");
            i++;
        }
        return lineBuilder.toString();
    }

    public static void executeSqlUpdate(String sqlUpdate, Connection connection) throws SQLException {
        long startTime = System.currentTimeMillis();
        Statement statement = connection.createStatement();
        int rowsUpdated = statement.executeUpdate(sqlUpdate);
        statement.close();
        connection.commit();
        long queryTime = System.currentTimeMillis() - startTime;
        logger.info("Query " + sqlUpdate + " updated " + rowsUpdated + " rows and executed in " + queryTime + "ms.");
    }

    public static void executeSql(String sqlStatement, Connection connection, File csvOutputFile, char separatorChar) throws SQLException, IOException {
        if (sqlStatement.toLowerCase().startsWith("select")) {
            executeSqlQuery(sqlStatement, connection, csvOutputFile, separatorChar);
        } else {
            executeSqlUpdate(sqlStatement, connection);
        }
    }

    /**
     * Get a Iterator containing all lines of the sql runtime from a
     * database script. This database script is getted in parameter like
     * a File object. The method use the BufferedReader object on a
     * FileReader object instanciate on the script file name.
     * @author  Alexandre Kraft
     *
     * @param   fileObject   File object of the database script file.
     * @return  Iterator containing all lines of the database script.
     */
    public static List<String> getScriptFileStatements( File fileObject ) throws IOException {
        List<String> scriptsRuntimeList  = new ArrayList<String>();

        BufferedReader buffered     = new BufferedReader( new FileReader(fileObject.getPath()) );
        String          buffer       = "";

        StringBuffer curSQLStatement = new StringBuffer();
        while((buffer = buffered.readLine()) != null)
        {
            if (buffer != null && buffer.trim().equals("/")) {
                // '/' indicates the end of the PL/SQL script for Oracle -> skip it here
                continue;
            }

            // let's check for comments.
            int commentPos = buffer.indexOf("#");
            if ((commentPos != -1) && (!isInQuotes(buffer, commentPos))) {
                buffer = buffer.substring(0, commentPos);
            }
            commentPos = buffer.indexOf("//");
            if ((commentPos != -1) && (!isInQuotes(buffer, commentPos))) {
                buffer = buffer.substring(0, commentPos);
            }
            commentPos = buffer.indexOf("/*");
            if ((commentPos != -1) && (!isInQuotes(buffer, commentPos))) {
                buffer = buffer.substring(0, commentPos);
            }
            commentPos = buffer.indexOf("REM ");
            if ((commentPos != -1) && (!isInQuotes(buffer, commentPos))) {
                buffer = buffer.substring(0, commentPos);
            }
            commentPos = buffer.indexOf("--");
            if ((commentPos != -1) && (!isInQuotes(buffer, commentPos))) {
                buffer = buffer.substring(0, commentPos);
            }

            // is the line after comment removal ?
            if (buffer.trim().length() == 0) {
                continue;
            }

            buffer = buffer.trim();

            if (buffer.endsWith(";")) {
                // found seperator char in the script file, finish constructing
                curSQLStatement.append(buffer.substring(0, buffer.endsWith("end;") ? buffer.length() : buffer.length()-1));
                String sqlStatement = curSQLStatement.toString().trim();
                if (!"".equals(sqlStatement)) {
                    // System.out.println("Found statement [" + sqlStatement + "]");
                    scriptsRuntimeList.add(sqlStatement);
                }
                curSQLStatement = new StringBuffer();
            } else {
                curSQLStatement.append(buffer);
                curSQLStatement.append('\n');
            }

        }
        String sqlStatement = curSQLStatement.toString().trim();
        if (!"".equals(sqlStatement)) {
            scriptsRuntimeList.add(sqlStatement);
        }
        buffered.close();

        return scriptsRuntimeList;
    }

    private static boolean isInQuotes(String sqlStatement, int pos) {
        if (pos < 0) {
            return false;
        }
        String beforeStr = sqlStatement.substring(0, pos);
        int quoteCount = 0;
        int curPos = 0;
        int quotePos = beforeStr.indexOf("'");
        while (quotePos != -1) {
            quoteCount++;
            curPos = quotePos +1;
            quotePos = beforeStr.indexOf("'", curPos);
        }
        if (quoteCount % 2 == 0) {
            return false;
        } else {
            return true;
        }
    }


}
