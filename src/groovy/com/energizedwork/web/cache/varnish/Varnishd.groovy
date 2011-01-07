package com.energizedwork.web.cache.varnish

import com.energizedwork.web.cache.Service
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.energizedwork.web.util.ThreadUtils
import org.apache.log4j.Logger
import com.energizedwork.web.util.NetUtils


class Varnishd {

    Logger log = Logger.getLogger(Varnishd)

    private Service cache, server
    private boolean stop
    private Process varnishd
    private Thread logger

    String command = '/usr/sbin/varnishd'
    String commandLine
    File configFile
    int managementPort
    long sleepTime = 500
    long timeoutInMillis = 10000
    File workingDirectory

    StringBuffer stdout, stderr

    boolean isRunning() { varnishd }

    void start(Service cache, Service server) {
        if(varnishd) { throw new IllegalStateException() }

        this.cache = cache
        this.server = server

        configure()

        log.info "Starting varnishd : $commandLine"

        varnishd = commandLine.execute()
        (stdout, stderr) = logOutputFromProcess(varnishd)

        waitForSuccessfulStartup() ?: fail()
    }

    void stop() {
        if(!stop) {
            stop = true
            manage("stop").waitFor()
            logger.join()
            varnishd.destroy()
            varnishd.waitFor()
            varnishd = null

            workingDirectory.deleteDir()
        }
    }

    Process manage(String command) {
        "varnishadm -T$cache.host:$managementPort $command".execute()
    }

    private addBackendToConfigFileIfNoneAreSpecified() {
        if(configFile) {
            boolean containsBackend
            configFile.eachLine { String line ->
                if(line =~ /^\s*backend\s.*/) { containsBackend = true }                
            }

            if(!containsBackend) {
                String backendSection = """
backend default {
     .host = "$server.host";
     .port = "$server.port";
}
"""
                File newConfig = File.createTempFile('grails', 'vcl', workingDirectory)
                newConfig.text = backendSection
                newConfig << configFile.text
                configFile = newConfig
            }
        }
    }
    
    private String buildCommandLine(Service cache, Service server) {
        def args = ["-F", "-a $cache.host:$cache.port",
                "-T $cache.host:$managementPort",
                "-n ${workingDirectory.absolutePath}"]

        if(configFile) {
            args << "-f $configFile.absolutePath"
        } else {
            args << "-b $server.host:$server.port"
        }

        ([command] + args).join(' ')
    }

    private configure() {
        managementPort = NetUtils.findFreePort()
        configFile = findConfigFile()
        createWorkingDirectoryIfNoneSpecified()
        addBackendToConfigFileIfNoneAreSpecified()
        commandLine = buildCommandLine(cache, server)        
    }

    private createWorkingDirectoryIfNoneSpecified() {
        if(!workingDirectory) {
            workingDirectory = File.createTempFile('grails', 'varnish')
            workingDirectory.delete()
            workingDirectory.mkdirs()
        }
    }

    private fail() {
        log.error "Failed to start varnish daemon: $commandLine"
        log.error stdout
        log.error stderr
        stop()
    }

    private File findConfigFile() {
        File config
        String configValue = ConfigurationHolder.config?.varnish?.vcl?.file
        if(configValue) {
            File file = new File(configValue)
            config = file.exists() ? file : null
        }
        config
    }
    
    private logOutputFromProcess(Process process) {
        StringBuffer out = new StringBuffer()
        StringBuffer err = new StringBuffer()

        logger = Thread.start {
            while(!stop) {
                ThreadUtils.stfu process.consumeProcessOutputStream(out)
                ThreadUtils.stfu process.consumeProcessErrorStream(err)
                Thread.sleep sleepTime
            }
        }

        [out, err]
    }

    private boolean waitForSuccessfulStartup() {
        boolean success
        long startTime = System.currentTimeMillis()
        long currentTime = System.currentTimeMillis()

        while(!success && (currentTime - startTime) < timeoutInMillis) {
            Thread.sleep(sleepTime)

            if(stderr.indexOf('VCL compilation failed') > -1) {
                throw new VCLCompilationException(stderr.toString())
            }

            Process varnishadm = manage("status")
            success = (varnishadm.waitFor() == 0)

            currentTime = System.currentTimeMillis()
        }

        success
    }

}
