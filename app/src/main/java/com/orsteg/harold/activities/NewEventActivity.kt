package com.orsteg.harold.activities

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.*
import com.orsteg.harold.R
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.dialogs.AddDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.event.Event
import com.orsteg.harold.utils.event.NotificationScheduler
import com.orsteg.harold.utils.result.Semester
import java.util.ArrayList

class NewEventActivity : AppCompatActivity() {

    private var title1: TextView? = null
    private var venue: EditText? = null
    private var lect: EditText? = null
    private var code: Spinner? = null
    private var time: Array<Any?> = arrayOfNulls(3)
    private var ok: Button? = null

    private var startBtn: Button? = null
    private var endBtn: Button? = null

    private var times: View? = null
    private var timestate: Int = 0

    private var startTxt: TextView? = null
    private var endtxt: TextView? = null

    private var Start: Int = 0

    private var adapt1: spinAdapt1? = null

    private var end: Int = 0

    private var Day: String = ""

    private var Nday: Int = 0

    private var currSem: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event)


        setSupportActionBar(findViewById<View>(R.id.toolbar3) as Toolbar)
        supportActionBar?.title = "Create New Event"

        startBtn = findViewById(R.id.startBtn)
        endBtn = findViewById(R.id.endBtn)
        startTxt = findViewById(R.id.startTxt)
        endtxt = findViewById(R.id.endTxt)
        times = findViewById(R.id.time)

        timestate = 0

        currSem = Preferences(this, Preferences.EVENT_PREFERENCES).mPrefs.getInt("event.semester.current", 0)

        title1 = findViewById<View>(R.id.title) as TextView
        venue = findViewById<View>(R.id.venue) as EditText
        lect = findViewById<View>(R.id.lecturer) as EditText

        code = findViewById<View>(R.id.code) as Spinner
        ok = findViewById<View>(R.id.ok) as Button

        validate()

        initiateTime()

        initCourseList(0)

        code?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                clearInput()

                if (position == 0) {
                    disableTimeSetup()
                    validate()
                    return
                } else if (position == parent?.adapter?.count!! - 1) {
                    parent.setSelection(0)

                    // Initiate add new course
                    addNew(currSem)

                    return
                }

                val adapt11 = parent.adapter as spinAdapt1

                title1?.text = adapt11.ctitle[position - 1]

                time[0] = parent.selectedItem
                time[1] = adapt11.ctitle[position - 1]
                time[2] = adapt11.sqlid[position - 1]

                enableTimeSetup()
                validate()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        val context = this

        ok?.setOnClickListener {
            val count = Event.eventCount(this, Day)

            val t = Event(context, 0, time[2] as Int, Day,count + 1, venue?.text.toString(), lect?.text.toString(), Start, end)
            t.addEvent()
            setAlarm(t)
            finish()
        }

        startBtn?.setOnClickListener {
            val dia = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                val start = hourOfDay * 3600000 + minute * 60000

                val avail = checkSlot(Day, start)

                if (!avail)
                    Toast.makeText(context, "This slot is not available", Toast.LENGTH_SHORT).show()
                else {
                    Start = start
                    startTxt?.text = "" + hourOfDay + " : " + minute

                    end = 0
                    endtxt?.text = ""
                }
                validate()
            }, 0, 0, false)
            dia.show()
        }

        endBtn?.setOnClickListener {
            val dia = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                val End = hourOfDay * 3600000 + minute * 60000

                val avail = endSlot(Day, End, Start)

                if (!avail)
                    Toast.makeText(context, "This slot is not available", Toast.LENGTH_SHORT).show()
                else {

                    if (startTxt?.text == "") {
                        Toast.makeText(context, "Set Start Time first", Toast.LENGTH_SHORT).show()

                    } else {
                        if (End <= Start) {
                            Toast.makeText(context, "End Time must be greater than start Time", Toast.LENGTH_SHORT).show()
                        } else {
                            end = End
                            endtxt?.text = "" + hourOfDay + " : " + minute
                        }
                    }
                }
                validate()
            }, 0, 0, false)
            dia.show()
        }


    }

    private fun clearInput() {
        venue?.setText("")
        lect?.setText("")
        title1?.text = ""
        if (timestate != 0) findViewById<View>(timestate).setBackgroundColor(resources.getColor(R.color.white))
        times?.visibility = View.GONE
        Day = ""
        Nday = 0
        startTxt?.text = ""
        endtxt?.text = ""
        Start = 0
        end = 0
        timestate = 0
    }

    private fun disableTimeSetup() {
        findViewById<View>(R.id.timesetup).visibility = View.GONE
    }

    private fun enableTimeSetup() {
        findViewById<View>(R.id.timesetup).visibility = View.VISIBLE
    }

    private fun initCourseList(select: Int) {

        val kode = ArrayList<String>()
        val id = ArrayList<Int>()
        val title = ArrayList<String>()

        val helper = ResultDataBase(this, currSem)
        val res = helper.getAllData()
        val Course_count = Semester.courseCount(this, currSem)

        kode.add("Select a course")

        for (j in 1 until Course_count + 1) {
            res.moveToPosition(j - 1)
            val SqL_Id = res.getInt(0)
            val Title = res.getString(1)
            val Code = res.getString(2)

            id.add(SqL_Id)
            title.add(Title)
            kode.add(Code)
        }

        kode.add("Add new Course")
        res.close()

        adapt1 = spinAdapt1(this, kode, id, title)

        code?.adapter = adapt1

        code?.setSelection(select)
    }

    private fun initiateTime() {
        val ts = intArrayOf(R.id.sun, R.id.mon, R.id.tue, R.id.wed, R.id.thu, R.id.fri, R.id.sat)

        val click = View.OnClickListener { view ->
            if (view.id != timestate) {
                times?.visibility = View.VISIBLE
                if (timestate != 0) {
                    findViewById<View>(timestate).setBackgroundColor(resources.getColor(R.color.white))
                    startTxt?.text = ""
                    endtxt?.text = ""
                    Start = 0
                    end = 0
                }

                Day = (view as TextView).text.toString()
                Nday = view.getTag() as Int

                view.setBackgroundColor(resources.getColor(R.color.colorAccent))
                timestate = view.getId()
            } else {

                view.setBackgroundColor(resources.getColor(R.color.white))
                times?.visibility = View.GONE
                Day = ""
                Nday = 0
                startTxt?.text = ""
                endtxt?.text = ""
                Start = 0
                end = 0
                timestate = 0
            }
            validate()
        }

        for (i in ts.indices) {
            findViewById<View>(ts[i]).tag = i + 1
            findViewById<View>(ts[i]).setOnClickListener(click)
        }

        findViewById<View>(R.id.cancel).setOnClickListener { finish() }
        validate()

    }

    private fun addNew(currSem: Int) {

        AddDialog(this, currSem, true, true){_, _ ->
            initCourseList(adapt1?.count!! - 2)
        }.show()
    }

    private inner class spinAdapt1(context: Context, objects: List<*>, val sqlid: ArrayList<Int>, val ctitle: ArrayList<String>) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_dropdown_item, objects)

    private fun endSlot(day: String, tim: Int, Start: Int): Boolean {

        val count = Event.eventCount(this, day)

        var slot = true

        if (count != 0) {

            val helper = EventDatabase(this, day)

            val res = helper.getAllData()

            for (j in 0 until count) {
                res.moveToPosition(j)
                val start = res.getInt(4)
                val end = res.getInt(5)

                if (start > Start && start < tim || end > Start && end < tim) {
                    slot = false
                    break
                }

            }
            res.close()
        }

        return slot

    }

    private fun checkSlot(day: String, tim: Int): Boolean {

        val count = Event.eventCount(this, day)

        var slot = true

        if (count != 0) {

            val helper = EventDatabase(this, day)

            val res = helper.getAllData()

            for (j in 0 until count) {
                res.moveToPosition(j)
                val start = res.getInt(4)
                val end = res.getInt(5)

                if (tim > start && tim < end) {
                    slot = false
                    break
                }

            }
            res.close()
        }

        return slot

    }

    fun setAlarm(event: Event) {
        val intent = Intent()

        intent.putExtras(event.getBundle())

        NotificationScheduler.setReminder(this, event.startTime, event.endTime, event.notificationId, intent)

    }

    private fun validate() {

        var valid = true
        if (startTxt?.text == "" || endtxt?.text == "") valid = false
        ok?.isEnabled = valid
    }

}
