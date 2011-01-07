package com.energizedwork.web.util


class NetUtils {

    static int findFreePort() {
        int result

        def socket = new ServerSocket(0)
        result = socket.localPort
        socket.close()

        result
    }

}
