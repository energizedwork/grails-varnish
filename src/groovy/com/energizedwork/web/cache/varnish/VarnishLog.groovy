package com.energizedwork.web.cache.varnish

class VarnishLog {

    private Thread logger
    private VarnishLogParser parser = new VarnishLogParser()
    private boolean requestsFinished
    private long sleepTime = 100
    private boolean stopLogging
    private Process varnishlog

    String stopText = 'EOF on CLI connection'
    boolean waitForRequestsToFinish = true

    void addListener(VarnishRequestListener listener) {
        parser.addListener listener
    }

    void start(Varnishd varnish) {
        String commandLine = "varnishlog -o -n $varnish.workingDirectory"
        varnishlog = commandLine.execute()

        logger = Thread.start {
            def out = new StringBuilder()
            while(!stop) {

                int available
                InputStream input = varnishlog.inputStream
                while ((available = input.available()) > 0) {
                    available.times { out << (char)input.read() }
                }

                if(out) {
                    if(stopLogging && waitForRequestsToFinish) {
                        requestsFinished = (out.indexOf(stopText) != -1)
                    }

                    int lastLineBreak = out.lastIndexOf('\n')
                    if(lastLineBreak >= 0) {
                        parser.parse out.substring(0, lastLineBreak)
                        String incompleteLine = out.substring(lastLineBreak+1)
                        out.length = 0
                        out << incompleteLine
                    }

                    if(stop && out) {
                        parser.parse out
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
        stopLogging && (waitForRequestsToFinish == requestsFinished)
    }

}
