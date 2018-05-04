package com.orsteg.harold.utils.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

/**
 * Created by goodhope on 4/14/18.
 */
class Preferences(context: Context, name: String = APP_PREFERENCES){

    val mPrefs: SharedPreferences = context.getSharedPreferences(name, Activity.MODE_PRIVATE)

    val mEditor: SharedPreferences.Editor = mPrefs.edit()

    fun apply(){
        mEditor.apply()
    }

    fun commit(){
        mEditor.commit()
    }

    companion object {
        val RESULT_PREFERENCES = "harold.result.preferences"
        val EVENT_PREFERENCES = "harold.app.preferences"
        val APP_PREFERENCES = "harold.app.preferences"
    }

}