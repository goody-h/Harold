package com.orsteg.harold.utils.firebase

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

/**
 * Created by goodhope on 4/14/18.
 */
object References {

    private val DATABASE_REFERENCE = FirebaseDatabase.getInstance().reference

    private val STORAGE_REFERENCE = FirebaseStorage.getInstance().reference

    val USERS_REF = DATABASE_REFERENCE.child("Users")

    val INSTITUTION_LIST = DATABASE_REFERENCE.child("InstitutionList")

    val DEPARTMENT_LIST = DATABASE_REFERENCE.child("DepartmentList")

    val TEMPLATES_DATABASE = DATABASE_REFERENCE.child("CourseTemplates")

    val INSTITUTIONS_REF = DATABASE_REFERENCE.child("Institutions")

    val TEMPLATES_STORAGE = STORAGE_REFERENCE.child("CourseTemplates")

    fun getEdRef(inst: String, dept: String) = DATABASE_REFERENCE.child("Eds").child(inst.escape()).child(dept.escape())

    fun getInstDepartments(inst: String) = DATABASE_REFERENCE.child("Institutions").child(inst.escape())

    private fun String.escape(): String {
        return this.replace("\\s+".toRegex(), "_")
    }

}