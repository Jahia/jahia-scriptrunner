package org.jahia.server.tools.scriptrunner.common;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

/**
 * An interface to execute implementation inside the context of the Jahia classloader
 */
public interface InContextRunner {

    public boolean run(ScriptRunnerConfiguration scriptRunnerConfiguration, String scriptName, InputStream scriptStream, ClassLoader classLoader);

}
