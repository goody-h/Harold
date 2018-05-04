package com.orsteg.harold.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import com.orsteg.harold.utils.event.Event
import com.orsteg.harold.utils.event.NotificationScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val b = intent.extras
        if (b != null) {

            val prefs = Preferences(context, Preferences.EVENT_PREFERENCES)

            val s = prefs.mPrefs.getInt("event.day.time.start", 0) * TimeConstants.HOUR
            val e = prefs.mPrefs.getInt("event.day.time.end", 24) * TimeConstants.HOUR

            val start = b.getInt(Event.START_TIME)
            val end = b.getInt(Event.END_TIME)

            if (start in s..(e - 1) && end > s && end <= e){
                NotificationScheduler.showNotification(context, b.getInt(Event.NOTIFICATION_ID), b)

            }

        }
    }
}
