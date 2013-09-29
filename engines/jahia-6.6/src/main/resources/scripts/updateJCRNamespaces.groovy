package scripts

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

Logger logger = LoggerFactory.getLogger("updateJCRNamespaces.groovy");

Connection connection = (Connection) jdbcConnection;

// first let's load the name space files into memory

InputStream nsRegStream = getDBFile("/namespaces", "ns_reg.properties", connection);
Properties nsReg = new Properties();
nsReg.load(nsRegStream);
nsRegStream.close();
InputStream nsIdxStream = getDBFile("/namespaces", "ns_idx.properties", connection);
Properties nsIdx = new Properties();
nsIdx.load(nsIdxStream);
nsIdxStream.close();

// now let's perform the modifications
Properties groovyScriptOptions = (Properties) scriptOptions;
if (groovyScriptOptions.containsKey("namespaceOperation") != null &&
        groovyScriptOptions.containsKey("namespace") != null) {
    String namespaceOperation = groovyScriptOptions.getProperty("namespaceOperation").toLowerCase();
    String namespace = groovyScriptOptions.getProperty("namespace");
    String namespacePrefix = null;
    String namespaceUri = namespace;
    int colonPos = namespace.indexOf(":");
    if (colonPos > -1) {
        namespacePrefix = namespace.substring(0, colonPos);
        namespaceUri = namespace.substring(colonPos+1);
    }
    if ("add".equals(namespaceOperation)) {
        if (!nsReg.containsKey(namespacePrefix)) {
            nsReg.setProperty(namespacePrefix, namespaceUri);
            logger.info("Added namespace prefix=" + namespacePrefix + " uri=" + namespaceUri + " to ns_reg.properties");
        }
        if (!nsIdx.containsKey(namespaceUri)) {
            Integer index = getIndex(nsIdx, namespaceUri);
            nsIdx.setProperty(namespaceUri, index.toString());
            logger.info("Added namespace uri=" + namespaceUri + " index=" + index + " to ns_idx.properties");
        }
    } else if ("remove".equals(namespaceOperation)) {
        if (nsReg.containsKey(namespacePrefix)) {
            nsReg.remove(namespacePrefix);
            logger.info("Removed namespace prefix=" + namespacePrefix + " uri=" + namespaceUri + " from ns_reg.properties");
        }
        if (nsIdx.containsKey(namespaceUri)) {
            nsIdx.remove(namespaceUri);
            logger.info("Removed namespace uri=" + namespaceUri + " from ns_idx.properties");
        }
    }
} else {
    logger.error("Missing namespaceOperation and namespace command line options. Will not update the DB file system namespace entries !");
    return;
}

// finally let's save them back to the DB file system.

ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
nsReg.store(byteArrayOutputStream, null);
byte[] byteArray = byteArrayOutputStream.toByteArray();
ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
int count = setDBFile(true, "/namespaces", "ns_reg.properties", System.currentTimeMillis(), byteArray.length, byteArrayInputStream, connection);
logger.info("Updated " + count + " DB file system entries");
byteArrayInputStream.close();

byteArrayOutputStream = new ByteArrayOutputStream();
nsIdx.store(byteArrayOutputStream, null);
byteArray = byteArrayOutputStream.toByteArray();
byteArrayInputStream = new ByteArrayInputStream(byteArray);
count = setDBFile(true, "/namespaces", "ns_idx.properties", System.currentTimeMillis(), byteArray.length, byteArrayInputStream, connection);
logger.info("Updated " + count + " DB file system entries");
byteArrayInputStream.close();

private InputStream getDBFile(String path, String name, Connection connection) {
    PreparedStatement preparedStatement = connection.prepareStatement("SELECT FSENTRY_DATA, FSENTRY_LASTMOD, FSENTRY_LENGTH FROM JR_FSG_FSENTRY WHERE FSENTRY_PATH=? AND FSENTRY_NAME=?");
    preparedStatement.setString(1, path);
    preparedStatement.setString(2, name);

    ResultSet resultSet = preparedStatement.executeQuery();

    InputStream result = null;
    while (resultSet.next()) {
        long lastModified = resultSet.getLong(2);
        Date lastModifiedDate = new Date(lastModified);
        long length = resultSet.getLong(3);
        if (length > 0) {
            InputStream data = resultSet.getBinaryStream(1);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(data, byteArrayOutputStream);
            result = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }
    }

    resultSet.close();
    preparedStatement.close();

    return result;
}

private int setDBFile(boolean alreadyExists, String path, String name, long lastModifiedTime, long length, InputStream inputStream, Connection connection) {
    PreparedStatement preparedStatement = null;
    int count = 0;

    if (alreadyExists) {
        preparedStatement = connection.prepareStatement("UPDATE JR_FSG_FSENTRY SET FSENTRY_DATA=?, FSENTRY_LASTMOD=?, FSENTRY_LENGTH=? WHERE FSENTRY_PATH=? AND FSENTRY_NAME=?");
        preparedStatement.setBinaryStream(1, inputStream);
        preparedStatement.setLong(2, lastModifiedTime);
        preparedStatement.setLong(3, length);
        preparedStatement.setString(4, path);
        preparedStatement.setString(5, name);
        count = preparedStatement.executeUpdate()
    } else {
        preparedStatement = connection.prepareStatement("INSERT INTO JR_FSG_FSENTRY (FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, FSENTRY_LASTMOD, FSENTRY_LENGTH) VALUES (?, ?, ?, ?, ?)");
        preparedStatement.setString(1, path);
        preparedStatement.setString(2, name);
        preparedStatement.setBinaryStream(3, inputStream);
        preparedStatement.setLong(4, lastModifiedTime);
        preparedStatement.setLong(5, length);
        count = preparedStatement.executeUpdate();
    }

    return count;
}

private Integer getIndex(Properties nsIdx, String uri) {
    // Need to use only 24 bits, since that's what
    // the BundleBinding class stores in bundles
    Integer idx = uri.hashCode() & 0x00ffffff;
    while (nsIdx.containsValue(idx.toString())) {
        idx = (idx + 1) & 0x00ffffff;
    }
    return idx;
}