package com.orsteg.harold.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.orsteg.harold.R
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.dialogs.AddDialog
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.result.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : BaseFragment() {
    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            if(pendingReset) {
                initiate()
                pendingReset = false
            }
        }
    }

    override val mPrefType: String = Preferences.RESULT_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onBackPressed(): Boolean {
        if (state == -1) {
            exitMultiSelect()
            return false
        }
        return true
    }

    override fun refresh(option: Int) {
        when (option) {
            0 -> {
                if (!isHidden) initiate()
                else pendingReset = true
            }
            1 -> {
                reset = true
            }
        }
    }

    private var mParam1: String? = null
    private var mParam2: String? = null

    private var semNav: View? = null
    private var semDisplay: View? = null
    private var actionBtn: FloatingActionButton? = null

    private var mAction: View.OnClickListener? = null

    var Semester_Id: Int = 0
    var GPA: Float = 0.toFloat()
    var tcu = 0.0
    var tcuTxt: TextView? = null
    var gpaTxt: TextView? = null

    var cgpaTxt: TextView? = null

    var result: ListView? = null
    var levSpinner: Spinner? = null
    var semSpinner: Spinner? = null
    var arr: CourseAdapter? = null
    private var empty: View? = null
    private var loader: LoaderDialog? = null
    var foot: View? = null
    var ring: View? = null


    var state: Int = 0

    private var SelectSem: Int = 0


    private var init: Boolean = false


    private var gradingSystem: GradingSystem? = null

    private var reset: Boolean = false

    private var pendingReset: Boolean = false

    private val timer = Timer()
    var task: TimerTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_result, container, false)
        semDisplay = view
        semNav = view.findViewById(R.id.nav)
        actionBtn = view.findViewById(R.id.actionBtn)
        ring = view.findViewById(R.id.ring)

        mAction = View.OnClickListener { newCourse() }

        // Inflate the layout for this fragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        levSpinner = semNav?.findViewById(R.id.level_select)
        semSpinner = semNav?.findViewById(R.id.sem_select)

        state = 0

        reset = false


        result = view.findViewById<View>(R.id.results) as ListView
        empty = view.findViewById(R.id.empty)
        loader = LoaderDialog(context!!)

        gradingSystem = GradingSystem(context!!)

        val h1 = ArrayList<String>()
        val h2 = ArrayList<String>()

        h1.add("LEVEL")
        h2.add("SEM")

        cgpaTxt = semDisplay?.findViewById<View>(R.id.cgpa) as TextView
        tcuTxt = semDisplay?.findViewById<View>(R.id.tcu) as TextView
        gpaTxt = semDisplay?.findViewById<View>(R.id.gpa) as TextView

        levSpinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, h1)
        semSpinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, h2)

        SelectSem = 0

        actionBtn?.setOnClickListener(mAction)

        init = true

        initiate()

    }

    private fun animateRing() {
        ring!!.alpha = 1f

        task?.cancel()

        task = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    ring!!.alpha = 0.3f
                    task = null
                }
            }

        }

        timer.schedule(task, 2000)
    }

    fun setActionBtn(resId: Int, listener: View.OnClickListener?) {
        actionBtn?.setImageResource(resId)
        actionBtn?.setOnClickListener(listener)
    }

    private fun newCourse() {

        AddDialog(context!!, Semester_Id, false, true){ semId, _, _ ->

            initiate()

        }.show()

    }

    private fun initiate() {

        cgpaCalc()

        val lel = context!!.resources.getStringArray(R.array.levels)
        val sem = context!!.resources.getStringArray(R.array.semes)
        val levels = ArrayList<String>()
        val num = ArrayList<Level>()

        for (i in 1..9) {
            var showLevel = false
            val l = Level(i * 1000)

            for (j in 1..3) {

                val semId = i * 1000 + j * 100
                if (Semester.courseCount(context!!, semId) != 0) {
                    l.sems.add(semId)
                    l.semn.add(sem[j - 1])
                    showLevel = true
                }
            }
            if (showLevel) {
                num.add(l)
                levels.add(lel[i - 1])
            }
        }

        if (levels.size != 0) {

            semNav?.findViewById<View>(R.id.nav)?.visibility = View.VISIBLE

            val adapt1 = LevelAdapter(context!!, levels, num)


            levSpinner?.adapter = adapt1


            levSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val adapt = parent?.adapter as LevelAdapter
                    val adapt2 = SemAdapter(context!!, adapt.levels[position].semn,
                            adapt.levels[position].sems)

                    semSpinner?.adapter = adapt2

                    if (init) {
                        val pos = 0

                        semSpinner?.setSelection(pos)
                        init = false
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

            semSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                    if (parent?.adapter is SemAdapter) {
                        val adapt = parent.adapter as SemAdapter
                        val semId = adapt.sem[position]
                        setSem(semId)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

        } else {
            result?.adapter = CourseAdapter(context!!)
            result?.emptyView = empty
            if(foot != null) {
                result?.removeFooterView(foot)
                foot = null
            }

            val l = ArrayList<String>()
            l.add("LEVEL")
            val s = ArrayList<String>()
            s.add("SEM")

            semNav?.findViewById<View>(R.id.nav)?.visibility = View.GONE


            levSpinner?.onItemSelectedListener = null
            semSpinner?.onItemSelectedListener = null
            levSpinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, l)
            semSpinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, s)

            gpaTxt?.text = "0"
            GPA = 0f
            cgpaTxt?.text = "0.00"
            tcu = 0.0
            tcuTxt?.text = "0"

        }
    }

    private fun setSem(SemId: Int) {

        Semester_Id = SemId
        tcu = 0.0

        SelectSem = SemId

        tcuTxt?.text = tcu.toString()

        val helper = ResultDataBase(context!!, SemId)
        val res = helper.getAllData()

        val courseCount = Semester.courseCount(context!!, SemId)

        val courses = ArrayList<Course>()

        for (j in 1 until courseCount + 1) {
            res.moveToPosition(j - 1)
            val courseId = SemId + j
            val sqlId = res.getInt(0)
            val title = res.getString(1)
            val code = res.getString(2)
            val cu = res.getDouble(3)
            val grade = res.getString(4)

            val cou = Course(context!!, courseId, j, code, title, cu, grade, sqlId)

            editTcu(tcu + cou.cu)

            courses.add(cou)
        }
        res.close()
        helper.close()

        arr = CourseAdapter(context!!)
        arr?.template = courses
        arr?.sem_Id = SemId

        result?.adapter = arr

        gpaCalc()
        cgpaCalc()

        animateRing()
    }

    private inner class LevelAdapter(context: Context, objects: List<*>, val levels: ArrayList<Level>) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_dropdown_item, objects)

    inner class SemAdapter(context: Context, objects: List<*>, val sem: ArrayList<Int>) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_dropdown_item, objects)

    override fun onStart() {
        super.onStart()

        if (reset || mPreferences.mPrefs.getBoolean("result.changed", false)) {

            initiate()

            mPreferences.mEditor.putBoolean("result.changed", false)

            reset = false

        }
        if (state != 0) exitMultiSelect()

    }


    inner class CourseAdapter(var context: Context, var template: ArrayList<Course> = ArrayList()) : BaseAdapter() {
        var multidel: ArrayList<Int> = ArrayList()
        var sem_Id: Int = 0


        // pointer showing which result is on editMode
        var editMode = -1

        init {
            editMode = -1
        }

        override fun getCount(): Int {

            return template.size
        }

        override fun getItem(position: Int): Any {
            return template[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var convertView = convertView

            val holder = ViewHolder()

            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.course_item, parent, false)

            val editor = inflater.inflate(R.layout.course_edit_layout, parent, false)

            holder.code = convertView.findViewById<View>(R.id.Course_code) as TextView
            holder.title = convertView.findViewById<View>(R.id.Course_title) as TextView
            holder.grade = convertView.findViewById<View>(R.id.grade) as TextView
            holder.num = convertView.findViewById<View>(R.id.index) as TextView

            if (template[position].courseNo != position + 1)
                template[position].editId(sem_Id + position + 1, position + 1)

            val course = template[position]


            val v = convertView.findViewById<View>(R.id.result)
            v.setOnClickListener { _ -> this@CourseAdapter.onClick(position, course) }
            v.setOnLongClickListener { _ ->
                this@CourseAdapter.onLongClick(position)
                true
            }

            holder.code!!.text = course.code
            holder.grade!!.text = course.grade
            holder.title!!.text = course.title
            holder.num!!.text = "" + (position + 1)

            convertView.isClickable = false
            convertView.isLongClickable = false

            if (course.isSelected) {
                v.setBackgroundColor(context.resources.getColor(R.color.colorAccent))
                v.alpha = 0.5f
            }

            if (editMode == position) {
                editMode(convertView.findViewById(R.id.main), position, editor, holder.grade)
            }

            if (foot == null) {
                foot = inflater.inflate(R.layout.result_foot, parent, false)
                result?.addFooterView(foot)
            }

            return convertView
        }


        // insert editlayout and initialise it
        private fun editMode(v: View, position: Int, editor: View, grade1: TextView?) {
            val spin = editor.findViewById<View>(R.id.gradespin) as Spinner
            val delete = editor.findViewById<View>(R.id.delete) as ImageButton
            val edit = editor.findViewById<View>(R.id.edit) as ImageButton
            val cUnit = editor.findViewById<View>(R.id.unit) as TextView


            delete.setOnClickListener {
                WarningDialog(context, "Do you want to delete this course?"){
                    val unit = template[position].cu
                    val helper = ResultDataBase(context, sem_Id)
                    helper.deleteCourse(template[position].sqlId)


                    template.removeAt(position)
                    editTcu(tcu - unit)
                    
                    mPreferences.commit()
                    editMode = -1
                    notifyDataSetChanged()
                    gpaCalc()
                    cgpaCalc()

                }.show()
            }
            edit.setOnClickListener {

                val unit = template[position].cu

                AddDialog(context,  sem_Id, true, false, template[position]){ _, cu, course ->

                    val s =
                            if (cu % 1 == 0.0) cu.toInt().toString() 
                            else cu.toString() 

                    cUnit.text = s

                    if (course != null)
                    template[position] = course

                    editTcu(tcu - unit + cu)

                    notifyDataSetChanged()
                    gpaCalc()
                    cgpaCalc()

                }.show()

            }


            spin.adapter = gradingSystem!!.gradeAdapter


            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(_parent: AdapterView<*>?, view: View?, _position: Int, _id: Long) {

                    var tt = spin.selectedItem as String
                    val u: Double
                    val qp: Float

                    if (tt == "Grade") {
                        tt = ""
                        u = 0.0

                    } else {
                        u = template[position].cu

                    }
                    val gp = gradingSystem!!.gradeToScore(_position)

                    template[position].editResult(tt)
                    grade1!!.text = tt

                    qp = (gp * u).toFloat()

                    mPreferences.mEditor
                            .putFloat(Course.unitPref(template[position].id), u.toFloat())
                            .putFloat(Course.qpPref(template[position].id), qp).commit()

                    gpaCalc()
                    cgpaCalc()

                }

                override fun onNothingSelected(_parent: AdapterView<*>?) {}
            }

            spin.setSelection(gradingSystem!!.getIndexFromGrade(template[position].grade))

            val s =
                    if (template[position].cu % 1 == 0.0) template[position].cu.toInt().toString() 
                    else template[position].cu.toString() 
            cUnit.text = s


            (v as LinearLayout).addView(editor)

        }

        private fun onLongClick(position: Int) {
            if (editMode != -2) {
                template[position].isSelected = true
                multidel.add(position)
                editMode = -2

                notifyDataSetChanged()

                semNav?.visibility = View.GONE
                state = -1

                setActionBtn(R.drawable.ic_delete_black_24dp, View.OnClickListener {
                    Collections.sort(multidel) { o1, o2 -> if (o1 > o2) -1 else if (o1 < o2) 1 else 0 }

                    WarningDialog(context, "You are about to delete "
                            + multidel.size + " courses, continue?"){
                        for (i in multidel.indices) {
                            val position1 = multidel[i]
                            val unit = template[position1].cu
                            val helper = ResultDataBase(context, sem_Id)
                            helper.deleteCourse(template[position1].sqlId)


                            template.removeAt(position1)
                            editTcu(tcu - unit)
                            
                            mPreferences.commit()
                        }

                        if(foot != null && template.size == 0) {
                            result?.removeFooterView(foot)
                            foot = null
                        }

                        multidel.clear()
                        exitMultiSelect()

                        gpaCalc()
                        cgpaCalc()

                    }.show()
                })

            }
            // make changes to the course element and editMode then notifydatasetchange
        }

        private fun onClick(position: Int, c: Course) {

            if (editMode != -2) {
                editMode = if (editMode == position)
                    -1
                else
                    position

            } else {
                if (!c.isSelected) {
                    template[position].isSelected = true
                    multidel.add(position)
                } else {
                    multidel.remove(position)
                    template[position].isSelected = false

                    if (multidel.size == 0) {

                        exitMultiSelect()

                        return
                    }
                }
            }

            notifyDataSetChanged()

            // make changes to the editMode element and notifydatasetchange
        }

        private inner class ViewHolder {
            internal var title: TextView? = null
            internal var code: TextView? = null
            internal var grade: TextView? = null
            internal var num: TextView? = null
        }

    }


    fun gpaCalc() {
        var gpa = 0f
        var tcu = 0f
        var tqp = 0f


        for (i in arr!!.template.indices) {
            val courseId = arr!!.template[i].id
            tcu += mPreferences.mPrefs.getFloat(Course.unitPref(courseId), 0f)
            tqp += mPreferences.mPrefs.getFloat(Course.qpPref(courseId), 0f)
        }
        if (tcu != 0f) {
            gpa = tqp / tcu
        }

        editGPA(gpa)
    }

    fun cgpaCalc() {

        var cgpa = 0f
        var tcu = 0f
        var tqp = 0f

        val levelCount = 9
        for (i in 1 until levelCount + 1) {
            val levelId = i * 1000
            val semCount = 3
            for (j in 1 until semCount + 1) {
                val semId = levelId + j * 100
                val courseCount = Semester.courseCount(context!!, semId)
                if (courseCount != 0) {
                    for (k in 1 until courseCount + 1) {
                        val courseId = semId + k
                        tcu += mPreferences.mPrefs.getFloat(Course.unitPref(courseId), 0f)
                        tqp += mPreferences.mPrefs.getFloat(Course.qpPref(courseId), 0f)
                    }
                }
            }
        }


        if (tcu != 0f) {
            cgpa = tqp / tcu
        }
        mPreferences.mEditor.putFloat("CGPA", cgpa).commit()

        if (cgpaTxt?.text != cgpa.string()) animateRing()

        cgpaTxt?.text = cgpa.string()

    }

    fun editTcu(cu: Double) {
        tcu = cu

        tcuTxt?.text = cu.string()
    }

    private fun editGPA(GPa: Float) {
        GPA = GPa
        gpaTxt?.text = GPa.string()
        mPreferences.mEditor.putFloat("GPA" + Semester_Id, GPa).commit()
    }

    fun Double.string(): String {
        val d = this.toString().indexOf(".")

        if (this % 1 == 0.0) return this.toInt().toString()

        if (d + 4 > this.toString().length) return this.toString()

        return this.toString().substring(0..d+2)
    }

    fun Float.string(): String {
        val d = this.toString().indexOf(".")

        if (d + 3 > this.toString().length) return this.toString() + "0"

        return this.toString().substring(0..d+2)
    }


    fun exitMultiSelect() {
        state = 0

        semNav?.visibility = View.VISIBLE
        arr?.editMode = -1
        if (arr!!.multidel.size != 0) {
            for (i in arr!!.template.indices) {
                arr!!.template[i].isSelected = false
            }
            arr!!.multidel.clear()
        }

        arr?.notifyDataSetChanged()

        setActionBtn(R.drawable.ic_add_black_24dp, mAction)
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ResultFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): ResultFragment {
            val fragment = ResultFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
