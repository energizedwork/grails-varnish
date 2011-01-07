package com.energizedwork.web.cache.varnish

import com.energizedwork.web.cache.WebCache
import com.energizedwork.web.cache.Service
import com.energizedwork.web.util.NetUtils
import com.energizedwork.web.util.ThreadUtils
import com.energizedwork.grails.util.GrailsHelper
import org.codehaus.groovy.grails.commons.ConfigurationHolder


class Varnish implements WebCache {

    private boolean running

    Service cache, server    
    String protocol = 'http'

    Varnishd varnishd = new Varnishd()

    void start() {
        start(new Service(host:GrailsHelper.host, port:GrailsHelper.httpPort, protocol:protocol))
    }

    void start(Service server) {
        this.server = server
        this.cache = new Service(host:InetAddress.localHost.hostName, port:NetUtils.findFreePort(), protocol:protocol)

        varnishd.start(cache, server)

        running = varnishd.running
    }

    void stop() { varnishd.stop() }

    boolean isRunning() { varnishd.running }
    
}

class Varnishd {

    private Service cache
    private boolean stop
    private Process varnishd
    private Thread logger

    String command = '/usr/sbin/varnishd'
    String commandLine
    long timeoutInMillis = 10000
    int managementPort
    long sleepTime = 500
    File workingDirectory
    File configFile
        
    StringBuffer stdout, stderr

    boolean isRunning() { varnishd }

    void start(Service cache, Service server) {
        if(varnishd) { throw new IllegalStateException() }

        this.cache = cache

        managementPort = NetUtils.findFreePort()
        configFile = findConfigFile()
        commandLine = buildCommandLine(cache, server)
        varnishd = commandLine.execute()
        (stdout, stderr) = logOutputFromProcess(varnishd)

        waitForSuccessfulStartup() ?: stop()
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

    def logOutputFromProcess(Process process) {
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

    boolean waitForSuccessfulStartup() {
        boolean success
        long startTime = System.currentTimeMillis()
        long currentTime = System.currentTimeMillis()

        while(!success && (currentTime - startTime) < timeoutInMillis) {
            Thread.sleep(sleepTime)

            Process varnishadm = manage("status")
            success = (varnishadm.waitFor() == 0)

            currentTime = System.currentTimeMillis()
        }

        success
    }

    String buildCommandLine(Service cache, Service server) {
        def args = ["-F", "-a $cache.host:$cache.port",
                "-T $cache.host:$managementPort",
                "-n ${getWorkingDirectory().absolutePath}"]

        if(configFile) {
            args << "-f $configFile.absolutePath"
        } else {
            args << "-b $server.host:$server.port"
        }

        ([command] + args).join(' ')
    }    

    File getWorkingDirectory() {
        if(!this.workingDirectory) {
            this.workingDirectory = File.createTempFile('grails', 'varnish')
            this.workingDirectory.delete()
            this.workingDirectory.mkdirs()
        }
        this.workingDirectory
    }

    File findConfigFile() {
        File config
        String configValue = ConfigurationHolder.config?.varnish?.vcl?.file
        if(configValue) {
            File file = new File(configValue)
            config = file.exists() ? file : null
        }
        config
    }

    Process manage(String command) {
        "varnishadm -T$cache.host:$managementPort $command".execute()
    }

}
