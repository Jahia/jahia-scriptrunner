package org.jahia.server.tools.scriptrunner;

import java.io.File;

/**
 * An interface to execute implementation inside the context of the Jahia classloader
 */
public interface InContextRunner {

    public boolean run(File jahiaInstallLocationFile, File scriptFile, ClassLoader classLoader);

}
