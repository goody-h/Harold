package com.orsteg.harold.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.orsteg.harold.R
import com.orsteg.harold.activities.GradingActivity
import com.orsteg.harold.activities.TemplateBrowserActivity
import com.orsteg.harold.activities.TemplateViewerActivity
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.dialogs.AddDialog
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.event.Event
import com.orsteg.harold.utils.result.*
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : BaseFragment() {
    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            semDisplay?.visibility = View.GONE
            semNav?.visibility = View.GONE

        } else {

            semDisplay?.visibility = View.VISIBLE
            semNav?.visibility = View.VISIBLE

            mListener?.shoWActionBtn(mAction)

        }
    }

    override val mPrefType: String = Preferences.RESULT_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {
        if (state == -1) {
            exitMultiselect()
            return false
        }
        return true
    }

    override fun refresh() {

    }

    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    private var semNav: View? = null
    private var semDisplay: View? = null

    private var mAction: View.OnClickListener? = null

    var Semester_Id: Int = 0
    var GPA: Float = 0.toFloat()
    var TCU = 0.0
    var TCU_Display: TextView? = null
    var GPA_Display: TextView? = null

    var _cgpa: TextView? = null

    var result: ListView? = null
    var spin1: Spinner? = null
    var spin2: Spinner? = null
    var arr: array? = null
    private var empty: View? = null
    private var loader: LoaderDialog? = null

    var state: Int = 0

    private var currSem: Int = 0

    private var SelectSem: Int = 0

    private var currlevel: Int = 0

    private var init: Boolean = false

    private var bottomBtn: Button? = null

    private var toggle: State? = null


    private var gradingSystem: GradingSystem? = null

    private var reset: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val views = mListener?.getTools(arrayOf(R.id.resultTopTool_inflater, R.id.resultTool_inflater))

        semDisplay = views?.get(1)?.inflate()
        semNav = views?.get(0)?.inflate()

        mAction = View.OnClickListener { newCourse() }

        if (isHidden){
            semDisplay?.visibility = View.GONE
            semNav?.visibility = View.GONE
        } else {
            mListener?.shoWActionBtn(mAction)
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spin1 = semNav?.findViewById(R.id.level_select)
        spin2 = semNav?.findViewById(R.id.sem_select)

        state = 0

        reset = false


        result = view.findViewById<View>(R.id.results) as ListView
        empty = view.findViewById(R.id.empty)
        loader = LoaderDialog(context!!)

        gradingSystem = GradingSystem(context!!)

        val h1 = ArrayList<String>()
        val h2 = ArrayList<String>()

        h1.add(mPreferences.mPrefs.getString("result.level.current.text", "LEVEL"))
        h2.add(mPreferences.mPrefs.getString("result.semester.current.text", "SEM"))

        _cgpa = semDisplay?.findViewById<View>(R.id.cgpa) as TextView
        TCU_Display = semDisplay?.findViewById<View>(R.id.tcu) as TextView
        GPA_Display = semDisplay?.findViewById<View>(R.id.gpa) as TextView

        bottomBtn = semDisplay?.findViewById<View>(R.id.options) as Button

        toggle = State()

        spin1?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, h1)
        spin2?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, h2)

        currSem = Semester.getCurrentSemester(context!!)
        SelectSem = 0
        if (currSem != 0) {
            setSem(currSem)
        }

        bottomBtn!!.setOnClickListener {
            mListener?.showBottomSheet(R.menu.result_options) { _, which ->
                when (which) {
                    R.id.save1 -> Thread { ResultEditor(context!!).saveResultState() }.start()
                    R.id.refresh -> resetResultState()
                    R.id.upload -> {
                        reset = true
                        getCurrentTemplate(TemplateViewerActivity.ACTION_UPLOAD)
                    }
                    R.id.current -> toggle!!.setChecked(true)
                    R.id.clear1 -> clearSemester()
                    R.id.clear2 -> clearResult()
                    R.id.grading -> {
                        reset = true
                        val i = Intent(context, GradingActivity::class.java)
                        activity?.startActivity(i)
                    }
                    R.id.set -> {
                        reset = true
                        val intent = Intent(context, TemplateBrowserActivity::class.java)
                        intent.action = TemplateViewerActivity.ACTION_APPLY
                        startActivity(intent)
                    }
                    R.id.make -> {
                        reset = true
                        getCurrentTemplate(TemplateViewerActivity.ACTION_SAVE)
                    }
                }
            }
        }

        toggle!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Semester.setCurrentSemester(context!!, SelectSem)
                mPreferences.mEditor
                        .putString("result.level.current.text", spin1?.selectedItem as String)
                        .putString("result.semester.current.text", spin2?.selectedItem as String).commit()

                currSem = SelectSem
            } else if (currSem == SelectSem) {
                Semester.setCurrentSemester(context!!, 0)
                mPreferences.mEditor
                        .putString("result.level.current.text", "LEVEL")
                        .putString("result.semester.current.text", "SEM").commit()

                currSem = 0
            }
        })

        init = true

        initiate()


    }

    private inner class State internal constructor() {
        internal var listener: CompoundButton.OnCheckedChangeListener

        init {
            listener = CompoundButton.OnCheckedChangeListener { compoundButton, b -> }
        }

        internal fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
            this.listener = listener
        }

        internal fun setChecked(check: Boolean) {
            listener.onCheckedChanged(null, check)
        }
    }


    private fun newCourse() {

        AddDialog(context!!, Semester_Id, false, true){ semId, _ ->
            var curr = false
            val sem = currSem
            val l = mPreferences.mPrefs.getString("result.level.current.text", "LEVEL")
            val s = mPreferences.mPrefs.getString("result.semester.current.text", "SEM")


            if (semId != currSem) {
                currSem = semId
                curr = true
            }
            initiate()

            if (curr) {
                toggle!!.setChecked(false)
                currSem = sem

                Semester.setCurrentSemester(context!!, sem)
                mPreferences.mEditor
                        .putString("result.level.current.text", l)
                        .putString("result.semester.current.text", s).commit()
            }
        }.show()

    }

    fun initiate() {

        _cgpacalc()

        val lel = context!!.resources.getStringArray(R.array.levels)
        val sem = context!!.resources.getStringArray(R.array.semes)
        val levels = ArrayList<String>()
        val num = ArrayList<Level>()

        currlevel = 0

        for (i in 1..9) {
            var showLevel = false
            val l = Level(i * 1000)

            for (j in 1..3) {

                val SemId = i * 1000 + j * 100
                if (Semester.courseCount(context!!, SemId) != 0) {
                    l.sems.add(SemId)
                    l.semn.add(sem[j - 1])
                    showLevel = true
                }
            }
            if (showLevel) {
                if (i * 1000 < currSem && i * 1000 + 1000 > currSem) {
                    currlevel = num.size
                }
                num.add(l)
                levels.add(lel[i - 1])
            }
        }

        if (levels.size != 0) {

            val adapt1 = spinAdapt1(context!!, levels, num)


            spin1?.adapter = adapt1

            spin1?.setSelection(currlevel)


            spin1?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val adapt = parent?.adapter as spinAdapt1
                    val adapt2 = spinAdapt2(context!!, adapt.levels[position].semn,
                            adapt.levels[position].sems)

                    spin2?.adapter = adapt2

                    if (init && position == currlevel) {
                        var pos = 0
                        if (currSem != 0) pos = adapt2.sems.indexOf(currSem)

                        spin2?.setSelection(pos)
                        init = false
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

            spin2?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // TODO: 10/29/2017 get the results here and iterate it into an adapter

                    if (parent?.adapter is spinAdapt2) {
                        val adapt = parent.adapter as spinAdapt2
                        val SemId = adapt.sems[position]
                        setSem(SemId)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        } else {
            result?.adapter = array(context!!)
            result?.emptyView = empty

            val l = ArrayList<String>()
            l.add("LEVEL")
            val s = ArrayList<String>()
            s.add("SEM")

            spin1?.onItemSelectedListener = null
            spin2?.onItemSelectedListener = null
            spin1?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, l)
            spin2?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, s)
            toggle!!.setChecked(false)

            GPA_Display?.text = "0"
            GPA = 0f
            _cgpa?.text = "0"
            TCU = 0.0
            TCU_Display?.text = "0"

        }
    }

    private fun setSem(SemId: Int) {

        Semester_Id = SemId
        TCU = 0.0

        SelectSem = SemId

        TCU_Display?.text = TCU.toString()

        val helper = ResultDataBase(context!!, SemId)
        val res = helper.getAllData()

        val Course_count = Semester.courseCount(context!!, SemId)

        val courses = ArrayList<Course>()

        for (j in 1 until Course_count + 1) {
            res.moveToPosition(j - 1)
            val Course_Id = SemId + j
            val SqL_Id = res.getInt(0)
            val Title = res.getString(1)
            val Code = res.getString(2)
            val Cu = res.getDouble(3)
            val Grade = res.getString(4)

            val cou = Course(context!!, Course_Id, j, Code, Title, Cu, Grade, SqL_Id)

            EditTCU(TCU + cou.cu)

            courses.add(cou)
        }
        res.close()
        helper.close()

        arr = array(context!!)
        arr?.template = courses
        arr?.sem_Id = SemId

        result?.adapter = arr

        _gpacalc()
        _cgpacalc()

        toggle!!.setChecked(SemId == currSem)

    }

    private inner class spinAdapt1(context: Context, objects: List<*>, val levels: ArrayList<Level>) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_dropdown_item, objects)

    inner class spinAdapt2(context: Context, objects: List<*>, val sems: ArrayList<Int>) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_dropdown_item, objects)

    override fun onStart() {
        super.onStart()

        if (reset || mPreferences.mPrefs.getBoolean("result.changed", false)) {
            var curr: Boolean

            val sem = mPreferences.mPrefs.getInt("result.semester.previous", 0)
            val l = mPreferences.mPrefs.getString("result.level.previous.text", "LEVEL")
            val s = mPreferences.mPrefs.getString("result.semester.previous.text", "SEM")

            val count = Semester.courseCount(context!!, SelectSem)


            if (sem != SelectSem) {
                curr = true
                currSem = SelectSem
            } else {
                curr = false
                currSem = SelectSem
            }

            if (count == 0) {
                currSem = sem
                curr = false
            }

            initiate()

            if (curr) {
                toggle!!.setChecked(false)
                currSem = sem
            }

            mPreferences.mEditor.putBoolean("result.changed", false)

            Semester.setCurrentSemester(context!!, sem)
            mPreferences.mEditor
                    .putString("result.level.current.text", l)
                    .putString("result.semester.current.text", s).commit()
            reset = false

        }
        if (state != 0) exitMultiselect()

    }

    override fun onStop() {
        super.onStop()

    }


    inner class array(var context: Context) : BaseAdapter() {
        var template: ArrayList<Course>
        var multidel: ArrayList<Int>
        var sem_Id: Int = 0


        // pointer showing which result is on editmode
        var editMode = -1

        init {
            template = ArrayList()
            multidel = ArrayList()
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

            val holder: viewHolder

            holder = viewHolder()
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

            // TODO: 10/29/2017 apply course deptTemplate


            val v = convertView.findViewById<View>(R.id.result)
            v.setOnClickListener { v -> this@array.onClick(v, position, course) }
            v.setOnLongClickListener { v ->
                this@array.onLongClick(v, position, course)
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
                editmode(convertView.findViewById(R.id.main), position, editor, holder.grade)
            }

            return convertView
        }


        // insert editlayout and initialise it
        fun editmode(v: View, position: Int, editor: View, grade1: TextView?) {
            val spin = editor.findViewById<View>(R.id.gradespin) as Spinner
            val delete = editor.findViewById<View>(R.id.delete) as ImageButton
            val edit = editor.findViewById<View>(R.id.edit) as ImageButton
            val Cunit = editor.findViewById<View>(R.id.unit) as TextView


            delete.setOnClickListener {
                WarningDialog(context, "Do you want to delete this course?"){
                    val unit = template[position].cu
                    val helper = ResultDataBase(context, sem_Id)
                    helper.deleteCourse(template[position].sqlId)


                    template.removeAt(position)
                    EditTCU(TCU - unit)
                    
                    mPreferences.commit()
                    editMode = -1
                    notifyDataSetChanged()
                    _gpacalc()
                    _cgpacalc()

                    deleteEvent()
                }.show()
            }
            edit.setOnClickListener {

                AddDialog(context,  sem_Id, true, false, template[position]){ _, cu ->


                    val s =
                            if (cu % 1 == 0.0) cu.toInt().toString() 
                            else cu.toString() 

                    Cunit.text = s
                    _gpacalc()
                    _cgpacalc()
                    notifyDataSetChanged()
                }.show()

            }


            spin.adapter = gradingSystem!!.gradeAdapter


            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(_parent: AdapterView<*>?, view: View?, _position: Int, _id: Long) {

                    var tt = spin.selectedItem as String
                    val u: Double
                    val Qp: Float

                    if (tt == "Grade") {
                        tt = ""
                        u = 0.0

                    } else {
                        u = template[position].cu

                    }
                    val gp = gradingSystem!!.gradeToScore(_position)

                    template[position].editResult(tt)
                    grade1!!.text = tt

                    Qp = (gp * u).toFloat()

                    mPreferences.mEditor
                            .putFloat(Course.unitPref(template[position].id), u.toFloat())
                            .putFloat(Course.qpPref(template[position].id), Qp).commit()

                    _gpacalc()
                    _cgpacalc()

                }

                override fun onNothingSelected(_parent: AdapterView<*>?) {}
            }

            spin.setSelection(gradingSystem!!.getIndexFromGrade(template[position].grade))

            val s =
                    if (template[position].cu % 1 == 0.0) template[position].cu.toInt().toString() 
                    else template[position].cu.toString() 
            Cunit.text = s


            (v as LinearLayout).addView(editor)

            // use position to edit editor view
        }

        fun onLongClick(v1: View, position: Int, c: Course) {
            // TODO: 10/29/2017 set long click action
            if (editMode != -2) {
                template[position].isSelected = true
                multidel.add(position)
                editMode = -2

                notifyDataSetChanged()

                semNav?.visibility = View.INVISIBLE
                mListener?.hideTabs()
                state = -1

                mListener?.setActionBtn(R.drawable.ic_delete_black_24dp, View.OnClickListener {
                    Collections.sort(multidel) { o1, o2 -> if (o1 > o2) -1 else if (o1 < o2) 1 else 0 }

                    WarningDialog(context, "You are about to delete "
                            + multidel.size + " courses, continue?"){
                        for (i in multidel.indices) {
                            val position1 = multidel[i]
                            val unit = template[position1].cu
                            val helper = ResultDataBase(context, sem_Id)
                            helper.deleteCourse(template[position1].sqlId)


                            template.removeAt(position1)
                            EditTCU(TCU - unit)
                            
                            mPreferences.commit()
                        }
                        multidel.clear()
                        exitMultiselect()

                        _gpacalc()
                        _cgpacalc()

                        deleteEvent()

                    }.show()
                })

                // TODO: 10/29/2017 Make UI changes
            }
            // make changes to the course element and editmode then notifydatasetchange
        }

        fun onClick(v: View, position: Int, c: Course) {
            // TODO: 10/29/2017 set click action

            if (editMode != -2) {
                if (editMode == position)
                    editMode = -1
                else
                    editMode = position

            } else {
                if (!c.isSelected) {
                    template[position].isSelected = true
                    multidel.add(position)
                } else {
                    multidel.remove(position)
                    template[position].isSelected = false

                    if (multidel.size == 0) {

                        exitMultiselect()

                        return
                    }
                }
            }

            notifyDataSetChanged()

            // make changes to the editMode element and notifydatasetchange
        }

        private inner class viewHolder {

            internal var title: TextView? = null
            internal var code: TextView? = null
            internal var grade: TextView? = null
            internal var num: TextView? = null
        }

    }


    fun _gpacalc() {
        var gpa = 0f
        var Tcu = 0f
        var Tqp = 0f


        for (i in arr!!.template.indices) {
            val Course_id = arr!!.template[i].id
            Tcu += mPreferences.mPrefs.getFloat(Course.unitPref(Course_id), 0f)
            Tqp += mPreferences.mPrefs.getFloat(Course.qpPref(Course_id), 0f)
        }
        if (Tcu != 0f) {
            gpa = Tqp / Tcu
        }

        EditGPA(gpa)
    }

    fun _cgpacalc() {

        var cgpa = 0f
        var Tcu = 0f
        var Tqp = 0f

        val Level_count = 9
        for (i in 1 until Level_count + 1) {
            val Levelid = i * 1000
            val Sem_Count = 3
            for (j in 1 until Sem_Count + 1) {
                val SemId = Levelid + j * 100
                val Course_count = Semester.courseCount(context!!, SemId)
                if (Course_count != 0) {
                    for (k in 1 until Course_count + 1) {
                        val Course_id = SemId + k
                        Tcu += mPreferences.mPrefs.getFloat(Course.unitPref(Course_id), 0f)
                        Tqp += mPreferences.mPrefs.getFloat(Course.qpPref(Course_id), 0f)
                    }
                }
            }
        }


        if (Tcu != 0f) {
            cgpa = Tqp / Tcu
        }
        mPreferences.mEditor.putFloat("CGPA", cgpa).commit()
        _cgpa?.text = setgpa(cgpa)

    }

    fun EditTCU(TCu: Double) {
        TCU = TCu


        val s =
                if (TCu % 1 == 0.0) TCu.toInt().toString() 
                else TCu.toString() 

        TCU_Display?.text = s
    }

    fun EditGPA(GPa: Float) {
        GPA = GPa
        GPA_Display?.text = setgpa(GPa)
        mPreferences.mEditor.putFloat("GPA" + Semester_Id, GPa).commit()
    }


    fun exitMultiselect() {
        state = 0

        semNav?.visibility = View.VISIBLE
        mListener?.showTabs()
        arr?.editMode = -1
        if (arr!!.multidel.size != 0) {
            for (i in arr!!.template.indices) {
                arr!!.template[i].isSelected = false
            }
            arr!!.multidel.clear()
        }

        arr?.notifyDataSetChanged()

        mListener?.setActionBtn(R.drawable.ic_add_black_24dp, mAction)
    }

    fun deleteEvent() {

        val td = Thread(Runnable {
            val days = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

            for (m in days.indices) {
                val day = days[m]

                val count = Event.eventCount(context!!, day)

                if (count != 0) {
                    val helper = EventDatabase(context!!, day)

                    val res = helper.getAllData()

                    for (j in 1 until count + 1) {
                        res.moveToPosition(j - 1)
                        val SqL_Id = res.getInt(0)
                        val Course_id = res.getInt(1)

                        Event(context!!, SqL_Id, Course_id, day, j)
                    }
                    res.close()
                    helper.close()
                }
            }
        })
        td.start()
    }
    

    private fun getCurrentTemplate(action: String) {

        val td = Thread(Runnable {

            val handler = FileHandler()

            val file = FileHandler.getTemporaryTemp(context!!)
            handler.createTemporaryFile(context!!)

            val i = Intent(context, TemplateViewerActivity::class.java)
            i.data = Uri.fromFile(file)
            i.action = action
            i.putExtra(TemplateViewerActivity.EXTRA_TEMPORARY, true)
            startActivity(i)
        })

        td.start()
    }


    fun resetResultState() {
        loader!!.show()
        loader!!.hideAbort()
        loader!!.setLoadMessage("Resetting result, please wait...")
        val td = Thread(Runnable {
            val editor = ResultEditor(context!!)

            val file = FileHandler.getResultFile(context!!)

            val worked = editor.addTemplate(FileInputStream(file), true, false)

            if (worked) {
                var curr: Boolean

                val sem = mPreferences.mPrefs.getInt("result.semester.previous", 0)
                val l = mPreferences.mPrefs.getString("result.level.previous.text", "LEVEL")
                val s = mPreferences.mPrefs.getString("result.semester.previous.text", "SEM")

                val count = Semester.courseCount(context!!, SelectSem)


                if (sem != SelectSem) {
                    curr = true
                    currSem = SelectSem
                } else {
                    curr = false
                    currSem = SelectSem
                }

                if (count == 0) {
                    currSem = sem
                    curr = false
                }


                activity?.runOnUiThread {
                    loader!!.dismiss()
                    initiate()
                }
                if (curr) {
                    toggle!!.setChecked(false)
                    currSem = sem
                }

                Semester.setCurrentSemester(context!!, sem)
                mPreferences.mEditor
                        .putString("result.level.current.text", l)
                        .putString("result.semester.current.text", s).commit()

            } else
                activity?.runOnUiThread {
                    loader!!.dismiss()
                    Toast.makeText(context, "Sorry unable to reset result", Toast.LENGTH_SHORT).show()
                }
        })
        td.start()

    }

    fun clearSemester() {

        val td = Thread(Runnable {

            ResultEditor(context!!).saveResultState()
            
            val helper = ResultDataBase(context!!, SelectSem)
            val sem = Semester.getCurrentSemester(context!!)

            Semester.decreaseCount(context!!, SelectSem, Semester.courseCount(context!!, SelectSem))
            helper.onUpgrade(helper.writableDatabase, 1, 1)

            if (sem != SelectSem) {
                currSem = sem
            } else {
                currSem = 0

                Semester.setCurrentSemester(context!!, 0)
                mPreferences.mEditor
                        .putString("result.level.current.text", "LEVEL")
                        .putString("result.semester.current.text", "SEM").commit()
            }
            activity?.runOnUiThread { initiate() }
        })

        td.start()

    }

    fun clearResult() {

        val td = Thread(Runnable {

            ResultEditor(context!!).saveResultState()
            
            for (i in 1..9) {
                for (j in 1..3) {

                    val s = i * 1000 + j * 100
                    if (Semester.courseCount(context!!, s) != 0) {
                        val helper = ResultDataBase(context!!, s)

                        Semester.decreaseCount(context!!, s, Semester.courseCount(context!!, s))
                        helper.onUpgrade(helper.writableDatabase, 1, 1)

                        currSem = 0

                        Semester.setCurrentSemester(context!!, 0)
                        mPreferences.mEditor
                                .putString("result.level.current.text", "LEVEL")
                                .putString("result.semester.current.text", "SEM").commit()
                    }
                }
            }

            activity?.runOnUiThread { initiate() }
        })

        td.start()

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

        fun setgpa(gpa: Float): String {
            return if (gpa == 0f) {
                "0"
            } else {
                if (gpa.toString().length > 4) {
                    gpa.toString().substring(0, 4)
                } else {
                    gpa.toString()
                }
            }
        }
    }
}// Required empty public constructor
