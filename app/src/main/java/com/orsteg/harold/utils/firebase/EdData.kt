package com.orsteg.harold.utils.firebase

/**
 * Created by goodhope on 4/16/18.
 */
class EdName() {
    var name = ""

    constructor(name: String): this() {
        this.name = name
    }
}

class EdData() {
    var name = ""
    var count = 0
}

class EdRequest(val userId: String, val institution: String, val department: String, val status: Boolean = false)