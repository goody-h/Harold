package com.orsteg.harold.services

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.orsteg.harold.utils.app.Preferences

class AppIdInstance : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {

        val refreshedToken = FirebaseInstanceId.getInstance().token

        val prefs = Preferences(baseContext)
        prefs.mEditor.putString("com.harold.device.token", refreshedToken).commit()

    }
}
