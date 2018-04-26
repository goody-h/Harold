package com.orsteg.harold.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.orsteg.harold.utils.event.Event

/**
 * Created by goodhope on 4/15/18.
 */

class EventDatabase(private val context: Context, private val day: String) : SQLiteOpenHelper(context,
        "harold.event.database", null, 1) {


    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $day (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_2 INTEGER, $COL_3 TEXT, $COL_4 TEXT, $COL_5 INTEGER, $COL_6 INTEGER)")

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $day")

        onCreate(db)
    }

    fun getAllData() = writableDatabase.rawQuery("Select * from $day", null)

    fun insertEvent(courseId: Int, venue: String, lecturer: String, startTime: Int, endTime: Int): Long {

        val content = ContentValues()

        content.put(COL_2, courseId)
        content.put(COL_3, venue)
        content.put(COL_4, lecturer)
        content.put(COL_5, startTime)
        content.put(COL_6, endTime)

        val l = writableDatabase.insert(day, null, content)

        if (l != -1L){
            Event.increaseCount(context, day)
        }

        return l
    }


    fun getEvent(id: Int): Array<Any>? {

        val res = writableDatabase.rawQuery("Select * from $day", null)

        for (i in 0 until res.count) {
            res.moveToPosition(i)

            if (res.getInt(0) == id) {
                return arrayOf(res.getInt(0), res.getInt(1), res.getString(2), res.getString(3), res.getInt(4), res.getInt(5), i + 1)
            }

        }
        res.close()

        return null

    }


    fun updateEventData(id: Int, courseId: Int, venue: String, lecturer: String, startTime: Int, endTime: Int) {

        val content = ContentValues()
        content.put(COL_2, courseId)
        content.put(COL_3, venue)
        content.put(COL_4, lecturer)
        content.put(COL_5, startTime)
        content.put(COL_6, endTime)

        writableDatabase.update(day, content, "ID=?", arrayOf(id.toString()))

    }

    fun updateEventTime(id: Int, startTime: Int, endTime: Int) {

        val content = ContentValues()
        content.put(COL_5, startTime)
        content.put(COL_6, endTime)

        writableDatabase.update(day, content, "ID=?", arrayOf(id.toString()))

    }

    fun deleteData(id: Int): Int {
        val l = writableDatabase.delete(day, "ID=?", arrayOf(id.toString()))

        if (l != 0) Event.decreaseCount(context, day)
        return l
    }


    fun deleteAllData(ids: Array<String>) {

        ids.indices
                .map { writableDatabase.delete(day, "ID=?", arrayOf(ids[it])) }
                .filter { it != 0 }
                .forEach { Event.decreaseCount(context, day) }

    }

    companion object {
        private val COL_2 = "COURSE_ID"
        private val COL_3 = "VENUE"
        private val COL_4 = "LECTURER"
        private val COL_5 = "START_TIME"
        private val COL_6 = "END_TIME"
    }
}