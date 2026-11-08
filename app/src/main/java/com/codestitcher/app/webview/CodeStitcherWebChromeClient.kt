package com.codestitcher.app.webview

import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CodeStitcherWebChromeClient(
    private val activity: Activity,
    private val customViewContainer: FrameLayout,
    private val fileChooserLauncher: ActivityResultLauncher<Array<String>>,
    private val onProgressUpdate: (Int) -> Unit,
    private val onTitleReceived: (String?) -> Unit,
    private val onCustomViewChanged: (Boolean) -> Unit
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    fun onFileChooserResult(uris: Array<Uri>?) {
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleReceived(title)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*")
        val mimeTypes = if (acceptTypes.isEmpty() || (acceptTypes.size == 1 && acceptTypes[0].isEmpty())) {
            arrayOf("*/*")
        } else {
            acceptTypes
        }

        try {
            fileChooserLauncher.launch(mimeTypes)
        } catch (e: Exception) {
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            return false
        }
        return true
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }

        customView = view
        customViewCallback = callback

        customViewContainer.apply {
            addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            visibility = View.VISIBLE
        }

        onCustomViewChanged(true)
    }

    override fun onHideCustomView() {
        if (customView == null) return

        customViewContainer.apply {
            visibility = View.GONE
            removeView(customView)
        }

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null

        onCustomViewChanged(false)
    }

    fun isCustomViewShowing(): Boolean = customView != null

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        MaterialAlertDialogBuilder(activity)
            .setTitle("CodeStitcher")
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                result?.confirm()
                dialog.dismiss()
            }
            .setOnCancelListener { result?.cancel() }
            .create()
            .show()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        MaterialAlertDialogBuilder(activity)
            .setTitle("CodeStitcher")
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                result?.confirm()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                result?.cancel()
                dialog.dismiss()
            }
            .setOnCancelListener { result?.cancel() }
            .create()
            .show()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        val input = EditText(activity).apply {
            setText(defaultValue ?: "")
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("CodeStitcher")
            .setMessage(message ?: "")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                result?.confirm(input.text.toString())
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                result?.cancel()
                dialog.dismiss()
            }
            .setOnCancelListener { result?.cancel() }
            .create()
            .show()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return super.onConsoleMessage(consoleMessage)
    }
}
