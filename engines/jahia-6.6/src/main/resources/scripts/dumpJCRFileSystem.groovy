package scripts

import org.apache.jackrabbit.core.fs.FileSystemResource
import org.apache.jackrabbit.core.fs.FileSystem
import org.jahia.server.tools.scriptrunner.engines.common.DatabaseConfiguration
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;

Logger logger = LoggerFactory.getLogger("dumpJCRFileSystem.groovy");

DatabaseConfiguration dbConfiguration = (DatabaseConfiguration) databaseConfiguration;

File jahiaInstallLocation = (File) jahiaInstallLocationFile;
File jackrabbitConfigFile = new File(jahiaInstallLocation, "WEB-INF" + File.separator + "etc" + File.separator + "repository" + File.separator + "jackrabbit" + File.separator + "repository.xml");
File jackrabbitHomeDir = new File(jahiaInstallLocation, "WEB-INF" + File.separator + "var" + File.separator + "repository");;

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(jackrabbitConfigFile, jackrabbitHomeDir, dbConfiguration, false, false);

FileSystem repositoryFileSystem = jackrabbitHelper.getRepositoryFileSystem();

dumpFolderContents("/", repositoryFileSystem, jackrabbitHelper, logger)

private void dumpFileContents(String name, InputStream data, Logger logger) {
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
            logger.info("--> Use option -x dumpXML=true to dump contents of XML file")
        }
    }
}

private void dumpFolderContents(String folderPath, FileSystem repositoryFileSystem, JackrabbitHelper jackrabbitHelper, Logger logger) {
    String[] folderContents = repositoryFileSystem.list(folderPath);
    for (String folderContent : folderContents) {
        String folderContentPath = folderPath + folderContent;
        if (!folderPath.endsWith("/")) {
            folderContentPath = folderPath + "/" + folderContent;
        }
        FileSystemResource folderResource = new FileSystemResource(jackrabbitHelper.getRepositoryFileSystem(), folderContentPath);
        if (repositoryFileSystem.isFile(folderContentPath)) {
            logger.info("path=" + folderResource.getPath() + " name=" + folderResource.getName() + " lastModified=" + folderResource.lastModified() + " length=" + folderResource.length());
            dumpFileContents(folderResource.getName(), folderResource.getInputStream(), logger);
        } else if (repositoryFileSystem.isFolder(folderContentPath)) {
            logger.info("path=" + folderResource.getPath() + " name=" + folderResource.getName() + " lastModified=" + folderResource.lastModified());
            dumpFolderContents(folderContentPath, repositoryFileSystem, jackrabbitHelper, logger);
        } else {
            logger.warn("Unrecognized file system entry " + folderContentPath + ". It is neither a file or a folder !");
        }
    }
}
