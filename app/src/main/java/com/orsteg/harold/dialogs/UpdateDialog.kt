package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Window
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.update_dialog_layout.*

/**
 * Created by goodhope on 5/2/18.
 */
class UpdateDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.update_dialog_layout)

        update.setOnClickListener { update() }
        cancel.setOnClickListener { dismiss() }

    }

    private fun update(){
        val uri = Uri.parse("market://details?id=" + context.packageName)
        val gotoMarket = Intent(Intent.ACTION_VIEW, uri)
        var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        flags = if (Build.VERSION.SDK_INT >= 21) {
            flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        } else {

            flags or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
        }
        gotoMarket.addFlags(flags)
        try {
            context.startActivity(gotoMarket)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)))
        }
        dismiss()
    }
}