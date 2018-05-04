package com.orsteg.harold.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orsteg.harold.utils.event.NotificationScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.setAllReminders(context)
    }
}
