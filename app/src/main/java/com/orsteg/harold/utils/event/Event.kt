package com.orsteg.harold.utils.event

import android.content.Context
import android.os.Bundle
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil
import com.orsteg.harold.R
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import java.util.*

/**
 * Created by goodhope on 4/16/18.
 */
class Event(private val context: Context, var sqlId: Int, var courseId: Int, var day: String,
            var period: Int = 0, var venue: String = "N/A", var lecturer: String = "N/A",
            var startTime: Int = 0, var endTime: Int = 0) {

    var notificationId = (TimeConstants.DAYS.indexOf(day) + 1) * 100 + sqlId
    var dayIndex = TimeConstants.DAYS.indexOf(day) + 1

    var cTitle = ""
    var cCode = ""


    private val database = EventDatabase(context, day)

    private val pref = Preferences(context, Preferences.EVENT_PREFERENCES)

    init {
        getCourse()
    }

    fun addEvent() {

        val count = eventCount(context, day)

        if (count == 0) {
            database.onUpgrade(database.writableDatabase, 1, 1)
        }

        if (period > count) {
            sqlId = database.insertEvent(courseId, venue, lecturer, startTime, endTime).toInt()
        }
    }

    fun editInfo(courseId: Int, venue: String, lecturer: String, startTime: Int, endTime: Int) {

        this.courseId = courseId
        this.venue = venue
        this.lecturer = lecturer
        this.startTime = startTime
        this.endTime = endTime

        database.updateEventData(sqlId, courseId, venue, lecturer, startTime, endTime)
    }


    fun editTime(startTime: Int, endTime: Int) {

        this.endTime = endTime
        this.startTime = startTime
        database.updateEventTime(sqlId, startTime, endTime)
    }

    private fun getCourse() {

        val currSem = pref.mPrefs.getInt(CURRENT_SEM, 0)

        val result = ResultDataBase(context, currSem)

        val obj = result.getCourseBySqlId(courseId)

        if (obj != null) {
            cTitle = obj[1]
            cCode = obj[0]
        } else {

            database.deleteData(sqlId)
            NotificationScheduler.cancelReminder(context, notificationId)
        }
    }


    fun getBundle(): Bundle {

        val b = Bundle()

        b.putString(VENUE, venue)
        b.putString(LECTURER, lecturer)
        b.putInt(START_TIME, startTime)
        b.putInt(END_TIME, endTime)
        b.putString(C_TITLE, cTitle)
        b.putString(C_CODE, cCode)
        b.putInt(DAY_INDEX, dayIndex)
        b.putInt(NOTIFICATION_ID, notificationId)

        return b
    }


    fun getWeekViewEvent(): WeekViewEvent {
        val start = WeekViewUtil.today()

        val startArr = getHandM(startTime)
        start.set(Calendar.DAY_OF_WEEK, dayIndex)
        start.set(Calendar.HOUR_OF_DAY, startArr[0])
        start.set(Calendar.MINUTE, startArr[1])

        val endArr = getHandM(endTime)
        val endTime = start.clone() as Calendar
        endTime.set(Calendar.HOUR_OF_DAY, endArr[0])
        endTime.set(Calendar.MINUTE, endArr[1])

        val event = WeekViewEvent(notificationId.toLong(), "$cCode $cTitle", venue, start, endTime)
        event.color = context.resources.getColor(R.color.event_color_01)

        return event
    }



    private fun getHandM(time: Int)
            = arrayOf(Math.floor((time / 3600000).toDouble()).toInt(), time % 3600000 / 60000)

    companion object {

        val VENUE = "event.venue"
        val LECTURER = "event.lecturer"
        val START_TIME = "event.startTime"
        val END_TIME = "event.endTime"
        val C_TITLE = "event.cTitle"
        val C_CODE = "event.cCode"
        val DAY_INDEX = "event.cTitle"
        val NOTIFICATION_ID = "event.notificationId"
        val CURRENT_SEM = "event.semester.current"
        
        private fun dayCount(day: String) = "event.day.$day.count"

        fun eventCount(context: Context, day: String): Int{
            val pref = Preferences(context, Preferences.EVENT_PREFERENCES)

            return pref.mPrefs.getInt(dayCount(day), 0)
        }

        fun increaseCount(context: Context, day: String, count: Int = 1): Int {
            val pref = Preferences(context, Preferences.EVENT_PREFERENCES)

            val p = pref.mPrefs.getInt(dayCount(day), 0)

            pref.mEditor.putInt(dayCount(day), p + count).apply()

            return p + count
        }

        fun decreaseCount(context: Context, day: String, count: Int = 1): Int {
            val pref = Preferences(context, Preferences.EVENT_PREFERENCES)

            val p = pref.mPrefs.getInt(dayCount(day), 0)

            val n = if (p - count < 0) 0 else p - count

            pref.mEditor.putInt(dayCount(day), n).apply()

            return n
        }

        fun getEventFromId(context: Context, id: Int): Event? {
            val d = Math.floor((id / 100).toDouble()).toInt()
            val i = id % 100
            val database = EventDatabase(context, TimeConstants.DAYS[d - 1])

            val o = database.getEvent(i)

            if (o != null) {

                return Event(context, o[0] as Int, o[1] as Int, TimeConstants.DAYS[d - 1], o[6] as Int,
                        o[2] as String, o[3] as String, o[4] as Int, o[5] as Int)
            }

            return null
        }

        fun delete(context: Context, id: Int) {

            val dayN = Math.floor((id / 100).toDouble()).toInt() - 1

            val sqlId = id % 100

            val day = TimeConstants.DAYS[dayN]

            val database = EventDatabase(context, day)

            database.deleteData(sqlId)

            NotificationScheduler.cancelReminder(context, id)

        }
    }


}
