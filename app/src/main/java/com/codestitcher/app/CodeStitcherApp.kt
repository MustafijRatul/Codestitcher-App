package com.codestitcher.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView

/**
 * Application class for CodeStitcher Android app.
 * Configures global app state and WebView environment.
 */
class CodeStitcherApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable remote Chrome DevTools debugging for debug builds
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
