package org.jahia.server.tools.scriptrunner;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jahia.commons.Version;
import org.jahia.server.tools.scriptrunner.common.InContextRunner;
import org.jahia.server.tools.scriptrunner.common.ScriptRunnerConfiguration;
import org.jahia.server.tools.scriptrunner.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main bootstrap class
 */
public class ScriptRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);
    private static Version scriptRunnerVersion;
    private static String scriptRunnerBuildNumber;
    private File tempDirectory;
    private List<File> tempFilesToDelete = new ArrayList<File>();
    private static ScriptRunner instance = new ScriptRunner();

    public static ScriptRunner getInstance() {
        return instance;
    }

    public static Options buildOptions() {

        Option configFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("The configuration file to use to setup the Jahia script runner")
                .withLongOpt("configFile")
                .create("c");

        Option targetDirectory = OptionBuilder.withArgName("dir")
                .hasArg()
                .withDescription("Target base directory")
                .withLongOpt("baseDirectory")
                .create("d");

        Option scriptOptions = OptionBuilder.withArgName("scriptOptions")
                .hasArg()
                .withDescription("A comma separated list of key=value options to pass to the script")
                .withLongOpt("scriptOptions")
                .create("x");

        Option jahiaVersion = OptionBuilder.withArgName("version")
                .hasArg()
                .withDescription("Overrides the automatic version detection and specify a version using this command line option")
                .withLongOpt("targetVersion")
                .create("v");

        Option listAvailableScripts = OptionBuilder
                .withDescription("Outputs the list of built-in available scripts for this Jahia version")
                .withLongOpt("listScripts")
                .create("l");

        Option help =
                OptionBuilder.withDescription("Prints this help screen")
                        .withLongOpt("help")
                        .create("h");

        Options options = new Options();
        options.addOption(configFile);
        options.addOption(targetDirectory);
        options.addOption(scriptOptions);
        options.addOption(jahiaVersion);
        options.addOption(listAvailableScripts);
        options.addOption(help);
        return options;
    }

    public static void main(String[] args) {
        // create the parser
        CommandLineParser parser = new PosixParser();
        try {
            displayStartupBanner();

            // parse the command line arguments
            Options options = buildOptions();
            CommandLine line = parser.parse(options, args);
            String[] lineArgs = line.getArgs();
            if (line.hasOption("h")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("jahia-scriptrunner [options] script_to_launch", options);
                return;
            }

            String configFileLocation = null;
            if (line.hasOption("c")) {
                configFileLocation = line.getOptionValue("c");
            }

            String baseDirectory = null;
            if (line.hasOption("d")) {
                baseDirectory = line.getOptionValue("d");
                if (baseDirectory.endsWith("/")) {
                    baseDirectory = baseDirectory.substring(0, baseDirectory.length()-1);
                }
            }

            String command = null;
            File scriptFile = null;
            if (lineArgs.length >= 1) {
                StringBuffer commandBuffer = new StringBuffer();
                for (int i = 0; i < lineArgs.length; i++) {
                    commandBuffer.append(lineArgs[i]);
                }
                command = commandBuffer.toString();
                if ("".equals(command)) {
                    command = null;
                }
                if (command != null) {
                    scriptFile = new File(command);
                }
            }

            String scriptOptionList = null;
            if (line.hasOption("x")) {
                scriptOptionList = line.getOptionValue("x");
            }

            String specificEngineVersion = null;
            if (line.hasOption("v")) {
                specificEngineVersion = line.getOptionValue("v");
            }

            boolean listBuiltInScripts = false;
            if (line.hasOption("l")) {
                listBuiltInScripts = true;
            }

            getInstance().executeScript(specificEngineVersion, configFileLocation, baseDirectory, command, scriptFile, scriptOptionList, listBuiltInScripts);

        } catch (ParseException exp) {
            // oops, something went wrong
            logger.error("Parsing failed.  Reason: ", exp);
        } catch (MalformedURLException e) {
            logger.error("Malformed URL ", e);
        } catch (ClassNotFoundException e) {
            logger.error("Class not found ", e);
        } catch (InstantiationException e) {
            logger.error("Error instantiating class", e);
        } catch (IllegalAccessException e) {
            logger.error("Illegal access", e);
        } catch (Exception e) {
            logger.error("Error", e);
        }

    }

    private boolean executeScript(final String specificEngineVersion, final String configFileLocation, final String baseDirectory, final String command, final File scriptFile, final String scriptOptionList, boolean listBuiltInScripts) throws Exception {

        ScriptRunnerConfiguration configuration = getConfiguration(configFileLocation, baseDirectory);
        if (scriptOptionList != null) {
            configuration.setScriptOptions(scriptOptionList);
        }

        tempDirectory = new File(configuration.getTempDirectory());
        tempDirectory.mkdirs();

        File baseDirectoryFile = new File(configuration.getBaseDirectory());
        if (!baseDirectoryFile.exists() && !baseDirectoryFile.isDirectory()) {
            logger.error("Invalid target directory " + baseDirectoryFile.getAbsolutePath());
            return false;
        }

        List<URL> classLoaderURLs = new ArrayList<URL>();

        // first we look for the Script Runner's jars
        // here because of http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4735639 that is still not fixed (!)
        // we have to resort to extract the JARs to a temporary directory
        String projectVersion = getScriptRunnerVersion().toString();
        ClassLoader appClassLoader = ScriptRunner.class.getClassLoader();
        URL commonEngineJarUrl = appClassLoader.getResource("libs/jahia-scriptrunner-engines-common-" + projectVersion + ".jar");
        URL extractedCommonEngineJarUrl = extractToTemp(commonEngineJarUrl).toURI().toURL();
        classLoaderURLs.add(extractedCommonEngineJarUrl);

        // resolve the target engine JAR, possibly resolving using intelligent resolving 6.6.1.1 -> 6.6.1 -> 6.6
        String engineVersion = configuration.getEngineDefaultVersion();
        if (specificEngineVersion != null) {
            engineVersion = specificEngineVersion;
        } else {
            Version jahiaImplementationVersion = null;
            File[] versionMatchingFiles = Utils.getMatchingFiles(configuration.getVersionDetectionJar(), tempFilesToDelete, tempDirectory);
            if (versionMatchingFiles != null && versionMatchingFiles.length > 0) {
                if (versionMatchingFiles.length > 1) {
                    logger.warn("More than one JAR was matched by wildcard " + configuration.getVersionDetectionJar() + ", will only use first match !");
                }
                File file = versionMatchingFiles[0];
                JarFile jarFile = new JarFile(file);
                Attributes mainAttributes = jarFile.getManifest().getMainAttributes();
                String implementationVersion = mainAttributes.getValue(configuration.getVersionDetectionVersionAttributeName());
                jahiaImplementationVersion = new Version(implementationVersion);
                engineVersion = implementationVersion;
                String implementationBuild = mainAttributes.getValue(configuration.getVersionDetectionBuildAttributeName());
                logger.info("Detected "+configuration.getEngineDisplayName()+" v" + jahiaImplementationVersion + " build number " + implementationBuild);
            }
        }
        URL engineJarUrl = appClassLoader.getResource("libs/jahia-scriptrunner-engines-"+configuration.getEngineName()+"-" + engineVersion + "-" + projectVersion + ".jar");
        while (engineJarUrl == null && engineVersion.length() > 0) {
            int lastDotPos = engineVersion.lastIndexOf(".");
            if (lastDotPos > -1) {
                engineVersion = engineVersion.substring(0, lastDotPos);
                engineJarUrl = appClassLoader.getResource("libs/jahia-scriptrunner-engines-"+configuration.getEngineName()+"-" + engineVersion + "-" + projectVersion + ".jar");
            } else {
                engineVersion = "";
            }
        }
        if (engineVersion.length() > 0) {
            logger.info("Using script engine v" + engineVersion);
        } else {
            logger.error("Couldn't find any engine for the specified target version, aborting !");
            return true;
        }
        URL extractedEngineJarUrl = extractToTemp(engineJarUrl).toURI().toURL();
        classLoaderURLs.add(extractedEngineJarUrl);

        classLoaderURLs.addAll(getTargetClassLoaderURLs(configuration.getClassPath()));

        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderURLs.toArray(new URL[classLoaderURLs.size()]), ScriptRunner.class.getClassLoader());
        if (listBuiltInScripts) {
            InputStream scriptClassLoaderStream = urlClassLoader.getResourceAsStream("scripts/availableScripts.properties");
            if (scriptClassLoaderStream == null) {
                logger.error("Couldn't find a built-in script list !");
            }
            Properties availableScripts = new Properties();
            availableScripts.load(scriptClassLoaderStream);
            logger.info("Available built-in scripts:");
            for (String availableScriptName : availableScripts.stringPropertyNames()) {
                logger.info("    " + availableScriptName + " : " + availableScripts.getProperty(availableScriptName));
            }
            return true;
        }
        String scriptName = null;
        InputStream scriptStream = null;
        if (scriptFile != null && !scriptFile.exists()) {
            InputStream scriptClassLoaderStream = urlClassLoader.getResourceAsStream("scripts/" + command);
            if (scriptClassLoaderStream == null) {
                logger.error("Couldn't find a built-in script named" + command + ", aborting !");
                return true;
            }
            scriptName = command;
            scriptStream = scriptClassLoaderStream;
        } else {
            if (scriptFile != null) {
                scriptName = scriptFile.getName();
                scriptStream = new FileInputStream(scriptFile);
            }
        }
        if (scriptStream != null) {
            Class inContextRunnerClass = urlClassLoader.loadClass("org.jahia.server.tools.scriptrunner.engines.common.InContextRunnerImpl");
            InContextRunner inContextRunner = (InContextRunner) inContextRunnerClass.newInstance();
            inContextRunner.run(configuration, scriptName, scriptStream, urlClassLoader);
        } else {
            logger.error("Couldn't resolve any script to run, aborting !");
        }

        logger.info("Cleaning up...");
        for (File tempFileToDelete : tempFilesToDelete) {
            tempFileToDelete.delete();
        }
        return false;
    }

    public static void displayStartupBanner() throws Exception {
        String message = "==========================================================================================\n" +
                         "Jahia Script Runner v" + getScriptRunnerVersion() + " build " + getScriptRunnerBuildNumber() + " (c) 2013 All Rights Reserved.     \n" +
                         "==========================================================================================\n";
        System.out.println(message);
    }

    public static Version getScriptRunnerVersion() throws Exception {
        if (scriptRunnerVersion != null) {
            return scriptRunnerVersion;
        }
        Package scriptRunnerPackage = ScriptRunner.class.getPackage();
        if (scriptRunnerPackage != null) {
            scriptRunnerVersion = new Version(scriptRunnerPackage.getImplementationVersion());
            return scriptRunnerVersion;
        }
        throw new Exception("Couldn't resolve ScriptRunner version !");
    }

    public static String getScriptRunnerBuildNumber() throws Exception {
        if (scriptRunnerBuildNumber != null) {
            return scriptRunnerBuildNumber;
        }
        Package scriptRunnerPackage = ScriptRunner.class.getPackage();
        if (scriptRunnerPackage != null) {
            Enumeration<URL> manifestEnum = ScriptRunner.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifestEnum.hasMoreElements() && scriptRunnerBuildNumber == null) {
                URL manifestURL = manifestEnum.nextElement();
                InputStream manifestStream = manifestURL.openStream();
                Manifest manifest = new Manifest(manifestStream);
                Attributes mainAttributes = manifest.getMainAttributes();
                String implementationVendorId = mainAttributes.getValue("Implementation-Vendor-Id");
                if ("org.jahia.server.tools.scriptrunner".equals(implementationVendorId)) {
                    scriptRunnerBuildNumber = mainAttributes.getValue("Implementation-Timestamp");
                }
                manifestStream.close();
            }
            return scriptRunnerBuildNumber;
        }
        throw new Exception("Couldn't resolve ScriptRunner build number !");
    }

    public File extractToTemp(URL resourceURL) throws IOException {
        String fileName = resourceURL.getFile();
        int lastSlashPos = fileName.lastIndexOf("/");
        if (lastSlashPos > -1) {
            fileName = fileName.substring(lastSlashPos + 1);
        }
        File destFile = new File(tempDirectory, fileName);
        logger.info("Extracting resource " + resourceURL + " to " + destFile);
        FileUtils.copyInputStreamToFile(resourceURL.openStream(), destFile);
        tempFilesToDelete.add(destFile);
        return destFile;
    }

    public static ScriptRunnerConfiguration getConfiguration(String configFileLocation, String baseDirectory) {
        ScriptRunnerConfiguration configuration = new ScriptRunnerConfiguration();
        Properties defaultConfigurationProperties = new Properties(System.getProperties());
        InputStream defaultConfigFileInputStream = ScriptRunner.class.getClassLoader().getResourceAsStream("scriptRunner.properties");
        try {
            defaultConfigurationProperties.load(defaultConfigFileInputStream);
        } catch (IOException e) {
            logger.error("Error loading default built-in properties", e);
        } finally {
            IOUtils.closeQuietly(defaultConfigFileInputStream);
        }

        applyProperties(configuration, defaultConfigurationProperties, baseDirectory);

        File scriptConfigFile = null;
        if (configFileLocation != null) {
            scriptConfigFile = new File(configFileLocation);
            if (!scriptConfigFile.exists()) {
                scriptConfigFile = null;
            }
        }
        if (scriptConfigFile == null) {
            scriptConfigFile = new File("scriptRunner.properties");
            if (!scriptConfigFile.exists()) {
                scriptConfigFile = null;
            }
        }
        Properties configurationProperties = new Properties(defaultConfigurationProperties);
        InputStream configFileInputStream = null;
        if (scriptConfigFile != null) {
            try {
                logger.info("Using configuration from file " + scriptConfigFile);
                configFileInputStream = new FileInputStream(scriptConfigFile);
                configurationProperties.load(configFileInputStream);
            } catch (FileNotFoundException e) {
                logger.error("Error loading properties from " + scriptConfigFile, e);
            } catch (IOException e) {
                logger.error("Error loading properties from " + scriptConfigFile, e);
            } finally {
                IOUtils.closeQuietly(configFileInputStream);
            }

        }
        applyProperties(configuration, configurationProperties, baseDirectory);

        return configuration;
    }

    private static void applyProperties(ScriptRunnerConfiguration configuration, Properties configurationProperties, String baseDirectory) {
        if (baseDirectory != null) {
            configurationProperties.setProperty("baseDirectory", baseDirectory);
        } else {
            if (configurationProperties.getProperty("baseDirectory") == null) {
                configurationProperties.setProperty("baseDirectory", System.getProperty("user.dir"));
            }
        }
        resolveProperties(configurationProperties);

        try {
            BeanUtils.populate(configuration, configurationProperties);
        } catch (IllegalAccessException e) {
            logger.error("Error applying loaded properties to configuration bean", e);
        } catch (InvocationTargetException e) {
            logger.error("Error applying loaded properties to configuration bean", e);
        }
    }


    private static void resolveProperties(Properties configurationProperties) {
        for (String propertyName : configurationProperties.stringPropertyNames()) {
            String propertyValue = resolvePropertyValue(configurationProperties, propertyName);
            if (propertyValue != null &&
                    !propertyValue.equals(configurationProperties.getProperty(propertyName))) {
                configurationProperties.setProperty(propertyName, propertyValue);
            }
        }
    }

    private static String resolvePropertyValue(Properties configurationProperties, String propertyName) {
        String propertyValue = configurationProperties.getProperty(propertyName);
        String propertyRefName = null;
        while ((propertyRefName = getPropertyReference(propertyValue)) != null) {
            if (configurationProperties.getProperty(propertyRefName) != null) {
                propertyValue = propertyValue.replace("${" + propertyRefName + "}", resolvePropertyValue(configurationProperties, propertyRefName));
            } else {
                break;
            }
        }
        return propertyValue;
    }

    private static String getPropertyReference(String value) {
        int markerStart = value.indexOf("${");
        if (markerStart < 0) {
            return null;
        }
        int markerEnd = value.indexOf("}", markerStart+"${".length());
        if (markerEnd < 0) {
            return null;
        }
        return value.substring(markerStart+"${".length(), markerEnd);
    }

    private List<URL> getTargetClassLoaderURLs(String targetClassPath) {
        String[] classPathParts = targetClassPath.split(",");
        List<URL> classLoaderURLs = new ArrayList<URL>();

        for (String classPathPart : classPathParts) {
            File[] matchingFiles = Utils.getMatchingFiles(classPathPart, tempFilesToDelete, tempDirectory);
            for (File matchingFile : matchingFiles) {
                try {
                    if (matchingFile.exists()) {
                        classLoaderURLs.add(matchingFile.toURI().toURL());
                    } else {
                        logger.warn("Class path location " + matchingFile + " does not exist, ignoring it !");
                    }
                } catch (MalformedURLException e) {
                    logger.error("Error transforming file to URL " + e);
                }
            }
        }
        return classLoaderURLs;
    }

}
