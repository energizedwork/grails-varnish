package com.energizedwork.web.cache


interface WebCache {

    void start()
    void start(Service server)
    void stop()

    Service getCache()
    Service getServer()

    boolean isRunning()

}
