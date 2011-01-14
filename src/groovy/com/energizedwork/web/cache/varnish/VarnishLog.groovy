package com.energizedwork.web.cache.varnish

import com.energizedwork.web.util.ThreadUtils


class VarnishLog {

    private Process varnishlog
    private Thread logger
    private boolean stopLogging
    private boolean requestsFinished
    private long sleepTime = 100
    private ByteArrayOutputStream out
    private VarnishLogParser parser = new VarnishLogParser()

    boolean waitForRequestsToFinish = true


    void addListener(VarnishRequestListener listener) {
        parser.addListener listener
    }

    void start(Varnishd varnish) {
        String commandLine = "varnishlog -o -n $varnish.workingDirectory"
        varnishlog = commandLine.execute()
        out = new ByteArrayOutputStream()

        logger = Thread.start {
            while(!stop) {
                ThreadUtils.stfu varnishlog.consumeProcessOutputStream(out)

                String logs = out.toString()

                if(logs) {
                    println logs

                    if(stopLogging && waitForRequestsToFinish) {
                        requestsFinished = logs.contains('ping')                        
                    }

                    int lastLineBreak = logs.lastIndexOf('\n')
                    if(lastLineBreak >= 0) {
                        parser.parse logs.substring(0, lastLineBreak)
                        out.reset()
                        out.write logs.substring(lastLineBreak+1).bytes
                    }
                }

                Thread.sleep sleepTime
            }
        }
    }

    void stop() {
        if(varnishlog) {
            stopLogging = true
            logger.join()
            varnishlog.destroy()
            varnishlog.waitFor()
            varnishlog = null
        }
    }

    private boolean getStop() {
        println "stop $stopLogging $waitForRequestsToFinish $requestsFinished"
        stopLogging && (waitForRequestsToFinish == requestsFinished)
    }

}
