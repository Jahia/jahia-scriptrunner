package org.jahia.server.tools.scriptrunner.common;

/**
 * Main configuration bean for Script Runner.
 */
public class ScriptRunnerConfiguration {

    String baseDirectory;
    String targetName;
    String targetDisplayName;
    String targetClassPath;
    String targetDefaultVersion;
    String targetDBConfigurationSource;
    String targetDerbySystemHome;

    String tempDirectory;

    String versionDetectionJar;
    String versionDetectionVersionAttributeName;
    String versionDetectionBuildAttributeName;

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

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetDisplayName() {
        return targetDisplayName;
    }

    public void setTargetDisplayName(String targetDisplayName) {
        this.targetDisplayName = targetDisplayName;
    }

    public String getTargetClassPath() {
        return targetClassPath;
    }

    public void setTargetClassPath(String targetClassPath) {
        this.targetClassPath = targetClassPath;
    }

    public String getTargetDefaultVersion() {
        return targetDefaultVersion;
    }

    public void setTargetDefaultVersion(String targetDefaultVersion) {
        this.targetDefaultVersion = targetDefaultVersion;
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

    public String getTargetDBConfigurationSource() {
        return targetDBConfigurationSource;
    }

    public void setTargetDBConfigurationSource(String targetDBConfigurationSource) {
        this.targetDBConfigurationSource = targetDBConfigurationSource;
    }

    public String getTargetDerbySystemHome() {
        return targetDerbySystemHome;
    }

    public void setTargetDerbySystemHome(String targetDerbySystemHome) {
        this.targetDerbySystemHome = targetDerbySystemHome;
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
