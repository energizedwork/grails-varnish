package com.energizedwork.web.cache.varnish

import com.energizedwork.web.cache.Service
import com.energizedwork.web.util.ThreadUtils
import com.energizedwork.web.util.NetUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ConfigurationHolder


class Varnishd {

    Logger log = Logger.getLogger(Varnishd)

    private Service cache, server
    private Thread logger
    private long sleepTime = 500
    private boolean stop
    private long timeoutInMillis = 10000
    private Process varnishd

    String command = '/usr/sbin/varnishd'
    String commandLine
    File vclFile, templateConfigFile
    int managementPort
    File workingDirectory
    
    VCLFileResolver vclFileResolver

    StringBuffer stdout, stderr

    boolean isRunning() { varnishd }

    Map getConfig() {
        [command:command, commandLine:commandLine, vclFile:vclFile, managementPort:managementPort,
            vclTemplate: templateConfigFile, workingDirectory:workingDirectory, url:cache.url, backendUrl: server.url]
    }

    void start(Service cache, Service server) {
        if(varnishd) { throw new IllegalStateException() }

        this.cache = cache
        this.server = server

        configure()

        varnishd = commandLine.execute()
        (stdout, stderr) = logOutputFromProcess(varnishd)

        waitForSuccessfulStartup() ?: fail()

        if(log.isInfoEnabled()) {
            log.info toString()
        }        
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

    String toString() {
        def result = new StringBuilder()
        result << 'Varnish HTTP Accelerator\n'
        result << "\tstatus: ${running ? 'running': 'stopped'}\n"
        config.each { key, value ->
            result << "\t$key: $value\n"            
        }
        result
    }

    private addBackendToConfigFileIfNoneAreSpecified() {
        if(vclFile) {
            boolean containsBackend
            vclFile.eachLine { String line ->
                if(line =~ /^\s*backend\s.*/) { containsBackend = true }                
            }

            if(!containsBackend) {
                String backendSection = """
backend default {
     .host = "$server.host";
     .port = "$server.port";
}
"""
                File newConfig = File.createTempFile('varnish', 'vcl', workingDirectory)
                newConfig.text = backendSection
                newConfig << vclFile.text
                templateConfigFile = vclFile
                vclFile = newConfig
            }
        }
    }
    
    private String buildCommandLine(Service cache, Service server) {
        def args = ["-F", "-a $cache.host:$cache.port",
                "-T $cache.host:$managementPort",
                "-n ${workingDirectory.absolutePath}"]

        if(vclFile) {
            args << "-f $vclFile.absolutePath"
        } else {
            args << "-b $server.host:$server.port"
        }

        ([command] + args).join(' ')
    }

    private configure() {
        managementPort = NetUtils.findFreePort()
        createWorkingDirectoryIfNoneSpecified()
        if(!vclFile) { vclFile = vclFileResolver?.vclFile }
        addBackendToConfigFileIfNoneAreSpecified()
        commandLine = buildCommandLine(cache, server)        
    }

    private createWorkingDirectoryIfNoneSpecified() {
        if(!workingDirectory) {
            workingDirectory = File.createTempFile('varnish', 'varnishd')
            workingDirectory.delete()
            workingDirectory.mkdirs()
        }
    }

    private fail() {
        log.error "Failed to start varnish daemon:\n $this"
        log.error stdout
        log.error stderr
        stop()
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
