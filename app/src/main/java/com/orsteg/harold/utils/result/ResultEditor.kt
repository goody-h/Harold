package com.orsteg.harold.utils.result

import android.content.Context
import com.orsteg.harold.database.EventDatabase
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import com.orsteg.harold.utils.event.Event
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import kotlin.collections.ArrayList

/**
 * Created by goodhope on 4/20/18.
 */
class ResultEditor(private val context: Context) {

    fun addTemplate(stream: InputStream, reset: Boolean, saveState: Boolean = true): Boolean {

        val handler = FileHandler()
        val obj = handler.validateFile(stream)

        return if (obj.optBoolean("validity", false)){
            setToResult(obj.optJSONArray("courses"), reset, saveState)

            true
        } else {
            // get error message
            obj.optString("message", "Read error")

            false
        }

    }

    fun deleteSem(semId: Int) {

        val helper = ResultDataBase(context, semId)

        Preferences(context, Preferences.RESULT_PREFERENCES)
                .mEditor.putInt(Semester.semCount(semId), 0).commit()

        helper.onUpgrade(helper.writableDatabase, 1, 1)

    }


    fun clearResult(){

        var i = 1100
        while (i < 9400) {
            deleteSem(i)
            i += if (i % 1000 == 300) 800
            else 100
        }

        Semester.setCurrentSemester(context, 0)
        Preferences(context, Preferences.RESULT_PREFERENCES).mEditor
                .putString("result.level.current.text", "LEVEL")
                .putString("result.semester.current.text", "Semester")
        deleteNullEvents()

    }


    private fun setToResult(obj: JSONArray, reset: Boolean, saveState: Boolean) {

        if (saveState) saveResultState()

        if (reset) clearResult()

        try {

            val ar = (0 until obj.length())
                    .mapTo(ArrayList<JSONObject>()) { obj.getJSONObject(it) }

            ar.sortWith(Comparator { o1, o2 ->
                val i1 = o1.getInt("semId")
                val i2 = o2.getInt("semId")
                if (i1 > i2) 1 else if (i1 < i2) -1 else 0
            })

            var semId = 0
            var sd: ResultDataBase? = null

            (0 until ar.size)
                    .map { ar[it] }
                    .forEach {
                        if (it.getInt("semId") != semId) {
                            semId = it.getInt("semId")
                            sd = ResultDataBase(context, semId)
                            val fCount = Semester.courseCount(context, semId)
                            if (fCount == 0) sd?.onUpgrade(sd!!.writableDatabase, 1, 1)
                        }

                        sd?.insertCourse(it.getString("title"), it.getString("code")
                                , it.getDouble("unit"), it.optString("grade", ""))
                    }

            Preferences(context, Preferences.RESULT_PREFERENCES).mEditor.putBoolean("result.changed", true).commit()
        } catch (e: JSONException){

        }
    }


    fun deleteNullEvents() {

        for (day in TimeConstants.DAYS) {

            val helper = EventDatabase(context, day)

            if (Event.eventCount(context, day) == 0) helper.onUpgrade(helper.writableDatabase, 1, 1)

            val res = helper.getAllData()

            while (res.moveToNext()) {
                Event(context, res.getInt(0), res.getInt(1), day)
            }

            res.close()
        }
    }

    fun saveResultState() {

        val result = Preferences(context, Preferences.RESULT_PREFERENCES)

        val handler = FileHandler()

        handler.saveResultFile(context)

        val sem = Semester.getCurrentSemester(context)
        val l = result.mPrefs.getString("result.level.current.text", "LEVEL")
        val s = result.mPrefs.getString("result.semester.current.text", "SEM")

        result.mEditor.putString("result.level.previous.text", l)
                .putInt("result.semester.previous", sem)
                .putString("result.semester.previous.text", s).commit()

    }


}
