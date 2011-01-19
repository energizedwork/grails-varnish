package com.energizedwork.web.cache.varnish

import org.apache.commons.lang.StringUtils

class VarnishRequestSide {

    boolean backend

    private data = [:]

    private parsers = [
        'reqStart': { def fields ->
            fields = fields.split()
            data['clientIP'] = fields[0]
            data['xid'] = fields[2]
        },
        'reqEnd': { def fields ->
            if(client) { assert data['xid'] == fields.split()[0] }
        },
        'X-Varnish': { String xid ->
            if(xid.contains(' ')) { xid = xid.split()[0] }
            if(backend) { data['xid'] = xid }
            else { assert data['xid'] == xid }
        }
    ]

    boolean isClient() { !backend }

    def propertyMissing(String property) { data[property] }

    def getAt(String property) {
        this."$property"
    }

    void putAt(String property, Object value) {
        String key = StringUtils.uncapitalize(property)
        parsers[key]?.call value

        if(property.contains('Header')) {
            value = convertValueIntoMap(value)
            value.each { headerKey, headerValue ->
                parsers[headerKey]?.call headerValue
            }
        }

        addOrMakeValueCollection key, value, data
    }

    String toString() {
        def out = new StringBuilder()
        data.each { key, value ->
            out << "\t$key = $value\n"
        }
        out
    }

    private addOrMakeValueCollection(String key, Object value, Map map) {
        def existingValue = map[key]
        if(existingValue) {
            if(existingValue in Map || existingValue in List) {
                existingValue << value
            } else {
                map[key] = [existingValue, value]
            }
        } else {
            map[key] = value
        }
    }

    private convertValueIntoMap(def value) {
        int colonIndex = value.indexOf(':')
        if(colonIndex >= 0) {
            value = [(value.substring(0, colonIndex)): (value.substring(colonIndex+1).trim())]
        }
        value
    }

}
