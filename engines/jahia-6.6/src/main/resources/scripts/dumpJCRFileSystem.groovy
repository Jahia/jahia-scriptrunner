package scripts

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;

Logger logger = LoggerFactory.getLogger("dumpJCRFileSystem.groovy");

Connection connection = (Connection) jdbcConnection;

logger.info("Dumping contents of table jr_fsg_fsentry...");

PreparedStatement preparedStatement = connection.prepareStatement("SELECT FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, FSENTRY_LASTMOD, FSENTRY_LENGTH FROM JR_FSG_FSENTRY");

ResultSet resultSet = preparedStatement.executeQuery();

while (resultSet.next()) {
    String path = resultSet.getString(1);
    String name = resultSet.getString(2);
    long lastModified = resultSet.getLong(4);
    Date lastModifiedDate = new Date(lastModified);
    long length = resultSet.getLong(5);
    logger.info("path=" + path + " name=" + name + " lastModified=" + lastModifiedDate + " length=" + length);
    if (length > 0) {
        InputStream data = resultSet.getBinaryStream(3);
        if (name.toLowerCase().endsWith(".properties")) {
            Properties properties = new Properties();
            properties.load(data);
            for (String propertyName : properties.stringPropertyNames()) {
                logger.info("--> " + propertyName + " = " + properties.getProperty(propertyName));
            }
        } else if (name.toLowerCase().endsWith(".xml")) {
            if (Boolean.getBoolean("dumpXML").booleanValue()) {
                fileReader = new InputStreamReader(data);
                SAXBuilder saxBuilder = new SAXBuilder();
                Document jdomDocument = saxBuilder.build(fileReader);
                XMLOutputter xmlOutputter = new XMLOutputter();
                xmlOutputter.setFormat(Format.getPrettyFormat());
                xmlOutputter.output(jdomDocument, System.out);
            } else {
                logger.info("--> Use option -XdumpXML=true to dump contents of XML file")
            }
        }
        data.close();
    }
}

resultSet.close();
preparedStatement.close();
