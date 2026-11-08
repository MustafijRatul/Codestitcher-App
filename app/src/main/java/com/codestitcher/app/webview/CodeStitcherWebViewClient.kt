package com.codestitcher.app.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.codestitcher.app.R

class CodeStitcherWebViewClient(
    private val context: Context,
    private val onPageLoadingChanged: (Boolean) -> Unit,
    private val onErrorReceived: (isError: Boolean, errorDescription: String?) -> Unit,
    private val onRetryRequested: () -> Unit
) : WebViewClient() {

    private var hasError = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase() ?: ""

        // Handle offline retry custom scheme
        if (scheme == "retry" || url.startsWith("retry://")) {
            onRetryRequested()
            return true
        }

        // Handle custom URL schemes
        if (scheme == "tel" || scheme == "mailto" || scheme == "sms" || scheme == "market" || scheme == "intent") {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
            }
            return true
        }

        // Local assets (like dino game) load inside webview
        if (url.startsWith("file:///android_asset/")) {
            return false
        }

        // Check if internal domain
        val host = uri.host?.lowercase() ?: ""
        if (host == "codestitcher-web.github.io" || host.endsWith(".github.io")) {
            return false // Load in WebView
        }

        // Open external links in Chrome Custom Tabs
        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(context, R.color.brand_primary))
                .build()
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(browserIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        hasError = false
        onPageLoadingChanged(true)
        injectMobileNoZoomScript(view)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        injectMobileNoZoomScript(view)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        injectMobileNoZoomScript(view)
        onPageLoadingChanged(false)
        if (!hasError) {
            onErrorReceived(false, null)
        }
    }

    /**
     * Completely disables zoom (in and out) and prevents horizontal overflowing/cutting off.
     */
    private fun injectMobileNoZoomScript(view: WebView?) {
        val js = """
            (function() {
                try {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        document.head.appendChild(meta);
                    }
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';
                    
                    var existingStyle = document.getElementById('cs-mobile-fit-style');
                    if (!existingStyle) {
                        var style = document.createElement('style');
                        style.id = 'cs-mobile-fit-style';
                        style.innerHTML = 'html, body { max-width: 100% !important; overflow-x: clip !important; word-break: break-word; touch-action: pan-x pan-y !important; }';
                        document.head.appendChild(style);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // Only trigger offline error screen for the main frame failure
        if (request?.isForMainFrame == true) {
            hasError = true
            onPageLoadingChanged(false)
            onErrorReceived(true, error?.description?.toString())
        }
    }
}
