package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.list_dialog_layout.*
import java.util.ArrayList

/**
 * Created by goodhope on 4/28/18.
 */
class ListDialog(context: Context, private val headerTxt: String, private val listItems: ArrayList<String>,
                 private val onItemClick: (AdapterView<*>, Int) -> Unit) : Dialog(context) {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.list_dialog_layout)

        header.text = headerTxt

        list.emptyView = empty
        list.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listItems)

        list.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            dismiss()
            onItemClick(parent, position)
        }
    }
}
