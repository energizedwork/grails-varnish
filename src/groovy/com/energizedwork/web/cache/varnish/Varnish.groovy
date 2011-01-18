package com.energizedwork.web.cache.varnish

import com.energizedwork.web.cache.WebCache
import com.energizedwork.web.cache.Service
import com.energizedwork.web.util.NetUtils
import com.energizedwork.web.util.ThreadUtils
import com.energizedwork.grails.util.GrailsHelper
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.apache.log4j.Logger
import com.energizedwork.web.cache.varnish.grails.GrailsVCLFileResolver


class Varnish implements WebCache {

    private boolean running

    Service cache, server    
    String protocol = 'http'

    Varnishd varnishd = new Varnishd(vclFileResolver:new GrailsVCLFileResolver())
    VarnishLog varnishlog

    Map getConfig() { varnishd.config }

    void start() {
        start(new Service(host:GrailsHelper.host, port:GrailsHelper.httpPort, protocol:protocol))
    }

    void start(Service server) {
        this.server = server
        this.cache = new Service(host:InetAddress.localHost.hostName, port:NetUtils.findFreePort(), protocol:protocol)

        varnishd.start(cache, server)
        varnishlog = new VarnishLog()
        varnishlog.start(varnishd)

        running = varnishd.running
    }

    void stop() {
        varnishd.stop()
        varnishlog?.stop()
    }

    boolean isRunning() { varnishd.running }

    String toString() {
        "WebCache:\n$varnishd"
    }
    
}

