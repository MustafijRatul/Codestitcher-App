package com.codestitcher.app.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import com.codestitcher.app.R

/**
 * Handles file downloads triggered by CodeStitcher IDE (Python scripts, reports, datasets).
 */
class DownloadHelper(private val context: Context) {

    fun downloadFile(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimetype: String?
    ) {
        try {
            var fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            if (fileName.isNullOrEmpty() || fileName == "downloadfile.bin") {
                fileName = "codestitcher_file_${System.currentTimeMillis()}"
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype)
                if (!ext.isNullOrEmpty()) {
                    fileName = "$fileName.$ext"
                }
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setDescription(context.getString(R.string.msg_downloading))
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                allowScanningByMediaScanner()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(
                context,
                "${context.getString(R.string.msg_downloading)}: $fileName",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Download error: ${e.localizedMessage ?: "Unknown"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
