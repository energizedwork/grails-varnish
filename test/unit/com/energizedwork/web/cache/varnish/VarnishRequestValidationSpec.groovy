package com.energizedwork.web.cache.varnish

import spock.lang.Specification


class VarnishRequestValidationSpec extends Specification {

    def "request with non-matching varnish XIDs fails validation"() {
        given: "a varnish request"
            def request = new VarnishRequest()
            request.client['ReqStart'] = "192.168.1.219 52904 1771187247"
            request.client['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'
            request.backend['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'

        when: "the client has one XID"
            request.client['TxHeader'] = 'X-Varnish: 1771187247'

        and: "the backend has a different XID"
            request.backend['TxHeader'] = 'X-Varnish: 1771187248'

        then: "validation fails"
            !request.validate()
    }

    def "request with matching varnish XIDs passes validation"() {
        given: "a varnish request"
            def request = new VarnishRequest()
            request.client['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'
            request.backend['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'

        and: "a varnish XID"
            String xid = "1771187247"

        when: "the client has one XID"
            request.client['ReqStart'] = "192.168.1.219 52904 $xid"
            request.client['TxHeader'] = "X-Varnish: $xid"

        and: "the backend has the same XID"
            request.backend['TxHeader'] = "X-Varnish: $xid"

        then: "validation fails"
            request.validate()
    }

    def "request missing varnish XIDs fails validation"() {
        given: "a varnish request"
            def request = new VarnishRequest()
            request.client['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'
            request.backend['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'

        and: "a varnish XID"
            String xid = "1771187247"

        when: "only the client has an XID"
            request.client['ReqStart'] = "192.168.1.219 52904 $xid"
            request.client['TxHeader'] = "X-Varnish: $xid"

        then: "validation fails"
            !request.validate()
    }

    def "ReqStart and ReqEnd must have the same XIDs"() {
        given: "a varnish request"
            def request = new VarnishRequest()
            request.client['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'
            request.backend['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'

        when: "we add the ReqStart line"
            request.client['ReqStart'] = "192.168.1.219 52904 1771187247"

        and: "we add a request line with a different XID"
            request.client['ReqEnd'] = "1771187248 1294759488.219491005 1294759488.226368904 0.165027618 0.006854534 0.000023365"

        then: "an exception is thrown"
            thrown AssertionError
    }

    def "a cache hit without a backend call validates"() {
        given: "a varnish request"
            def request = new VarnishRequest()
            request.client['TxURL'] = '/script/js/jquery/jquery.qtip-1.0.0-rc3.js?d=1674666369'

        when: "we add the ReqStart line"
            request.client['ReqStart'] = "192.168.1.219 52904 1771187280"

        and: "we add the X-Varnish header"
            request.client['TxHeader'] = "X-Varnish: 1771187280 1771187184"

        and: "we finish the client request"
            request.client['ReqEnd'] = "1771187280 1294759492.230493784 1294759492.230621099 1.295840502 0.000055552 0.000071764"

        then: "the entire request validates"
            request.validate()
    }


}
