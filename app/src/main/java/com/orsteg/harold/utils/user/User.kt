package com.orsteg.harold.utils.user

import android.content.Context
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants

/**
 * Created by goodhope on 4/14/18.
 */
open class User() {

    var userName = ""
    var department = ""
    var institution = ""
    var canUpload = true
    var lastUpdate = 0L
    var tempCount = 0L

    constructor(userName: String, department: String, institution: String, canUpload: Boolean,
                lastUpdate: Long = TimeConstants.getTime(), tempCount: Long = 0) : this() {
        this.userName = userName
        this.department = department
        this.institution = institution
        this.canUpload = canUpload
        this.lastUpdate = lastUpdate
        this.tempCount = tempCount
    }

    fun getEdKey() = "$institution%&$department"
}

class AppUser (user: User, val userId: String) : User(user.userName, user.department, user.institution, user.canUpload,
        user.lastUpdate, user.tempCount) {


    fun persistUser(context: Context){
        val prefs = Preferences(context)

        prefs.mEditor.putString("user.userName", userName)
        prefs.mEditor.putString("user.department", department)
        prefs.mEditor.putString("user.institution", institution)
        prefs.mEditor.putBoolean("user.canUpload", canUpload)
        prefs.mEditor.putLong("user.lastUpdate", lastUpdate)
        prefs.mEditor.putLong("user.tempCount", tempCount)
        prefs.mEditor.putString("user.userId", userId)

        prefs.mEditor.putBoolean("user.hasState", true)

        prefs.commit()
    }

    fun saveUserState(outState: Bundle){
        outState.putString("user.userName", userName)
        outState.putString("user.department", department)
        outState.putString("user.institution", institution)
        outState.putBoolean("user.canUpload", canUpload)
        outState.putLong("user.lastUpdate", lastUpdate)
        outState.putLong("user.tempCount", tempCount)
        outState.putString("user.userId", userId)

        outState.putBoolean("user.hasState", true)
    }

    fun updateUser(user: User): Boolean{
        return lastUpdate != user.lastUpdate
    }

    fun getUserReference() = FirebaseDatabase.getInstance().reference.child("Users/$userId")

    companion object {

        val MAX_UPLOADS = 19

        fun getPublicEdKey(inst: String, dept: String) = "$inst%&$dept"

        fun getPersistentUser(context: Context): AppUser?{

            val prefs = Preferences(context)

            if (prefs.mPrefs.getBoolean("user.hasState", false)) {

                val user = User(prefs.mPrefs.getString("user.userName", ""),
                        prefs.mPrefs.getString("user.department", ""),
                        prefs.mPrefs.getString("user.institution", ""),
                        prefs.mPrefs.getBoolean("user.canUpload", true),
                        prefs.mPrefs.getLong("user.lastUpdate", 0),
                        prefs.mPrefs.getLong("user.tempCount", 0))
                val userId = prefs.mPrefs.getString("user.userId", "")

                return AppUser(user, userId)
            }

            return null
        }

        fun getSavedState(savedState: Bundle): AppUser?{
            if (savedState.getBoolean("user.hasState", false)) {

                val user = User(savedState.getString("user.userName"),
                        savedState.getString("user.department"),
                        savedState.getString("user.institution"),
                        savedState.getBoolean("user.canUpload"),
                        savedState.getLong("user.lastUpdate"),
                        savedState.getLong("user.tempCount"))
                val userId = savedState.getString("user.userId")

                return AppUser(user, userId)
            }

            return null
        }

        fun signOut(context: Context){
            FirebaseAuth.getInstance().signOut()

            clearPersistence(context)
        }

        fun clearPersistence(context: Context){
            val prefs = Preferences(context)

            prefs.mEditor.putString("user.userName", "")
            prefs.mEditor.putString("user.department", "")
            prefs.mEditor.putString("user.institution", "")
            prefs.mEditor.putBoolean("user.canUpload", false)
            prefs.mEditor.putLong("user.lastUpdate", 0)
            prefs.mEditor.putLong("user.tempCount", 0)
            prefs.mEditor.putString("user.userId", "")
            prefs.mEditor.putBoolean("user.hasState", false)

            prefs.commit()
        }
    }
}