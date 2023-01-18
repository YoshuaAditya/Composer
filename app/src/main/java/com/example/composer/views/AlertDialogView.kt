package com.example.composer.views

import android.app.AlertDialog
import android.content.Context

class AlertDialogView {
    companion object {
        fun buildAlert(context: Context){
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Permission needed")
            builder.setMessage("To access this feature, you need to allow calendar permission")

            builder.setPositiveButton("OK") { dialog, which ->
            }
            builder.show()
        }
    }
}