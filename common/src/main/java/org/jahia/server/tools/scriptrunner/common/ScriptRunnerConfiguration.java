package org.jahia.server.tools.scriptrunner.common;

/**
 * Main configuration bean for Script Runner.
 */
public class ScriptRunnerConfiguration {

    String baseDirectory;
    String engineName;
    String engineDisplayName;
    String classPath;
    String engineDefaultVersion;

    String tempDirectory;

    String versionDetectionJar;
    String versionDetectionVersionAttributeName;
    String versionDetectionBuildAttributeName;

    String dbConfigurationSource;
    String dbDerbySystemHome;
    String dbDriverClassName;
    String dbUrl;
    String dbUserName;
    String dbPassword;
    String dbDatabaseType;

    String scriptOptions;

    String jackrabbitConfigFile;
    String jackrabbitHomeDirectory;
    boolean jackrabbitConsistencyCheck;
    boolean jackrabbitConsistencyFix;

    public ScriptRunnerConfiguration() {
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineDisplayName() {
        return engineDisplayName;
    }

    public void setEngineDisplayName(String engineDisplayName) {
        this.engineDisplayName = engineDisplayName;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getEngineDefaultVersion() {
        return engineDefaultVersion;
    }

    public void setEngineDefaultVersion(String engineDefaultVersion) {
        this.engineDefaultVersion = engineDefaultVersion;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public String getVersionDetectionJar() {
        return versionDetectionJar;
    }

    public void setVersionDetectionJar(String versionDetectionJar) {
        this.versionDetectionJar = versionDetectionJar;
    }

    public String getVersionDetectionVersionAttributeName() {
        return versionDetectionVersionAttributeName;
    }

    public void setVersionDetectionVersionAttributeName(String versionDetectionVersionAttributeName) {
        this.versionDetectionVersionAttributeName = versionDetectionVersionAttributeName;
    }

    public String getVersionDetectionBuildAttributeName() {
        return versionDetectionBuildAttributeName;
    }

    public void setVersionDetectionBuildAttributeName(String versionDetectionBuildAttributeName) {
        this.versionDetectionBuildAttributeName = versionDetectionBuildAttributeName;
    }

    public String getDbDriverClassName() {
        return dbDriverClassName;
    }

    public void setDbDriverClassName(String dbDriverClassName) {
        this.dbDriverClassName = dbDriverClassName;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbDatabaseType() {
        return dbDatabaseType;
    }

    public void setDbDatabaseType(String dbDatabaseType) {
        this.dbDatabaseType = dbDatabaseType;
    }

    public String getDbConfigurationSource() {
        return dbConfigurationSource;
    }

    public void setDbConfigurationSource(String dbConfigurationSource) {
        this.dbConfigurationSource = dbConfigurationSource;
    }

    public String getDbDerbySystemHome() {
        return dbDerbySystemHome;
    }

    public void setDbDerbySystemHome(String dbDerbySystemHome) {
        this.dbDerbySystemHome = dbDerbySystemHome;
    }

    public String getScriptOptions() {
        return scriptOptions;
    }

    public void setScriptOptions(String scriptOptions) {
        this.scriptOptions = scriptOptions;
    }

    public String getJackrabbitConfigFile() {
        return jackrabbitConfigFile;
    }

    public void setJackrabbitConfigFile(String jackrabbitConfigFile) {
        this.jackrabbitConfigFile = jackrabbitConfigFile;
    }

    public String getJackrabbitHomeDirectory() {
        return jackrabbitHomeDirectory;
    }

    public void setJackrabbitHomeDirectory(String jackrabbitHomeDirectory) {
        this.jackrabbitHomeDirectory = jackrabbitHomeDirectory;
    }

    public boolean isJackrabbitConsistencyCheck() {
        return jackrabbitConsistencyCheck;
    }

    public void setJackrabbitConsistencyCheck(boolean jackrabbitConsistencyCheck) {
        this.jackrabbitConsistencyCheck = jackrabbitConsistencyCheck;
    }

    public boolean isJackrabbitConsistencyFix() {
        return jackrabbitConsistencyFix;
    }

    public void setJackrabbitConsistencyFix(boolean jackrabbitConsistencyFix) {
        this.jackrabbitConsistencyFix = jackrabbitConsistencyFix;
    }
}
