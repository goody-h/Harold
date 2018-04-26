package com.orsteg.harold.utils.result

import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.orsteg.harold.utils.app.TimeConstants

/**
 * Created by goodhope on 4/14/18.
 */
open class Template() {

    var name = ""
    var note = ""
    var fileUrl = ""
    var ownerName = ""
    var ownerId = ""
    var edKey = ""
    var lastUpdate = 0L

    constructor(name: String, note: String, fileUrl: String, ownerName: String, ownerId: String,
                edKey: String, lastUpdate: Long = TimeConstants.getTime()) : this() {
        this.name = name
        this.note = note
        this.fileUrl = fileUrl
        this.ownerName = ownerName
        this.ownerId = ownerId
        this.edKey = edKey
        this.lastUpdate = lastUpdate
    }
}

class TemplateManager (temp: Template, val id: String, val iAction: String) : Template(
        temp.name, temp.note, temp.fileUrl, temp.ownerName, temp.ownerId,
        temp.edKey, temp.lastUpdate) {

    var isOwner = false

    init {
        isOwner = FirebaseAuth.getInstance().currentUser?.uid.equals(id)
    }

    fun saveState(bundle: Bundle){
        bundle.putString("t.name", name)
        bundle.putString("t.note", note)
        bundle.putString("t.fileUrl", fileUrl)
        bundle.putString("t.ownerName", ownerName)
        bundle.putString("t.ownerId", ownerId)
        bundle.putString("t.edKy", edKey)
        bundle.putLong("t.lastUpdate",lastUpdate)
        bundle.putString("t.id", id)
    }

    companion object {

        fun restoreState(bundle: Bundle): TemplateManager {
            val temp = Template(bundle.getString("t.name"), bundle.getString("t.note"),
                    bundle.getString("t.fileUrl"), bundle.getString("t.ownerName"),
                    bundle.getString("t.ownerId"), bundle.getString("t.edKy"),
                    bundle.getLong("t.lastUpdate"))

            val id = bundle.getString("t.id")

            return TemplateManager(temp, id, "")
        }
    }

}
