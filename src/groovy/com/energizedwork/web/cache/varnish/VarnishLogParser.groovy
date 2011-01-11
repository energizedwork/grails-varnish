package com.energizedwork.web.cache.varnish


class VarnishLogParser {

    private Set listeners = []
    private VarnishRequest request

    void addListener(VarnishRequestListener listener) {
        if(listener) { listeners << listener }
    }

    void parse(String input) {
        input.eachLine { String line ->
            def columns = line.split()
            if(columns.length >= 4) {
                if(columns[1] == 'TxRequest' || columns[1] == 'ReqStart') {
                    request = request ?: new VarnishRequest()
                } else if(columns[1] == 'ReqEnd') {
                    listeners*.request request
                    request = null
                } else {
                    if(request) {
                        def target = request.client
                        if(columns[2] == 'b' || columns[2] == '-') {
                            target = request.backend
                        }
                        target.putAt(columns[1], columns[3..-1].join(' '))
                    }
                }
            }
        }
    }

}
