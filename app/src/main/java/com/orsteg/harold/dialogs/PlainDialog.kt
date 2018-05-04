package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.orsteg.harold.R

/**
 * Created by goodhope on 4/28/18.
 */
class PlainDialog(context: Context, var title: String, private val type: Int, private val def: String) : Dialog(context) {

    var value: EditText? = null
    var auth: EditText? = null
    var head: TextView? = null
    var save: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.plain_dialog_layout)

        auth = findViewById(R.id.value3)
        when (type) {
            1 -> value = findViewById(R.id.value1)
            2 -> value = findViewById(R.id.value2)
        }
        value?.visibility = View.VISIBLE
        value?.setText(def)
        save = findViewById(R.id.save)


        head = findViewById(R.id.title)
        head?.text = title

        findViewById<View>(R.id.cancel).setOnClickListener { dismiss() }

    }
}