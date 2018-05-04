package com.orsteg.harold.utils.result

import android.content.Context
import android.os.Environment
import android.text.Html
import android.util.Xml
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.orsteg.harold.database.ResultDataBase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by goodhope on 4/14/18.
 */
class FileHandler {

    fun validateFile(stream: InputStream) = readFile(stream)

    private fun readFile(stream: InputStream): JSONObject {

        val x = Xml.newPullParser()
        var data = "{}"
        var type = ""
        var result = JSONObject()

        x.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        x.setInput(stream, null)

        try {

            x.nextTag()
            x.require(XmlPullParser.START_TAG, null, "harold")

            val version = x.getAttributeValue(null, "ver")


            if (version.toDouble() < Math.floor(VERSION.toDouble() + 1.0)) {

                while (x.next() != XmlPullParser.END_TAG) {
                    if (x.eventType != XmlPullParser.START_TAG) continue

                    val name = x.name

                    if (name == "data") {
                        x.require(XmlPullParser.START_TAG, null, "data")

                        type = x.getAttributeValue(null, "type")

                        if (x.next() == XmlPullParser.TEXT) {
                            data = x.text
                        }

                    } else {
                        var i = 1
                        while (i != 0) {
                            x.next()

                            when (x.eventType) {
                                XmlPullParser.START_TAG -> i++
                                XmlPullParser.END_TAG -> i--
                            }
                        }
                    }
                }
            }

            result = findCourses(JSONArray("[$data]"))

            stream.close()
        } catch (e: XmlPullParserException){
            result.put("validity", false)
            result.put("message", "")
            stream.close()
        } catch (e: IOException){
            result.put("validity", false)
            result.put("message", "")
            stream.close()
        } catch (e: JSONException){
            result.put("validity", false)
            result.put("message", "")
            stream.close()
        }

        result.put("type", type)

        return result
    }

    private fun findCourses(data: JSONArray): JSONObject {

        val levels = ArrayList<Any>()
        val keys = ArrayList<I>()

        val result = JSONObject()
        val targets = JSONArray()

        var pIndex = -1

        try {

            while (pIndex < keys.size) {

                val k = if (pIndex > -1) keys[pIndex] else I(0, "0")
                val p = if (pIndex > -1) levels[k.parent] else data

                val c: Any = when (p) {
                    is JSONObject -> p.get(k.i)
                    is JSONArray -> p.get(k.i.toInt())
                    else -> 0
                }

                val cKeys = ArrayList<I>()

                if (c is JSONObject) {

                    if (c.has("semId") && verifyCourse(c)) {
                        targets.put(c)
                    } else {
                        levels.add(c)

                        for (ck in c.keys()) {
                            if (c.get(ck) is JSONObject || c.get(ck) is JSONArray)
                                cKeys.add(I(levels.size - 1, ck))
                        }
                        keys.addAll(pIndex + 1, cKeys)
                    }
                } else if (c is JSONArray) {

                    levels.add(c)

                    (0 until c.length())
                            .filter { c[it] is JSONArray || c[it] is JSONObject }
                            .mapTo(cKeys) { I(levels.size - 1, it.toString()) }
                    keys.addAll(pIndex + 1, cKeys)

                }

                pIndex++

            }


            if (targets.length() != 0) {
                result.put("courses", targets)
                result.put("validity", true)
                result.put("message", "")

            } else {
                result.put("validity", false)
                result.put("message", "")
            }
        } catch (e: JSONException){
            result.put("validity", false)
            result.put("message", "")

        } catch (e: Exception){
            result.put("validity", false)
            result.put("message", "")

        }

        return result
    }

    private fun verifyCourse(c: JSONObject): Boolean{

        return VALID_IDS.contains(c.getInt("semId")) && c.has("title") &&
                c.has("code") && c.has("unit")

    }

    private class I(val parent: Int, val i: String)


    fun saveResultFile(context: Context): JSONObject {

        return save(context, false, getResultFile(context))
    }

    fun createTemporaryFile(context: Context): JSONObject {

        return save(context, true, getTemporaryTemp(context))
    }

    fun saveTemplate(context: Context, name: String): JSONObject {

        return save(context, true, getTemplateFile(name))
    }

