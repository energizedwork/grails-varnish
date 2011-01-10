package com.energizedwork.grails.util

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper


class GrailsHelper {

    static BuildSettings getBuildSettings() {
        if (!BuildSettingsHolder.settings) {
            BuildSettingsHolder.settings = new BuildSettings(null, new File("."))
        }
        BuildSettingsHolder.settings
    }

    static ConfigObject getConfig() {
        ConfigObject config = ConfigurationHolder.config

        if(!config) {
            File configDotGroovy = new File(buildSettings.baseDir, 'grails-app/conf/Config.groovy')
            if(configDotGroovy.exists()) {
                config = new ConfigSlurper().parse(configDotGroovy.toURI().toURL())
                ConfigurationHelper.initConfig(config)
                ConfigurationHolder.config = config
            }
        }

        return ConfigurationHolder.config
    }

    static int getHttpPort() {
        int result = findPropertyValue("server.port", 8080)
        findPropertyValue("grails.server.port.http", result)
    }

    static int getHttpsPort() {
        int result = findPropertyValue("server.port.https", 8443)
        findPropertyValue("grails.server.port.https", result)
    }

    static String getHost() {
        String result = findPropertyValue("server.host", "localhost")
        findPropertyValue("grails.server.host", result)
    }

    private static findPropertyValue(String name, defaultValue) {
        def value = System.getProperty(name)
        if (value != null) { return value }
        if(buildSettings.hasProperty(name)) {
            value = buildSettings[name]
        }
        value ?: defaultValue
    }

}
