package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.orsteg.harold.R

/**
 * Created by goodhope on 4/28/18.
 */
class SaveDialog(context: Context) : Dialog(context){

    var cancel: Button? = null
    var save: Button? = null
    var fileName: EditText? = null
    var note: EditText? = null
    var nameerror: TextView? = null
    var filname: String = ""
    var mNote: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.save_dialog_layout)

        save = findViewById<View>(R.id.save) as Button
        cancel = findViewById<View>(R.id.cancel) as Button
        nameerror = findViewById<View>(R.id.nameerror) as TextView
        fileName = findViewById<View>(R.id.filename) as EditText
        note = findViewById<View>(R.id.note) as EditText

        cancel?.setOnClickListener { dismiss() }


        fileName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                filname = s.toString()
                validate()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        note?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                mNote = s.toString()
                validate()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        validate()

    }


    private fun validate() {

        var def = true
        if (filname == "") {
            def = false
        }
        save?.isEnabled = def
        save?.setTextColor(txtcolor(def))
    }

    private fun txtcolor(boo: Boolean?): Int {
        return if (boo!!) {
            -0x1000000
        } else {
            -0x555347
        }
    }

    fun hideNote() {
        note?.visibility = View.GONE
    }

}