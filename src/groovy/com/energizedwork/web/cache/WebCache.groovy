package com.energizedwork.web.cache


interface WebCache {

    void start()
    void start(Service server)
    void stop()

    Map getConfig()

    Service getCache()
    Service getServer()

    boolean isRunning()

}
