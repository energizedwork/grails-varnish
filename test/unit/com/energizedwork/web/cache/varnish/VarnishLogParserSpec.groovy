package com.energizedwork.web.cache.varnish

import spock.lang.Specification


class VarnishLogParserSpec extends Specification {

    final String BACKEND_FETCH = '''
   19 TxRequest    - GET
   19 TxURL        - /
   19 TxProtocol   - HTTP/1.1
   19 TxHeader     - Host: animal.energylab.ew:38128
   19 TxHeader     - User-Agent: Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101110 Gentoo Firefox/3.6.12
   19 TxHeader     - Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
   19 TxHeader     - Accept-Language: en-gb,en;q=0.5
   19 TxHeader     - Accept-Encoding: gzip,deflate
   19 TxHeader     - Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
   19 TxHeader     - X-Forwarded-For: 192.168.1.219
   19 TxHeader     - X-Varnish: 1771187184
   19 RxProtocol   - HTTP/1.1
   19 RxStatus     - 200
   19 RxResponse   - OK
   19 RxHeader     - Content-Type: text/html; charset=utf-8
   19 RxHeader     - Content-Language: en-GB
   19 RxHeader     - Content-Length: 8913
   19 RxHeader     - Server: Jetty(6.1.21)
   19 Length       - 8913
   19 BackendReuse - default
   12 SessionOpen  c 192.168.1.219 52901 animal.energylab.ew:38128
   12 ReqStart     c 192.168.1.219 52901 1771187184
   12 RxRequest    c GET
   12 RxURL        c /
   12 RxProtocol   c HTTP/1.1
   12 RxHeader     c Host: animal.energylab.ew:38128
   12 RxHeader     c User-Agent: Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101110 Gentoo Firefox/3.6.12
   12 RxHeader     c Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
   12 RxHeader     c Accept-Language: en-gb,en;q=0.5
   12 RxHeader     c Accept-Encoding: gzip,deflate
   12 RxHeader     c Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
   12 RxHeader     c Keep-Alive: 115
   12 RxHeader     c Connection: keep-alive
   12 VCL_call     c recv lookup
   12 VCL_call     c hash hash
   12 VCL_call     c miss fetch
   12 Backend      c 19 default default
   12 TTL          c 1771187184 RFC 120 1294759486 0 0 0 0
   12 VCL_call     c fetch deliver
   12 ObjProtocol  c HTTP/1.1
   12 ObjStatus    c 200
   12 ObjResponse  c OK
   12 ObjHeader    c Content-Type: text/html; charset=utf-8
   12 ObjHeader    c Content-Language: en-GB
   12 ObjHeader    c Server: Jetty(6.1.21)
   12 VCL_call     c deliver deliver
   12 TxProtocol   c HTTP/1.1
   12 TxStatus     c 200
   12 TxResponse   c OK
   12 TxHeader     c Content-Type: text/html; charset=utf-8
   12 TxHeader     c Content-Language: en-GB
   12 TxHeader     c Server: Jetty(6.1.21)
   12 TxHeader     c Content-Length: 8913
   12 TxHeader     c Date: Tue, 11 Jan 2011 15:24:46 GMT
   12 TxHeader     c X-Varnish: 1771187184
   12 TxHeader     c Age: 0
   12 TxHeader     c Via: 1.1 varnish
   12 TxHeader     c Connection: keep-alive
   12 Length       c 8913
   12 ReqEnd       c 1771187184 1294759485.988468409 1294759486.049083710 0.000043392 0.060565472 0.000049829
'''

