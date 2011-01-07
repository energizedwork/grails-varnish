package com.energizedwork.web.util

import java.lang.Thread.UncaughtExceptionHandler


class ThreadUtils {

    static UncaughtExceptionHandler getStfu() {
        new UncaughtExceptionHandler() {
            void uncaughtException(Thread t, Throwable e) {}
        }
    }

    static stfu(Thread thread) {
        thread.uncaughtExceptionHandler = stfu
    }

}
