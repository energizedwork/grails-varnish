package com.energizedwork.web.cache.varnish.grails

import com.energizedwork.web.cache.varnish.VCLFileResolver
import com.energizedwork.grails.util.GrailsHelper


class GrailsVCLFileResolver implements VCLFileResolver {

    File getVclFile() {
        File result

        String configValue = GrailsHelper.config?.varnish?.vcl?.file

        if(configValue) {
            File file = new File(configValue)
            result = file.exists() ? file : null
        }

        result  
    }

}
