package com.energizedwork.web.cache.varnish


class VarnishLogParser {

    private Set listeners = []
    private VarnishRequest request

    int urlCount = 0

    void addListener(VarnishRequestListener listener) {
        if(listener) { listeners << listener }
    }

    void parse(String input) {
        input.eachLine { String line ->
            def columns = line.split()
            if(columns.length >= 4) {
                if(isRequestStart(columns)) {
                    request = request ?: new VarnishRequest()
                } else if(isRequestEnd(columns)) {
                    listeners*.request request
                    request = null
                }
                if(request) {
                    def side = selectSide(request, columns)
                    side[columns[1]] = columns[3..-1].join(' ')
                }
            }
        }
    }

    boolean isRequestStart(String[] columns) {
        columns[1] in ['TxRequest', 'ReqStart']
    }

    boolean isRequestEnd(String[] columns) {
        columns[1] == 'ReqEnd'
    }

    VarnishRequestSide selectSide(VarnishRequest request, String[] columns) {
        (columns[2] == 'b' || columns[2] == '-') ? request.backend : request.client
    }

}
