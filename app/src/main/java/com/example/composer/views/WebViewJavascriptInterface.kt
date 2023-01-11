package com.example.composer.views

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebViewJavascriptInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }
}
