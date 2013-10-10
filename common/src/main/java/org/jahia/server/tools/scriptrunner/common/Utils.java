package org.jahia.server.tools.scriptrunner.common;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of static utility methods
 */
public class Utils {

    public static File[] getMatchingFiles(String wildcardPath) {
        String parentPath = null;
        String wildCardName = wildcardPath;
        int lastSlashPos = wildcardPath.lastIndexOf("/");
        if (lastSlashPos > -1) {
            parentPath = wildcardPath.substring(0, lastSlashPos);
            wildCardName = wildcardPath.substring(lastSlashPos + 1);
        }
        if (!wildCardName.contains("*")) {
            return new File[] { new File(wildcardPath) };
        }
        wildCardName = wildCardName.replaceAll("\\.", "\\\\.");
        wildCardName = wildCardName.replaceAll("\\*", ".*");
        File parentFile = new File(".");
        if (parentPath != null) {
            parentFile = new File(parentPath);
        }
        if (parentFile == null || !parentFile.exists() || !parentFile.isDirectory()) {
            return new File[0];
        }
        final Pattern matchingNamePattern = Pattern.compile(wildCardName);
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
            matchingFiles = new File[0];
        }
        return matchingFiles;
    }

}
