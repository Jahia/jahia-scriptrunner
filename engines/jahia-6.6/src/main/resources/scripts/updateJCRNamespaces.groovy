package scripts

import org.apache.jackrabbit.core.fs.FileSystemResource
import org.jahia.server.tools.scriptrunner.engines.jahia66.JackrabbitHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger logger = LoggerFactory.getLogger("updateJCRNamespaces.groovy");

// first let's load the name space files into memory

JackrabbitHelper jackrabbitHelper = new JackrabbitHelper(scriptRunnerConfiguration);

FileSystemResource namespaceRegistryFile = new FileSystemResource(
        jackrabbitHelper.getRepositoryFileSystem(), "/namespaces/ns_reg.properties");
FileSystemResource namespaceIndexFile = new FileSystemResource(
        jackrabbitHelper.getRepositoryFileSystem(), "/namespaces/ns_idx.properties");

InputStream nsRegStream = namespaceRegistryFile.getInputStream();
Properties nsReg = new Properties();
nsReg.load(nsRegStream);
nsRegStream.close();
InputStream nsIdxStream = namespaceIndexFile.getInputStream();
Properties nsIdx = new Properties();
nsIdx.load(nsIdxStream);
nsIdxStream.close();

// now let's perform the modifications
Properties groovyScriptOptions = (Properties) scriptOptions;
if (groovyScriptOptions.containsKey("namespaceOperation") &&
        groovyScriptOptions.containsKey("namespace")) {
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
            // Before removing the index entry, let's check there there is no more remaining references to this uri
            // in the ns_reg.properties table.
            if (nsReg.containsValue(namespaceUri)) {
                logger.warn("Remaining references to the uri=" + namespaceUri + " in the ns_reg.properties file, will not remove from ns_idx.properties file");
            } else {
                nsIdx.remove(namespaceUri);
                logger.info("Removed namespace uri=" + namespaceUri + " from ns_idx.properties");
            }
        }
    }
} else {
    logger.error("Missing namespaceOperation and namespace command line options. Will not update the DB file system namespace entries !");
    return;
}

// finally let's save them back to the DB file system.

OutputStream outputStream = namespaceRegistryFile.getOutputStream();
nsReg.store(outputStream, null);
outputStream.close();

outputStream = namespaceIndexFile.getOutputStream();
nsIdx.store(outputStream, null);
outputStream.close();

private Integer getIndex(Properties nsIdx, String uri) {
    // Need to use only 24 bits, since that's what
    // the BundleBinding class stores in bundles
    Integer idx = uri.hashCode() & 0x00ffffff;
    while (nsIdx.containsValue(idx.toString())) {
        idx = (idx + 1) & 0x00ffffff;
    }
    return idx;
}