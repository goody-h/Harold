package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import com.orsteg.harold.R
import com.orsteg.harold.utils.result.Course
import com.orsteg.harold.utils.result.Semester
import kotlinx.android.synthetic.main.set_course_dialog_layout.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by goodhope on 4/28/18.
 */
class AddDialog(context: Context, private var id: Int, private val staticId: Boolean,
                private val new: Boolean, private val course: Course? = null,
                private val onSuccess: (Int, Double) -> Unit ) : Dialog(context) {

    private var ss = ""

    private val TAG = "mytest"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.set_course_dialog_layout)

        dTitle.text = if(new) "Add Course" else "Edit Course"
        add.text = if(new) "Add" else "Edit"

        val lelN = Math.floor(id /1000.0) - 1
        val semN = (id % 1000) / 100 - 1

        if (!staticId) {
            val l = context.resources.getStringArray(R.array.levels)
            val lel = (0 until l.size ) .mapTo(ArrayList<String>()) { l[it] }

            val s = context.resources.getStringArray(R.array.semes)
            val sem = (0 until s.size ) .mapTo(ArrayList<String>()) { s[it] }

            levels.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, lel)
            levels.setSelection(lelN.toInt())
            semesters.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sem)
            semesters.setSelection(semN)
        } else {

            levels.visibility = View.GONE
            semesters.visibility = View.GONE

            if(!new){
                title.setText(course?.title)
                val s =
                        if (course!!.cu % 1 == 0.0) course.cu.toInt().toString()
                        else course.cu.toString()

                cu.setText(s)
                code.setText(course.code)
            }
        }

        code.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                ss = s.toString()
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length > 10) {
                    code.setText(ss)
                    code.setSelection(ss.length - 1)
                }
                validate()
            }

            override fun afterTextChanged(s: Editable) {

                if (s.length == 10) {
                    title.requestFocus()
                }
            }
        })
        title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                validate()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        cu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                ss = s.toString()
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (s.length > 5) {
                    cu.setText(ss)
                    cu.setSelection(ss.length - 1)
                }

                validate()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        cancel.setOnClickListener( { dismiss() })

        if (new) {
            add.setOnClickListener({
                createNew()
                dismiss()
                onSuccess(id, cu.text.toString().toDouble())
            })
        } else {
            add.setOnClickListener({
                update()
                dismiss()
                onSuccess(id, cu.text.toString().toDouble())
            })
        }

    }

    fun validate(){
        add.isEnabled = enable()
        add.setTextColor(txtColor(enable()))

    }


    private fun enable(): Boolean {
        return code.text.isNotEmpty() && title.text.isNotEmpty() && cu.text.isNotEmpty()
    }

    private fun txtColor(boo: Boolean): Int {
        return if (boo) {
            -0x1000000
        } else {
            -0x555347
        }
    }

    private fun createNew() {

        id = if(staticId) id else (levels.selectedItemPosition + 1) * 1000 + (semesters.selectedItemPosition + 1) * 100

        val cCode = "${code.text}"
        val cTitle = title.text.toString()
        val cCU = cu.text.toString().toDouble()
        val cn = Semester.courseCount(context, id) + 1
        val cId = id + cn

        Course(context, cId, cn, cCode, cTitle, cCU, "", 0)
    }

    fun update() {

        val cCode = "${code.text}"
        val cTitle = title.text.toString()
        val cCU = cu.text.toString().toDouble()

        course?.editInfo(cTitle, cCode, cCU)
    }
}