    private fun save(context: Context, isTemplate: Boolean, file: File): JSONObject {

        val result = JSONObject()

        val dataBuilder = StringBuilder("${tab(2)}{\n${tab(3)}\"courses\": [\n")
        var started = false

        var semId = 1100
        while (semId < 9400) {
            val helper = ResultDataBase(context, semId)
            if (Semester.courseCount(context, semId) == 0) helper.onUpgrade(helper.writableDatabase, 1, 1)
            val res = helper.getAllData()


            while (res.moveToNext()) {

                val courseBuilder = StringBuffer()

                val grade = if (isTemplate)
                    ""
                else
                    res.getString(4)

                courseBuilder
                        .append(if (started) "\n," else "")
                        .append("{ \"semId\"=$semId")
                        .append(", \"title\"=\"${res.getString(1)}\"")
                        .append(", \"code\"=\"${res.getString(2)}\"")
                        .append(", \"unit\"=${res.getDouble(3)}")
                        .append(", \"grade\"=\"$grade\" }")

                dataBuilder.append("${tab(4)}$courseBuilder")

                started = true
            }
            res.close()
            helper.close()

            semId += if (semId % 1000 == 300) 800
            else 100
        }

        dataBuilder.append("\n${tab(3)}]\n${tab(2)}}")


        val data = dataBuilder.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

        val fileData = wrapFile(data, isTemplate)

        val outputStream: FileOutputStream
        try {
            outputStream = FileOutputStream(file)
            outputStream.write(fileData.toByteArray())
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    companion object {

        private val VALID_IDS: ArrayList<Int>
            get() {
                val l = ArrayList<Int>()
                var i = 1100
                while (i < 9400) {
                    l.add(i)
                    i += if (i % 1000 == 300) 800
                    else 100
                }
                return l
            }

        private val HEAD_TEXT = "${tab(2)}AUTOMATICALLY GENERATED FILE." +
                "\n${tab(2)}MAKING CHANGES TO ANY OR ALL PARTS OF THIS FILE MIGHT RENDER IT INVALID FOR USE." +
                "\n${tab(2)}IF A COPYRIGHT TAG IS NOT AT THE END OF THIS FILE THEN THE FILE MIGHT BE BROKEN." +
                "\n${tab(2)}IF AN ERROR OCCURS WHILE OPENING FILE IN THE TEMPLATE VIEWER, PLEASE FEEL FREE TO SEND AN ERROR REPORT THROUGH ANY AVAILABLE CHANNEL."
        private val TEMPLATE_HEAD = "${tab(2)}THIS IS A TEMPLATE FILE (.tmp.txt) USED TO POPULATE THE COURSE LIST. TO SEE PREVIEW OPEN IN THE HAROLD TEMPLATE VIEWER."
        private val RESULT_HEAD = "${tab(2)}THIS IS A PERSONAL RESULT FILE (.res.txt)"
        private val FOOT_TEXT = "${tab(2)}${FirebaseRemoteConfig.getInstance().getString("contact_info").replace("||", "\n${tab(2)}")}"

        private val VERSION = "1.0"

        val EXTERNAL_DIR: File
            get() {
                val f = File(Environment.getExternalStorageDirectory(), "Harold Files/")
                if (!f.exists()) f.mkdirs()
                return f
            }
        val TEMPLATES_DIR: File
            get() {
                val f = File(EXTERNAL_DIR, "Course Templates/")
                if (!f.exists()) f.mkdirs()
                return f
            }

        fun getTemplateFile(name: String) = File(TEMPLATES_DIR, "$name.tmp.txt")
        fun getResultFile(context: Context)  = File(context.filesDir,"RESULT_STATE.tmp.txt")
        fun getTemporaryTemp(context: Context) = File(context.cacheDir, "TEMP_FILE.tmp.txt")

        fun wrapFile(data: String, isTemplate: Boolean = true) =
                "<harold ver=\"$VERSION\">\n" +
                        "${wrapHead(isTemplate)}\n${wrapData(data, isTemplate)}\n${wrapFoot()}\n</harold>"

        private fun wrapHead(isTemplate: Boolean) = "${tab()}<header>\n$HEAD_TEXT\n\n${if (isTemplate) TEMPLATE_HEAD else RESULT_HEAD}\n${tab()}</header>"

        private fun wrapData(data: String, isTemplate: Boolean) = "${tab()}<data type=\"${if (isTemplate) "template" else "result"}\">\n$data\n${tab()}</data>"

        private fun wrapFoot() = "${tab()}<footer>\n$FOOT_TEXT\n\n${tab(2)}Copyright © Orsteg Inc\n${tab()}</footer>"

        private fun tab(c: Int = 1, t: String = "   ") = t.repeat(c)

    }

}