    final String CACHE_HIT = '''
   12 ReqStart     c 192.168.1.219 52901 1771187188
   12 RxRequest    c GET
   12 RxURL        c /image/53777/bg-left.png
   12 RxProtocol   c HTTP/1.1
   12 RxHeader     c Host: animal.energylab.ew:38128
   12 RxHeader     c User-Agent: Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101110 Gentoo Firefox/3.6.12
   12 RxHeader     c Accept: image/png,image/*;q=0.8,*/*;q=0.5
   12 RxHeader     c Accept-Language: en-gb,en;q=0.5
   12 RxHeader     c Accept-Encoding: gzip,deflate
   12 RxHeader     c Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
   12 RxHeader     c Keep-Alive: 115
   12 RxHeader     c Connection: keep-alive
   12 RxHeader     c Referer: http://animal.energylab.ew:38128/style/gzip_N363763480/style/app.css
   12 VCL_call     c recv lookup
   12 VCL_call     c hash hash
   12 Hit          c 1771187163
   12 VCL_call     c hit deliver
   12 VCL_call     c deliver deliver
   12 TxProtocol   c HTTP/1.1
   12 TxStatus     c 200
   12 TxResponse   c OK
   12 TxHeader     c Expires: Tue, 01 Jan 2013 00:00:00 GMT
   12 TxHeader     c Content-Type: image/png
   12 TxHeader     c Server: Jetty(6.1.21)
   12 TxHeader     c Content-Length: 53777
   12 TxHeader     c Date: Tue, 11 Jan 2011 15:24:46 GMT
   12 TxHeader     c X-Varnish: 1771187188 1771187163
   12 TxHeader     c Age: 155
   12 TxHeader     c Via: 1.1 varnish
   12 TxHeader     c Connection: keep-alive
   12 Length       c 53777
   12 ReqEnd       c 1771187188 1294759486.075134516 1294759486.075240612 0.005517006 0.000028849 0.000077248
'''

    def 'log parser can parse request that was cached'() {
        given: 'a varnish log parser with a single listener'
            def parser = new VarnishLogParser()
            def request
            def listener = [request: { request = it }] as VarnishRequestListener
            parser.addListener listener

        when: 'we hand it a full client request'
            parser.parse CACHE_HIT

        then: 'it notifies the listeners of a request'
            request

        and: 'the request url is /image/53777/bg-left.png'
            '/image/53777/bg-left.png' == request.client.rxURL

        and: 'its a cache hit'
            request.client.vCL_call.contains 'hit deliver'
            request.client.txHeader.'X-Varnish' == '1771187188 1771187163'
    }

    def 'log parser can parse request that involves backend fetch'() {
        given: 'a varnish log parser with a single listener'
            def parser = new VarnishLogParser()
            def request
            def listener = [request: { request = it }] as VarnishRequestListener
            parser.addListener listener

        when: 'we hand it a full client request'
            parser.parse BACKEND_FETCH

        then: 'it notifies the listeners of a request'
            request
        
        and: 'the request url is /'
            '/' == request.client.rxURL
            '/' == request.backend.txURL

        and: 'the client request headers are as expected'
            request.client.rxHeader.'Host' == 'animal.energylab.ew:38128'
            request.client.rxHeader.'User-Agent' == 'Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.12) Gecko/20101110 Gentoo Firefox/3.6.12'
            request.client.rxHeader.'Accept' == 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
            request.client.rxHeader.'Accept-Language' == 'en-gb,en;q=0.5'
            request.client.rxHeader.'Accept-Encoding' == 'gzip,deflate'
            request.client.rxHeader.'Accept-Charset' == 'ISO-8859-1,utf-8;q=0.7,*;q=0.7'
            request.client.rxHeader.'Keep-Alive' == '115'
            request.client.rxHeader.'Connection' == 'keep-alive'

        and: 'the client response headers are as expected'
            request.client.txHeader.'Content-Type' == 'text/html; charset=utf-8'
            request.client.txHeader.'Content-Language' == 'en-GB'
            request.client.txHeader.'Server' == 'Jetty(6.1.21)'
            request.client.txHeader.'Date' == 'Tue, 11 Jan 2011 15:24:46 GMT'
            request.client.txHeader.'X-Varnish' == '1771187184'
            request.client.txHeader.'Age' == '0'
            request.client.txHeader.'Via' == '1.1 varnish'
            request.client.txHeader.'Connection' == 'keep-alive'

        and: 'the backend response headers are as expected'
            request.backend.rxHeader.'Content-Type' == 'text/html; charset=utf-8'
            request.backend.rxHeader.'Content-Language' == 'en-GB'
            request.backend.rxHeader.'Content-Length' == '8913'
            request.backend.rxHeader.'Server' == 'Jetty(6.1.21)'
    }

}

