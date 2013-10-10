package scripts

import org.apache.jackrabbit.core.fs.FileSystem
import org.apache.jackrabbit.core.fs.FileSystemResource
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.jdom.Document
import org.jdom.input.SAXBuilder
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger logger = LoggerFactory.getLogger("dumpJCRFileSystem.groovy");

Properties groovyScriptOptions = (Properties) scriptOptions;

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(scriptRunnerConfiguration);

FileSystem repositoryFileSystem = jackrabbitHelper.getRepositoryFileSystem();

dumpFolderContents("/", repositoryFileSystem, jackrabbitHelper, groovyScriptOptions, logger)

private void dumpFileContents(String name, InputStream data, Properties scriptOptions, Logger logger) {
    if (name.toLowerCase().endsWith(".properties")) {
        Properties properties = new Properties();
        properties.load(data);
        for (String propertyName : properties.stringPropertyNames()) {
            logger.info("--> " + propertyName + " = " + properties.getProperty(propertyName));
        }
    } else if (name.toLowerCase().endsWith(".xml")) {
        if (scriptOptions.getProperty("dumpXML") != null &&
            "true".equals(scriptOptions.getProperty("dumpXML"))) {
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

private void dumpFolderContents(String folderPath, FileSystem repositoryFileSystem, JackrabbitHelper jackrabbitHelper, Properties scriptOptions, Logger logger) {
    String[] folderContents = repositoryFileSystem.list(folderPath);
    for (String folderContent : folderContents) {
        String folderContentPath = folderPath + folderContent;
        if (!folderPath.endsWith("/")) {
            folderContentPath = folderPath + "/" + folderContent;
        }
        FileSystemResource folderResource = new FileSystemResource(jackrabbitHelper.getRepositoryFileSystem(), folderContentPath);
        if (repositoryFileSystem.isFile(folderContentPath)) {
            logger.info("path=" + folderResource.getPath() + " name=" + folderResource.getName() + " lastModified=" + folderResource.lastModified() + " length=" + folderResource.length());
            dumpFileContents(folderResource.getName(), folderResource.getInputStream(), scriptOptions, logger);
        } else if (repositoryFileSystem.isFolder(folderContentPath)) {
            logger.info("path=" + folderResource.getPath() + " name=" + folderResource.getName() + " lastModified=" + folderResource.lastModified());
            dumpFolderContents(folderContentPath, repositoryFileSystem, jackrabbitHelper, scriptOptions, logger);
        } else {
            logger.warn("Unrecognized file system entry " + folderContentPath + ". It is neither a file or a folder !");
        }
    }
}
