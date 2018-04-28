package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.loader_layout.*

/**
 * Created by goodhope on 4/28/18.
 */
class LoaderDialog(context: Context, private val cancelable: Boolean = false) : Dialog(context) {

    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.loader_layout)

        if (cancelable) setCancelable {  }
    }

    fun setLoadMessage(mes: String) {
        loadmessage.text = mes
    }

    fun setLoading(mes: String) {
        onload.visibility =  View.VISIBLE
        onfailed.visibility =  View.GONE
        loadmessage.text = mes
    }

    fun setFaileddMessage(mes: String) {
        failedmessage.text = mes
    }

    fun setFailed(mes: String, onCancel: (View) -> Unit, onRetry: (View) -> Unit) {
        onfailed.visibility = View.VISIBLE
        onload.visibility = View.GONE
        failedmessage.text = mes
        cancel.setOnClickListener(onCancel)
        retry.setOnClickListener(onRetry)
    }

    fun setCancelable(onAbort: () -> Unit) {
        abort.visibility = View.VISIBLE
        abort.setOnClickListener({
            dismiss()
            onAbort()
        })
    }

    fun hideAbort() {
        abort.visibility = View.GONE
    }
}
