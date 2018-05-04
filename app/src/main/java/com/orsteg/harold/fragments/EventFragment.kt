package com.orsteg.harold.fragments


import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekLoader
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent

import com.orsteg.harold.R
import com.orsteg.harold.activities.NewEventActivity
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import com.orsteg.harold.utils.event.Event
import com.orsteg.harold.utils.event.NotificationScheduler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * A simple [Fragment] subclass.
 * Use the [EventFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EventFragment : BaseFragment(), WeekView.EventClickListener, WeekLoader.WeekChangeListener, WeekView.EventLongPressListener, WeekView.EmptyViewLongPressListener {
    
    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden){
            eventSet?.visibility = View.GONE
        } else {
            eventSet?.visibility = View.VISIBLE

            mListener?.shoWActionBtn(mAction)
        }
    }

    override val mPrefType: String = Preferences.EVENT_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {

    }

    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        if (inMultiSelect) {
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
    
    private var eventSet: View? = null
    private var mAction: View.OnClickListener? = null

    private var mWeekView: WeekView? = null

    private var mEvents: ArrayList<WeekViewEvent> = ArrayList()

    var inMultiSelect = false

    private var selections: ArrayList<Long> = ArrayList()

    private var empty: View? = null

    private var init = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }

        initiateEvents()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        eventSet = mListener?.getTools(arrayOf(R.id.eventTool_inflater))?.get(0)?.inflate()

        mAction = View.OnClickListener {
            val intent = Intent(context, NewEventActivity::class.java)
            startActivity(intent)
        }

        if (isHidden) {
            eventSet?.visibility = View.GONE
        } else {
            mListener?.shoWActionBtn(mAction)
        }

        return inflater.inflate(R.layout.fragment_event, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewtype = eventSet?.findViewById<View>(R.id.viewtype) as Spinner
        val options = eventSet?.findViewById<View>(R.id.options2)

        val type = mPreferences.mPrefs.getInt("WeekViewType", 0)
        val start = mPreferences.mPrefs.getInt("event.day.time.start", 0)
        val end = mPreferences.mPrefs.getInt("event.day.time.end", 24)


        // Get a reference for the week view in the layout.
        mWeekView = view.findViewById<View>(R.id.weekView) as WeekView
        empty = view.findViewById(R.id.empty)

        setupWeekView(type)
        mWeekView?.startOfDay = start
        mWeekView?.endOfDay = end

        // Show a toast message about the touched event.
        mWeekView?.setOnEventClickListener(this)

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        mWeekView?.monthChangeListener = this

        // Set long press listener for events.
        mWeekView?.eventLongPressListener = this

        // Set long press listener for empty view
        mWeekView?.emptyViewLongPressListener = this

        // Set up a date time interpreter to interpret how the date and time will be formatted in
        // the week view. This is optional.
        setupDateTimeInterpreter(false)
        
        val typ = ArrayList<String>()
        typ.add("Day View")
        typ.add("3 Day View")
        typ.add("Week View")

        viewtype.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, typ)

        viewtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!init) {
                    mPreferences.mEditor.putInt("WeekViewType", position).commit()
                    initiateEvents()
                    setupWeekView(position)

                }
                init = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        viewtype.setSelection(type)


        options?.setOnClickListener {
            mListener?.showBottomSheet(R.menu.event_options1, { _, which ->
                when (which) {
                    R.id.now -> mWeekView!!.goToNow()
                    R.id.reset -> WarningDialog(context!!, "This will reset all events. Continue?"){
                        mPreferences.mEditor.putBoolean("harold.event.setup", false).putInt("event.semester.current", 0).commit()
                        mListener?.resetGroup(1)

                        Thread {
                            for (day in TimeConstants.DAYS) {
                                val helper = EventDatabase(context!!, day)
                                helper.onUpgrade(helper.writableDatabase, 1, 1)
                                Event.decreaseCount(context!!, day, Event.eventCount(context!!, day))
                            }

                            mPreferences.commit()

                            NotificationScheduler.cancelAllReminders(context!!)

                        }.start()
                    }.show()
                    R.id.start -> setStartOfDay()
                    R.id.end -> setEndOfDay()
                }
            })
        }

    }


    private fun setupWeekView(position: Int) {
        when (position) {

            0 -> {
                mWeekView?.numberOfVisibleDays = 1

                // Lets change some dimensions to best fit the view.
                mWeekView?.columnGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
                mWeekView?.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics).toInt()
                mWeekView?.eventTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, resources.displayMetrics).toInt()
            }
            1 -> {
                mWeekView?.numberOfVisibleDays = 3

                // Lets change some dimensions to best fit the view.
                mWeekView?.columnGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
                mWeekView?.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics).toInt()
                mWeekView?.eventTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, resources.displayMetrics).toInt()
            }
            2 -> {
                mWeekView?.numberOfVisibleDays = 7

                // Lets change some dimensions to best fit the view.
                mWeekView?.columnGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
                mWeekView?.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics).toInt()
                mWeekView?.eventTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics).toInt()
            }
        }

    }

    private fun initiateEvents() {

        mEvents = ArrayList()

        val days = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

        val s = mPreferences.mPrefs.getInt("event.day.time.start", 0) * TimeConstants.HOUR
        val e = mPreferences.mPrefs.getInt("event.day.time.end", 24) * TimeConstants.HOUR

        for (i in 1 until days.size + 1) {
            val day = days[i - 1]

            val count = Event.eventCount(context!!, day)

            if (count != 0) {

                val helper = EventDatabase(context!!, day)

                val res = helper.getAllData()
                val type = mPreferences.mPrefs.getInt("WeekViewType", 0)

                for (j in 1 until count + 1) {
                    res.moveToPosition(j - 1)
                    val SqL_Id = res.getInt(0)
                    val Course_id = res.getInt(1)
                    val venue = res.getString(2)
                    val start = res.getInt(3)
                    val end = res.getInt(4)

                    val time = Event(context!!, SqL_Id, Course_id, day, j, venue, start, end)

                    if (start in s..(e - 1) && end > s && end <= e) mEvents.add(time.getWeekViewEvent(type))
                }
                res.close()
                helper.close()

            }

        }

        if (empty != null)
            if (mEvents.size == 0) {
                empty?.visibility = View.VISIBLE
            } else
                empty?.visibility = View.GONE

    }

    override fun onStart() {
        super.onStart()

        initiateEvents()
        mWeekView!!.notifyDatasetChanged()

        if (inMultiSelect) exitMultiselect()

    }


    /**
     * Set up a date time interpreter which will show short date values when in week view and long
     * date values otherwise.
     * @param shortDate True if the date values should be short.
     */
    private fun setupDateTimeInterpreter(shortDate: Boolean) {
        mWeekView?.dateTimeInterpreter = object : DateTimeInterpreter {
            override fun interpretDate(date: Calendar): String {
                val weekdayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
                var weekday = weekdayNameFormat.format(date.time)
                val format = SimpleDateFormat(" M/d", Locale.getDefault())

                // All android api level do not have a standard way of getting the first letter of
                // the week day name. Hence we get the first char programmatically.
                // Details: http://stackoverflow.com/questions/16959502/get-one-letter-abbreviation-of-week-day-of-a-date-in-java#answer-16959657
                if (shortDate)
                    weekday = weekday[0].toString()
                return weekday.toUpperCase() + format.format(date.time)
            }

            override fun interpretTime(hour: Int): String {
                return if (hour > 11) (hour - 12).toString() + " PM" else if (hour == 0) "12 AM" else hour.toString() + " AM"
            }
        }
    }


    override fun onEventClick(event: WeekViewEvent?, p1: RectF?) {
        if (inMultiSelect) {

            mEvents.indices
                    .filter { event?.id == mEvents[it].id }
                    .forEach {
                        if (mEvents[it].isSelected) {
                            selections.remove(mEvents[it].id)
                            mEvents[it].setSelected(false, 0)
                            if (selections.size == 0) exitMultiselect()
                        } else {
                            selections.add(mEvents[it].id)
                            mEvents[it].setSelected(true, context!!.resources.getColor(R.color.event_color_01))
                        }
                    }
            mWeekView?.notifyDatasetChanged()

            return
        }
        mListener?.showBottomSheet(R.menu.event_options, { _, which ->
            if (which == R.id.delete) {
                WarningDialog(context!!, "Sure to delete Event?"){
                    Event.delete(context!!, event?.id?.toInt()?:0)
                    Toast.makeText(context, "Delete Success", Toast.LENGTH_SHORT).show()
                    initiateEvents()
                    mWeekView!!.notifyDatasetChanged()
                }.show()
            }
        })
    }

    override fun onWeekChange(): MutableList<out WeekViewEvent> {
        return mEvents
    }

    override fun onEventLongPress(event: WeekViewEvent?, p1: RectF?) {
        if (!inMultiSelect) {

            inMultiSelect = true
            mListener?.hideTabs()
            
            mListener?.setActionBtn(R.drawable.ic_delete_black_24dp, View.OnClickListener{
                WarningDialog(context!!, "Sure to delete Events?"){
                    deleteEvents()
                }.show()
            })

        }


        mEvents.indices
                .filter { event?.id == mEvents[it].id }
                .forEach {
                    if (mEvents[it].isSelected) {
                        selections.remove(mEvents[it].id)
                        mEvents[it].setSelected(false, 0)
                        if (selections.size == 0) inMultiSelect = false
                    } else {
                        selections.add(mEvents[it].id)
                        mEvents[it].setSelected(true, context!!.resources.getColor(R.color.event_color_02))
                    }
                }
        mWeekView?.notifyDatasetChanged()
    }

    override fun onEmptyViewLongPress(p0: Calendar?) {

    }

    fun setStartOfDay() {
        TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, i, _ ->
            val result = mWeekView!!.setStartOfDay(i)
            when (result) {
                0 -> {
                    mPreferences.mEditor.putInt("event.day.time.start", i).commit()
                    initiateEvents()
                    mWeekView!!.notifyDatasetChanged()
                }
                1 -> Toast.makeText(context, "Start time must be less than end time", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }, 0, 0, false).show()
    }

    fun setEndOfDay() {
        TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, i, _ ->
            var i = i
            if (i == 0) i = 24
            val result = mWeekView!!.setEndOfDay(i)
            when (result) {
                0 -> {
                    mPreferences.mEditor.putInt("event.day.time.end", i).commit()
                    initiateEvents()
                    mWeekView!!.notifyDatasetChanged()
                }
                1 -> Toast.makeText(context, "End time must be greater than start time", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }, 0, 0, false).show()
    }

    fun deleteEvents() {
        for (i in selections.indices) {
            Event.delete(context!!, selections[i].toInt())
        }
        
        Toast.makeText(context, "Delete Success", Toast.LENGTH_SHORT).show()

        exitMultiselect()

    }

    fun exitMultiselect() {
        selections.clear()

        inMultiSelect = false

        mListener?.showTabs()

        initiateEvents()

        mWeekView?.notifyDatasetChanged()

        mListener?.setActionBtn(R.drawable.ic_add_black_24dp, mAction)
    }





    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        private val TYPE_DAY_VIEW = 1
        private val TYPE_THREE_DAY_VIEW = 2
        private val TYPE_WEEK_VIEW = 3

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EventFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: Int, param2: String): EventFragment {
            val fragment = EventFragment()
            val args = Bundle()
            args.putInt(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }


        fun timeTOString(time: Long): String {
            var time = time

            val sec: Long = 1000
            val min = sec * 60          /*60000*/
            val hour = min * 60          /*3600000*/

            var hourOfDay = 0
            var meridian = "am"

            while (time >= hour) {
                time -= hour

                hourOfDay++

                if (hourOfDay == 12) {
                    meridian = "pm"
                }
                if (hourOfDay == 13) hourOfDay = 1

            }

            var minute = 0
            while (time >= min) {
                time -= min
                minute++
            }

            return "$hourOfDay:$minute $meridian"
        }
    }

}// Required empty public constructor
