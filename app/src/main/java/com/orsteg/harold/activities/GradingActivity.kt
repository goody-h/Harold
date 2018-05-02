package com.orsteg.harold.activities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import com.orsteg.harold.R
import com.orsteg.harold.dialogs.PlainDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.result.GradingSystem
import java.util.ArrayList

class GradingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grading)

        setSupportActionBar(findViewById<View>(R.id.toolbar2) as Toolbar)
        supportActionBar?.title = "Grading System"
        init()
        findViewById<View>(R.id.cancel).setOnClickListener { finish() }

        findViewById<View>(R.id.reset).setOnClickListener { view ->
            GradingSystem(view.context).resetSystem()

            init()
        }
    }

    private fun init() {

        val system = GradingSystem(this)

        val g = system.allGrades

        val gp = system.allGradePoints

        val b = system.gradeStates

        val gs = (0..17).mapTo(ArrayList<Grading>()) { Grading(this, g[it], gp[it], b[it]) }

        val adapter = GradingAdapter(this, gs)

        (findViewById<View>(R.id.list) as ListView).adapter = adapter

    }

    private inner class Grading(private val context: Context, var grade: String, var gp: Float, var state: Boolean) {

        fun setGp(gp: Float, pos: Int) {
            this.gp = gp
            Preferences(context, Preferences.RESULT_PREFERENCES).mEditor.putFloat("grading.gradePoint$pos", gp).commit()
        }

        fun setState(state: Boolean, pos: Int) {
            this.state = state
            Preferences(context, Preferences.RESULT_PREFERENCES).mEditor.putBoolean("grading.gradeState$pos", state).commit()

        }
    }

    private inner class GradingAdapter(var context: Context, var mgrading: ArrayList<Grading>) : BaseAdapter() {

        override fun getCount(): Int {
            return mgrading.size
        }

        override fun getItem(i: Int): Grading {
            return mgrading[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {

            val inflater = LayoutInflater.from(context)

            val v = inflater.inflate(R.layout.grading_item, viewGroup, false)

            (v.findViewById<View>(R.id.grade) as TextView).text = getItem(i).grade

            (v.findViewById<View>(R.id.gp) as TextView).text = getItem(i).gp.toString()

            v.findViewById<View>(R.id.gpset).setOnClickListener { v ->
                val dialog = PlainDialog(v.context, "Set value", 2, getItem(i).gp.toString())
                dialog.show()
                dialog.save?.setOnClickListener {
                    if (dialog.value?.text.toString().isNotEmpty()) {
                        mgrading[i].setGp(java.lang.Float.valueOf(dialog.value?.text.toString())!!, i)
                        dialog.dismiss()
                    }
                }
            }

            val state = v.findViewById<Switch>(R.id.state)

            state.isChecked = getItem(i).state

            state.setOnCheckedChangeListener { _, b ->
                if (b && !getItem(i).state) {
                    mgrading[i].setState(true, i)
                } else if (!b && getItem(i).state) {
                    mgrading[i].setState(false, i)
                }
            }

            return v
        }
    }
}
