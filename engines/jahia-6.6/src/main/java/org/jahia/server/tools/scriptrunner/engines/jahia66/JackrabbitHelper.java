package org.jahia.server.tools.scriptrunner.engines.jahia66;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.db.DbFileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;
import java.io.*;

/**
 * A collection of helper methods to make it quicker to develop scripts that manipulate Jackrabbit data structure
 */
public class JackrabbitHelper {

    private static final Logger logger = LoggerFactory.getLogger(JackrabbitHelper.class);

    public static DataSource getDataSource(ConnectionFactory connectionFactory, String driver, String url, String user, String password) throws Exception {
        return connectionFactory.getDataSource(driver, url, user, password);
    }

    public static NodeId loadRootNodeId(org.apache.jackrabbit.core.fs.FileSystem fileSystem) throws RepositoryException {
        final NodeId ROOT_NODE_ID = NodeId.valueOf("cafebabe-cafe-babe-cafe-babecafebabe");

        try {
            FileSystemResource uuidFile = new FileSystemResource(
                    fileSystem, "/meta/rootUUID");
            if (uuidFile.exists()) {
                // Load uuid of the repository's root node. It is stored in
                // text format (36 characters) for better readability.
                InputStream inputStream = uuidFile.getInputStream();
                try {
                    return NodeId.valueOf(IOUtils.toString(inputStream, "US-ASCII"));
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            } else {
                // Use hard-coded uuid for root node rather than generating
                // a different uuid per repository instance; using a
                // hard-coded uuid makes it easier to copy/move entire
                // workspaces from one repository instance to another.
                uuidFile.makeParentDirs();
                OutputStream out = uuidFile.getOutputStream();
                try {
                    out.write(ROOT_NODE_ID.toString().getBytes("US-ASCII"));
                    return ROOT_NODE_ID;
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        } catch (IOException e) {
            throw new RepositoryException(
                    "Failed to load or persist the root node identifier", e);
        } catch (FileSystemException fse) {
            throw new RepositoryException(
                    "Failed to access the root node identifier", fse);
        }
    }

    public static Element getRepositoryXmlRootElement(File jahiaInstallLocation) {
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            InputStreamReader fileReader = new InputStreamReader(new FileInputStream(new File(jahiaInstallLocation, "WEB-INF" + File.separator + "etc" + File.separator + "repository" + File.separator + "jackrabbit" + File.separator + "repository.xml")));
            org.jdom.Document jdomDocument = saxBuilder.build(fileReader);
            Element rootElement = jdomDocument.getRootElement();
            return rootElement;
        } catch (FileNotFoundException e) {
            logger.error("Error loading database configuration", e);
        } catch (JDOMException e) {
            logger.error("Error loading database configuration", e);
        } catch (IOException e) {
            logger.error("Error loading database configuration", e);
        }
        return null;
    }

    ;

    public static BundleDbPersistenceManager getPersistenceManagerClass(Element rootElement) {
        Element persistenceManagerElement = null;
        try {
            persistenceManagerElement = (Element) XPath.newInstance("/Repository/Workspace/PersistenceManager").selectSingleNode(rootElement);
        } catch (JDOMException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
            return null;
        }
        String persistenceManagerClassName = persistenceManagerElement.getAttributeValue("class");
        Class persistenceManagerClass = null;
        BundleDbPersistenceManager bundleDbPersistenceManager = null;
        try {
            persistenceManagerClass = JackrabbitHelper.class.getClassLoader().loadClass(persistenceManagerClassName);
            bundleDbPersistenceManager = (BundleDbPersistenceManager) persistenceManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        } catch (InstantiationException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        } catch (IllegalAccessException e) {
            logger.error("Error retrieving persistence manager class from Jackrabbit repository configuration", e);
        }
        return bundleDbPersistenceManager;
    }

    public static DbFileSystem getDbFileSystem(Element rootElement) {
        Element fileSystemElement = null;
        try {
            fileSystemElement = (Element) XPath.newInstance("/Repository/FileSystem").selectSingleNode(rootElement);
        } catch (JDOMException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        }
        String fileSystemClassName = fileSystemElement.getAttributeValue("class");
        Class fileSystemClass = null;
        DbFileSystem dbFileSystem = null;
        try {
            fileSystemClass = JackrabbitHelper.class.getClassLoader().loadClass(fileSystemClassName);
            dbFileSystem = (DbFileSystem) fileSystemClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        } catch (InstantiationException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        } catch (IllegalAccessException e) {
            logger.error("Error retrieving db file system class from Jackrabbit repository configuration", e);
        }
        return dbFileSystem;
    }

}
