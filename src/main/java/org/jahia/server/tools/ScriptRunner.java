package org.jahia.server.tools;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Main bootstrap class
 */
public class ScriptRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);
    private static String jahiaInstallLocation;

    public static Options buildOptions() {

         Option threads = OptionBuilder.withArgName("dir")
                 .hasArg()
                 .withDescription("Jahia installation directory")
                 .withLongOpt("installationDirectory")
                 .create("d");

         Option help =
                 OptionBuilder.withDescription("Prints this help screen")
                 .withLongOpt("help")
                 .create("h");

         Options options = new Options();
         options.addOption(threads);
         options.addOption(help);
         return options;
     }

    public static void main(String[] args) {
        // create the parser
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            Options options = buildOptions();
            CommandLine line = parser.parse(options, args);
            String[] lineArgs = line.getArgs();
            if (line.hasOption("h")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "jahia-scriptrunner [options] project_directory [command]", options );
                return;
            }

            if (line.hasOption("d")) {
                jahiaInstallLocation = line.getOptionValue("d");
            } else {
                jahiaInstallLocation = System.getProperty("user.dir");
            }

            File jahiaInstallLocationFile = new File(jahiaInstallLocation);
            if (!jahiaInstallLocationFile.exists() && !jahiaInstallLocationFile.isDirectory()) {
                logger.error("Invalid jahia installation directory " + jahiaInstallLocationFile.getAbsolutePath());
                return;
            }

            String command = null;
            if (lineArgs.length >= 1) {
                StringBuffer commandBuffer = new StringBuffer();
                for (int i=0; i<lineArgs.length; i++) {
                    commandBuffer.append(lineArgs[i]);
                }
                command = commandBuffer.toString();
                if ("".equals(command)) {
                    command = null;
                }
            }
            File scriptFile = new File(command);

            List<URL> jahiaClassLoaderURLs = new ArrayList<URL>();

            File classesDirectory = new File(jahiaInstallLocationFile, "WEB-INF/classes");
            jahiaClassLoaderURLs.add(classesDirectory.toURI().toURL());

            File libDirectory = new File(jahiaInstallLocationFile, "WEB-INF/lib");
            File[] jarFiles = libDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File file, String name) {
                    if (name.toLowerCase().endsWith(".jar")) {
                        return true;
                    }
                    return false;
                }
            });
            for (File jarFile : jarFiles) {
                jahiaClassLoaderURLs.add(jarFile.toURI().toURL());
            }

            URLClassLoader urlClassLoader = new URLClassLoader(jahiaClassLoaderURLs.toArray(new URL[jahiaClassLoaderURLs.size()]), ScriptRunner.class.getClassLoader());
            Class inContextRunnerClass = urlClassLoader.loadClass("org.jahia.server.tools.InContextRunnerImpl");
            InContextRunner inContextRunner = (InContextRunner) inContextRunnerClass.newInstance();
            inContextRunner.run(jahiaInstallLocationFile, scriptFile, urlClassLoader);

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
        }

    }

}
