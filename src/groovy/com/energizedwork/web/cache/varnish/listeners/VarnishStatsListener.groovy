package com.energizedwork.web.cache.varnish.listeners

import com.energizedwork.web.cache.varnish.VarnishRequest
import com.energizedwork.web.cache.varnish.VarnishRequestListener


class VarnishStatsListener implements VarnishRequestListener {

    def stats = [:]

    void request(VarnishRequest request) {
        String uri = request?.client?.rxURL
        if(stats.containsKey(uri)) {
            stats[uri] = stats[uri]++
        } else {
            stats[uri] = 1
        }
    }

    String toString() {
        def out = new StringBuilder()
        stats.each { url, count ->
            out << "$count : $url\n"
        }
        out
    }

}
