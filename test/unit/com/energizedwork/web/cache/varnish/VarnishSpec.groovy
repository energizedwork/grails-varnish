package com.energizedwork.web.cache.varnish

import groovyx.net.http.*

import com.energizedwork.web.cache.WebCache
import spock.lang.Specification
import com.energizedwork.web.cache.Service
import com.energizedwork.web.util.NetUtils
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.apache.log4j.BasicConfigurator


class VarnishSpec extends Specification {

    WebCache webCache

    def setupSpec() {
        BasicConfigurator.configure()
    }

    def cleanup() {
        webCache?.stop()
        ConfigurationHolder.config = null
    }

    def "can start and stop varnish without a project vcl"() {
        given: "a webcache with no vcl file"
            webCache = new Varnish()

        when: "we start it"
            Service server = new Service(host:'localhost', port:NetUtils.findFreePort())
            webCache.start(server)

        then: "it calculates a non-zero port"
            0 < webCache.cache.port

        and: "determines the localhost name"
            webCache.cache.host

        and: "comes up correctly"
            webCache.running

        and: "returns service unavailable for the backend server"
            503 == hitUrl(webCache.cache.url, '/')

        and: "we can stop it cleanly"
            webCache.stop()
            !webCache.running

        and: "it cleans up the working directory afterwards"
            !webCache.config.workingDirectory.exists()
    }

    def "can determine server from grails project settings"() {
        given: "a webcache with no vcl file"
            webCache = new Varnish()

        when: "we start it without a specified server"
            webCache.start()

        then: "the server settings are determined from the grails config"
            Thread.sleep 60000
            8080 == webCache.server.port
            'localhost' == webCache.server.host
    }

    def "trying to start running server throws an exception"() {
        given: "a webcache with no vcl file"
            webCache = new Varnish()

        when: "we start it without a specified server"
            webCache.start()
            webCache.start()

        then: "it will throw an exception when attempt is made to start it again"
            thrown(IllegalStateException)
    }

    def "uses vcl file specified in config if present"() {
        given: "a vcl file specified in config"
            def config = new ConfigObject()
            config.varnish.vcl.file = new File('.', 'test/resources/test.vcl').absolutePath
            ConfigurationHolder.config = config

        when: "we start it up"
            webCache = new Varnish()
            webCache.start()

        then: "it uses the vcl file"
            Process varnishadm = webCache.varnishd.manage("vcl.show boot")
            varnishadm.text.contains 'EnergizedWork varnish plugin test configuration'        
    }

    def "copies vcl file to working directory and adds backend if no backend present"() {
        given: "a vcl file specified in config"
            def config = new ConfigObject()
            config.varnish.vcl.file = new File('.', 'test/resources/nobackend.vcl').absolutePath
            ConfigurationHolder.config = config

        when: "we start it up"
            webCache = new Varnish()
            webCache.start()

        then: "it creates a new vcl file in the working directory"
            webCache.varnishd.vclFile.parentFile == webCache.varnishd.workingDirectory

        and: "it uses the original vcl file as a template"
            Process varnishadm = webCache.varnishd.manage("vcl.show boot")
            varnishadm.text.contains 'EnergizedWork varnish plugin no-backend test configuration'
    }

    def "catches vcl compilation failures"() {
        given: "a vcl file that will create compilation errors"
            def config = new ConfigObject()
            config.varnish.vcl.file = new File('.', 'test/resources/invalid.vcl').absolutePath
            ConfigurationHolder.config = config

        when: "we try to start it up"
            webCache = new Varnish()
            webCache.start()

        then: "it throws a compilation exception"
            thrown(VCLCompilationException)
    }

    private int hitUrl(String url, String path) {
        int result
        try {
            result = new RESTClient(url).head(path:path).status
        } catch(HttpResponseException ex) {
            result = ex.statusCode
        }
        result
    }

}
