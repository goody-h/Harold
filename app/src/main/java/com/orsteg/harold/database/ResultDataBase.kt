package com.orsteg.harold.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.orsteg.harold.utils.result.Semester

/**
 * Created by goodhope on 4/15/18.
 */
class ResultDataBase(private val context: Context, private val semId: Int) : SQLiteOpenHelper(context,
        "harold.result.database", null, 1) {

    private val semester = Semester.semName(semId)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $semester (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_2 TEXT, $COL_3 TEXT, $COL_4 INTEGER, $COL_5 TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $semester")

        onCreate(db)
    }

    fun getAllData(): Cursor = writableDatabase.rawQuery("Select * from $semester", null)

    fun insertCourse(title: String, code: String, unit: Int, grade: String): Long {

        val content = ContentValues()

        content.put(COL_2, title)
        content.put(COL_3, code)
        content.put(COL_4, unit)
        content.put(COL_5, grade)

        val r = writableDatabase.insert(semester, null, content)

        if (r != -1L) Semester.increaseCount(context, semId)

        return r
    }


    fun getCourse(id: Int): Array<Any>? {

        val res = writableDatabase.rawQuery("Select * from $semester", null)

        if (res.moveToPosition(id - 1)) {
            val data = arrayOf(res.getInt(0), res.getString(1), res.getString(2),
                    res.getInt(3), res.getString(4))
            res.close()

            return data
        }

        return null
    }

    fun getCourseBySqlId(id: Int): Array<String>? {

        val res = writableDatabase.rawQuery("Select * from $semester", null)

        while (res.moveToNext()){
            if (res.getInt(0) == id) {
                val a = arrayOf(res.getString(2), res.getString(1))
                res.close()
                return a
            }
        }

        res.close()

        return null
    }


    fun updateCourseData(id: Int, title: String, code: String, unit: Int) {

        val content = ContentValues()
        content.put(COL_2, title)
        content.put(COL_3, code)
        content.put(COL_4, unit)
        writableDatabase.update(semester, content, "ID=?", arrayOf(id.toString()))

    }

    fun updateCourseResult(id: Int, grade: String) {

        val content = ContentValues()
        content.put(COL_5, grade)
        writableDatabase.update(semester, content, "ID=?", arrayOf(id.toString()))

    }

    fun deleteCourse(id: Int): Int {
        val l = writableDatabase.delete(semester, "ID=?", arrayOf(id.toString()))

        if (l != 0) Semester.decreaseCount(context, semId)
        return l
    }


    fun deleteAllData(ids: Array<String>) {

        ids.indices
                .map { writableDatabase.delete(semester, "ID=?", arrayOf(ids[it])) }
                .filter { it != 0 }
                .forEach { Semester.decreaseCount(context, semId) }
    }

    companion object {
        private val COL_2 = "TITLE"
        private val COL_3 = "CODE"
        private val COL_4 = "UNIT"
        private val COL_5 = "GRADE"
    }
}