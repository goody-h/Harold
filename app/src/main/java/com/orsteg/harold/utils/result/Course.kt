package com.orsteg.harold.utils.result

import android.content.Context
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.utils.app.Preferences
import java.util.ArrayList

/**
 * Created by goodhope on 4/15/18.
 */

class Course(context: Context, var id: Int, var courseNo: Int, var code: String, var title: String,
             var cu: Double, var grade: String, var sqlId: Int) {

    private val semId = id - courseNo
    private val database = ResultDataBase(context, semId)
    private val prefs = Preferences(context, Preferences.RESULT_PREFERENCES)

    var isSelected = false
    
    init {

        val count = prefs.mPrefs.getInt(Semester.semCount(semId), 0)

        if (count == 0) {
            database.onUpgrade(database.writableDatabase, 1, 1)
        }

        val u: Double = if (grade == "") {
            0.0
        } else {
            cu
        }
        
        val system = GradingSystem(context)

        val qp = system.gradeToScore(system.getIndexFromGrade(grade)) * u

        prefs.mEditor
                .putFloat(unitPref(id), u.toFloat())
                .putFloat(qpPref(id), qp.toFloat())
        
        if (courseNo > count) {
            prefs.mEditor
                    .putFloat(unitPref(id), 0f)
                    .putFloat(qpPref(id), 0f)
                    .putInt(Semester.semCount(semId), courseNo)

            this.sqlId = database.insertCourse(title, code, cu, grade).toInt()
        }
        
        prefs.commit()

    }

    fun editInfo(title: String, code: String, unit: Double) {
        this.title = title
        this.code = code
        this.cu = unit
        
        val newUnit: Float
        val newQp: Float
        if (prefs.mPrefs.getFloat(unitPref(id), 0f) == 0f) {
            newUnit = 0f
            newQp = 0f
        } else {
            val gp = prefs.mPrefs.getFloat(qpPref(id), 0f) / prefs.mPrefs.getFloat(unitPref(id), 0f)
            newUnit = unit.toFloat()
            newQp = (unit * gp).toFloat()
        }

        prefs.mEditor
                .putFloat(unitPref(id), newUnit)
                .putFloat(qpPref(id), newQp)
                .commit()
        database.updateCourseData(sqlId, title, code, unit)
    }


    fun editResult(grade: String) {
        this.grade = grade
        database.updateCourseResult(sqlId, grade)
    }


    fun editId(id: Int, no: Int) {
        val previousId = this.id
        this.id = id
        courseNo = no
        prefs.mEditor
                .putFloat(unitPref(id), prefs.mPrefs.getFloat(unitPref(previousId), 0f))
                .putFloat(qpPref(id), prefs.mPrefs.getFloat(qpPref(previousId), 0f))
                .commit()
    }

    companion object {

        fun unitPref(id: Int) = "course.$id.unit"

        fun qpPref(id: Int) = "course.$id.qp"

    }

}

object Semester{

    private val CURRENT = "result.semester.current"


    fun semCount(semId: Int) = "result.semester.$semId.count"

    fun semName(semId: Int) = "result.semester.$semId"


    fun courseCount(context: Context, semId: Int): Int{
        val pref = Preferences(context, Preferences.RESULT_PREFERENCES)

        return pref.mPrefs.getInt(semCount(semId), 0)
    }

    fun increaseCount(context: Context, semId: Int, count: Int = 1): Int {
        val pref = Preferences(context, Preferences.RESULT_PREFERENCES)

        val p = pref.mPrefs.getInt(semCount(semId), 0)

        pref.mEditor.putInt(semCount(semId), p + count).apply()

        return p + count
    }

    fun decreaseCount(context: Context, semId: Int, count: Int = 1): Int {
        val pref = Preferences(context, Preferences.RESULT_PREFERENCES)

        val p = pref.mPrefs.getInt(semCount(semId), 0)

        val n = if (p - count < 0) 0 else p - count

        pref.mEditor.putInt(semCount(semId), n).apply()

        return n
    }

    fun setCurrentSemester(context: Context, semId: Int){
        val pref = Preferences(context, Preferences.RESULT_PREFERENCES)

        pref.mEditor.putInt(CURRENT, semId).apply()

    }

    fun getCurrentSemester(context: Context): Int{
        val pref = Preferences(context, Preferences.RESULT_PREFERENCES)

        return pref.mPrefs.getInt(CURRENT, 0)
    }

}

class Level(var Level_Id: Int) {
    var sems: ArrayList<Int> = ArrayList()
    var semn: ArrayList<String> = ArrayList()

}

