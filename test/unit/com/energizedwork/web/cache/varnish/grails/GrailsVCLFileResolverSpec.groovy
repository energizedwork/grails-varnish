package com.energizedwork.web.cache.varnish.grails

import spock.lang.Specification
import org.codehaus.groovy.grails.commons.ConfigurationHolder


class GrailsVCLFileResolverSpec extends Specification {

    def setup() {
        ConfigurationHolder.config = null
    }

    def cleanup() {
        ConfigurationHolder.config = null
    }

    def "no vcl file is returned if no configuration exists"() {
        given: 'the file resolver'
            def resolver = new GrailsVCLFileResolver()

        when: 'we retrieve the file from it'
            File actual = resolver.VCLFile

        then: 'we get a null response'
            !actual        
    }

    def "resolver returns null if vcl file specified in config does not actually exist"() {
        given: 'the file resolver'
            def resolver = new GrailsVCLFileResolver()

        and: 'a configuration pointing to a non-existent vcl file'
            ConfigObject config = new ConfigObject()
            config.varnish.vcl.file = '/path/to/somewhere/that/doesnt/exist.vcl'
            ConfigurationHolder.config = config

        when: 'we ask the resolver for the vcl file'
            File actual = resolver.VCLFile

        then: 'we get a null response'
            !actual
    }

    def "resolver returns correct vcl file specified in config"() {
        given: 'the file resolver'
            def resolver = new GrailsVCLFileResolver()

        and: 'a configuration pointing to an existing vcl file'
            File vclFile = new File('.', 'test/resources/test.vcl')
            ConfigObject config = new ConfigObject()
            config.varnish.vcl.file = vclFile.absolutePath
            ConfigurationHolder.config = config

        when: 'we ask the resolver for the vcl file'
            File actual = resolver.VCLFile

        then: 'we get the correct vcl file in response'
            actual.absolutePath == vclFile.absolutePath
            actual.exists()
    }

}
