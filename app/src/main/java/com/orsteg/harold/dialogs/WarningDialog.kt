package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.warning_layout.*

/**
 * Created by goodhope on 4/28/18.
 */
class WarningDialog(context: Context, private val messageTxt: String, private val onYes: () -> Unit) : Dialog(context) {


    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.warning_layout)

        message.text = messageTxt
        no.setOnClickListener( { dismiss() })
        yes.setOnClickListener({
            onYes()
            dismiss()
        })
    }

}
