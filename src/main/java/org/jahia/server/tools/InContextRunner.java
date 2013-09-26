package org.jahia.server.tools;

/**
 * An interface to execute implementation inside the context of the Jahia classloader
 */
public interface InContextRunner {

    public boolean run();

}
