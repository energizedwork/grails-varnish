package com.energizedwork.web.cache


@Immutable class Service {

    String host
    String protocol
    int port

    String getUrl() { "${protocol ?: 'http'}://$host:$port" }

}
