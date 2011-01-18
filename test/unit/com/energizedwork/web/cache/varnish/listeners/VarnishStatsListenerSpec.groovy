package com.energizedwork.web.cache.varnish.listeners

import spock.lang.Specification
import com.energizedwork.web.cache.varnish.VarnishLogParser
import com.energizedwork.web.cache.varnish.VarnishRequestListener


class VarnishStatsListenerSpec extends Specification {
        
    def 'log parser can parse client request urls'() {
        given: 'a varnish log parser with a single listener'
            def parser = new VarnishLogParser()
            def listener = Mock(VarnishRequestListener)
            def stats = new VarnishStatsListener()
            parser.addListener listener
            parser.addListener stats

        when: 'we hand it a full client request'
            parser.parse new File('test/resources/varnishlog-stdout.txt').text

        then: 'it notifies the listeners of a request'
            102 * listener.request(_)

        and: 'the stats are as expected'
            1 == stats.stats['/']
            1 == stats.stats['/weblog']
            1 == stats.stats['/favicon.png']
            1 == stats.stats['/software']
    }

}
