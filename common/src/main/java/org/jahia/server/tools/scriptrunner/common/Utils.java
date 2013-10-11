package org.jahia.server.tools.scriptrunner.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of static utility methods
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static File[] getMatchingFiles(final String filePath, List<File> tempFileToDelete, File tempDirectoryFile) {

        // let's start with default values
        String matchingFilePath = filePath;
        String insideJarPath = null;
        String parentPath = null;
        String fileName = matchingFilePath;

        // if there is no exclamation point or no wildcard, we simply return the file as is.
        if (!fileName.contains("*") && !fileName.contains("!")) {
            return new File[] { new File(matchingFilePath) };
        }

        // let's check if there is an exclamation point marking an inside JAR path.
        int exclamationPos = matchingFilePath.indexOf("!");
        if (exclamationPos > -1) {
            insideJarPath = matchingFilePath.substring(exclamationPos+1);
            insideJarPath = insideJarPath.replaceAll("\\.", "\\\\.");
            insideJarPath = insideJarPath.replaceAll("\\*", ".*");
            matchingFilePath = matchingFilePath.substring(0, exclamationPos);
            fileName = matchingFilePath;
        }

        // now let's extract the file name
        int lastSlashPos = matchingFilePath.lastIndexOf("/");
        if (lastSlashPos > -1) {
            parentPath = matchingFilePath.substring(0, lastSlashPos);
            fileName = matchingFilePath.substring(lastSlashPos + 1);
        }

        fileName = fileName.replaceAll("\\.", "\\\\.");
        fileName = fileName.replaceAll("\\*", ".*");
        File parentFile = new File(".");
        if (parentPath != null) {
            parentFile = new File(parentPath);
        }
        if (!parentFile.exists() || !parentFile.isDirectory()) {
            return new File[0];
        }
        final Pattern matchingNamePattern = Pattern.compile(fileName);
        File[] matchingFiles = parentFile.listFiles(new FilenameFilter() {
            public boolean accept(File file, String name) {
                Matcher nameMatcher = matchingNamePattern.matcher(name);
                if (nameMatcher.matches()) {
                    return true;
                }
                return false;
            }
        });
        if (matchingFiles == null) {
            return new File[0];
        }
        List<File> realMatchingFiles = new ArrayList<File>();
        Pattern insideJarPattern = null;
        if (insideJarPath != null) {
            insideJarPattern = Pattern.compile(insideJarPath);
        }
        for (File matchingFile : matchingFiles) {
            if (matchingFile.getName().toLowerCase().endsWith(".jar") && insideJarPattern != null) {
                JarInputStream jarInputStream = null;
                try {
                    jarInputStream = new JarInputStream(new FileInputStream(matchingFile));
                    JarEntry jarEntry = null;
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        Matcher insideJarMatcher = insideJarPattern.matcher(jarEntry.getName());
                        if (insideJarMatcher.matches() && !jarEntry.isDirectory()) {
                            // we found a matching JarEntry, we must now extract it so we can use it on
                            // the class path since the JVM doesn't support embedded JARs in a class path
                            File extractedFile = extractToTemp(jarInputStream, jarEntry.getName(), tempFileToDelete, tempDirectoryFile);
                            realMatchingFiles.add(extractedFile);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(jarInputStream);
                }
            } else {
                realMatchingFiles.add(matchingFile);
            }
        }
        return realMatchingFiles.toArray(new File[realMatchingFiles.size()]);
    }

    public static File extractToTemp(InputStream fileInputStream, String fileName, List<File> tempFilesToDelete, File tempDirectoryFile) throws IOException {
        int lastSlashPos = fileName.lastIndexOf("/");
        if (lastSlashPos > -1) {
            fileName = fileName.substring(lastSlashPos + 1);
        }
        File destFile = new File(tempDirectoryFile, fileName);
        logger.info("Extracting embedded file " + fileName + " to " + destFile);
        FileOutputStream destFileOutputStream = new FileOutputStream(destFile);
        IOUtils.copy(fileInputStream, destFileOutputStream);
        destFileOutputStream.close();
        tempFilesToDelete.add(destFile);
        return destFile;
    }

}
