package com.orsteg.harold.utils.result

import android.content.Context
import android.widget.ArrayAdapter
import com.orsteg.harold.utils.app.Preferences
import java.util.ArrayList

/**
 * Created by goodhope on 4/16/18.
 */

class GradingSystem(private val context: Context) {

    private val gradeStore = Preferences(context, Preferences.RESULT_PREFERENCES)


    private val grades: ArrayList<String>
        get() {
            val grades = ArrayList<String>()

            grades.add("Grade")
            (0..17)
                    .filter { gradeStore.mPrefs.getBoolean("grading.gradeState$it", DEFAULT_GRADE_STATES[it]) }
                    .mapTo(grades) { gradeStore.mPrefs.getString("grading.grade$it", DEFAULT_GRADES[it]) }
            return grades
        }

    private val gradePoints: ArrayList<Float>
        get() {
            return (0..17)
                    .filter { gradeStore.mPrefs.getBoolean("grading.gradeState$it", DEFAULT_GRADE_STATES[it]) }
                    .mapTo(ArrayList()) { gradeStore.mPrefs.getFloat("grading.gradePoint$it", DEFAULT_GRADE_POINTS[it]) }
        }
    

    val gradeAdapter: ArrayAdapter<String>
        get() = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, grades)

    val gradeCount: Int
        get() {
            return DEFAULT_GRADE_STATES.count { it }
        }

    val gradeStates: ArrayList<Boolean>
        get() {
            return (0..17).mapTo(ArrayList()) { gradeStore.mPrefs.getBoolean("grading.gradeState$it", DEFAULT_GRADE_STATES[it]) }
        }

    val allGrades: ArrayList<String>
        get() {
            return (0..17).mapTo(ArrayList()) { gradeStore.mPrefs.getString("grading.grade$it", DEFAULT_GRADES[it]) }
        }

    val allGradePoints: ArrayList<Float>
        get() {
            return (0..17).mapTo(ArrayList()) { gradeStore.mPrefs.getFloat("grading.gradePoint$it", DEFAULT_GRADE_POINTS[it]) }
        }

    fun resetSystem() {

        for (i in 0..17) {
            gradeStore.mEditor.putFloat("grading.gradePoint$i", DEFAULT_GRADE_POINTS[i])
                    .putBoolean("grading.gradeState$i", DEFAULT_GRADE_STATES[i])
        }
        gradeStore.commit()
    }

    fun getIndexFromGrade(grade: String): Int {

        return grades.indices.lastOrNull { grade == grades[it] } ?: 0
    }

    fun gradeToScore(gdSpin: Int): Float {
        
        return if (gdSpin != 0)
            gradePoints[gdSpin - 1]
        else
            0f
    }

    companion object {

        private val DEFAULT_GRADES = arrayOf("A", "AB", "A+", "A-", "B+", "B", "BC", "B-", "C+", "C", "C-", "D+", "D", "D-", "E", "F", "NC", "U")

        private val DEFAULT_GRADE_POINTS = floatArrayOf(5.0f, 3.0f, 4.0f, 3.7f, 3.3f, 4.0f, 2.5f, 2.7f, 2.3f, 3.0f, 1.7f, 1.0f, 2.0f, 0.7f, 1.0f, 0.0f, 0.0f, 0.0f)

        private val DEFAULT_GRADE_STATES = booleanArrayOf(true, false, false, false, false, true, false, false, false, true, false, false, true, false, true, true, false, false)
    }
}
