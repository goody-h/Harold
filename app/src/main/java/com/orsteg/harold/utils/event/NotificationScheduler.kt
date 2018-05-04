package com.orsteg.harold.utils.event

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import com.orsteg.harold.activities.MainActivity
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.utils.app.TimeConstants
import java.util.*
import com.orsteg.harold.R
import com.orsteg.harold.fragments.EventFragment
import com.orsteg.harold.receivers.AlarmReceiver
import com.orsteg.harold.receivers.BootReceiver

/**
 * Created by goodhope on 4/16/18.
 */
object NotificationScheduler {


    fun cancelAllReminders(context: Context) {

        for (i in TimeConstants.DAYS.indices) {

            val count = Event.eventCount(context, TimeConstants.DAYS[i])

            if (count != 0) {

                val database = EventDatabase(context, TimeConstants.DAYS[i])

                val res = database.getAllData()

                while (res.moveToNext()) {
                    val sqlId = res.getInt(0)

                    val nId = (i + 1) * 100 + sqlId

                    cancelReminder(context, nId)
                }
                res.close()
                database.close()
            }
        }
    }

    fun setAllReminders(context: Context) {

        for (i in TimeConstants.DAYS.indices) {

            val count = Event.eventCount(context, TimeConstants.DAYS[i])

            if (count != 0) {

                val database = EventDatabase(context, TimeConstants.DAYS[i])

                val res = database.getAllData()

                for (j in 1 until count + 1) {
                    res.moveToPosition(j - 1)
                    val sqlId = res.getInt(0)
                    val courseId = res.getInt(1)
                    val venue = res.getString(2)
                    val startTime = res.getInt(3)
                    val endTime = res.getInt(4)

                    val event = Event(context, sqlId, courseId, TimeConstants.DAYS[i], j, venue, startTime, endTime)


                    val intent = Intent(context, AlarmReceiver::class.java)

                    intent.putExtras(event.getBundle())

                    setReminder(context, event.startTime, event.endTime, event.notificationId, intent)
                }
                res.close()
                database.close()

            }

        }
    }

    fun setReminder(context: Context, start: Int, end: Int, Id: Int, intent: Intent) {

        val calendar = Calendar.getInstance()

        val dayofWeek = intent.extras?.getInt(Event.DAY_INDEX)?: return

        val cdate = calendar.time
        val date = calendar.time
        date.hours = 0
        date.minutes = 0
        date.seconds = 0


        val cday = calendar.get(Calendar.DAY_OF_WEEK).toLong()

        val daydiff = (dayofWeek - cday) * TimeConstants.DAY

        var date1 = Date(date.time + daydiff + start.toLong())

        val date2 = Date(date.time + daydiff + end.toLong())

        // cancel already scheduled reminders
        cancelReminder(context, Id)

        if (date2.before(cdate))
            date1 = Date(date.time + daydiff + TimeConstants.WEEK + start.toLong())

        // Enable a receiver

        val receiver = ComponentName(context, BootReceiver::class.java)
        val pm = context.packageManager

        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)

        val pintent = PendingIntent.getBroadcast(context, Id, intent, PendingIntent.FLAG_UPDATE_CURRENT)


        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?

        alarm?.setRepeating(AlarmManager.RTC_WAKEUP, date1.time, AlarmManager.INTERVAL_DAY * 7, pintent)

    }

    fun cancelReminder(context: Context, Id: Int) {
        // Disable a receiver
        val inactive = TimeConstants.DAYS.none { Event.eventCount(context, it) > 0 }

        if (inactive) {
            val receiver = ComponentName(context, BootReceiver::class.java)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        }

        val intent1 = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, Id, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        am?.cancel(pendingIntent)
        pendingIntent.cancel()

    }

    fun showNotification(context: Context, id: Int, e: Bundle) {

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)

        val pendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context)

        val notification = builder.setContentTitle(e.getString(Event.C_CODE))
                .setContentText("${e.getString(Event.C_TITLE)} at ${e.getString(Event.VENUE)}")
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setSubText("Lecture Started" + " Ends " + EventFragment.timeTOString(e.getInt(Event.END_TIME).toLong()))
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_filled))
                .setSmallIcon(R.drawable.ic_event_black_24dp)
                .setContentIntent(pendingIntent).build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.notify(id, notification)

    }

}