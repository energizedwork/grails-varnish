package com.energizedwork.web.cache.varnish

import org.apache.commons.lang.StringUtils


class VarnishRequest {

    String uri
    def backend = new VarnishRequestSide()
    def client = new VarnishRequestSide()

}

class VarnishRequestSide {

    private data = [:]

    def getProperty(String property) { data[property] }
    
    def getAt(String property) { data[property] }

    void putAt(String property, Object value) {
        String key = StringUtils.uncapitalize(property)
        if(property.contains('Header')) {
            int colonIndex = value.indexOf(':')
            if(colonIndex >= 0) {
                value = [(value.substring(0, colonIndex)): (value.substring(colonIndex+1).trim())]
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
                println key
                println value
                existingValue << value
            } else {
                map[key] = [existingValue, value]
            }
        } else {
            map[key] = value
        }
    }


}
