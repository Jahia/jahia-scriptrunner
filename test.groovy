import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test.groovy");

Connection connection = (Connection) jdbcConnection;

System.out.println("Hello World !");

PreparedStatement preparedStatement = connection.prepareStatement("SELECT FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, FSENTRY_LASTMOD, FSENTRY_LENGTH FROM jr_fsg_fsentry");

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
            logger.info("--> " + properties);
        }
        data.close();
    }
}

resultSet.close();
preparedStatement.close();
