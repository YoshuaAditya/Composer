package com.example.composer.views

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class WebViewJavascript {
    companion object {
        private val JAVASCRIPT_BRIDGE_NAME: String = "javascriptAndroidHandler"
        private val LOCAL_HTML_FILE_PATH = "file:///android_asset/index.html"
        @Composable
        fun IndexHTML() {
            // Adding a WebView inside AndroidView
            // with layout as full screen
            AndroidView(factory = {
                WebView(it).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(WebViewJavascriptInterface(context), JAVASCRIPT_BRIDGE_NAME)
                    loadUrl(LOCAL_HTML_FILE_PATH)
                }
            },modifier = Modifier.fillMaxSize())
        }
    }
}