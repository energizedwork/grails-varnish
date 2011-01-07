class VarnishGrailsPlugin {
    
    def version = "0.1-SNAPSHOT"
    def grailsVersion = "1.3.5 > *"
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Gus Power"
    def authorEmail = "gus@energizedwork.com"
    def title = "Grails Varnish HTTP Accelerator plugin"
    def description = '''\\
This plugin aims to seamlessly integrate the Varnish HTTP accelerator into grails, both at runtime and
in functional testing situations
'''

    def documentation = "http://grails.org/plugin/varnish"

    def doWithWebDescriptor = { xml -> }
    def doWithSpring = {}
    def doWithDynamicMethods = { ctx -> }
    def doWithApplicationContext = { applicationContext -> }
    def onChange = { event -> }
    def onConfigChange = { event -> }
    
}
