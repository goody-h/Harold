package com.orsteg.harold.utils.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

/**
 * Created by goodhope on 4/14/18.
 */
class AndroidPermissions private constructor() {

    fun requestForWriteExternalStoragePermission(activity: Activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(activity, "Allow External Storage write to use this functionality.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE)
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE)
        }
    }

    fun checkWriteExternalStoragePermission(activity: Activity): Boolean {
        return isGranted(ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    //Write External Storage Permission.
    // function to return true or false based on the permission result
    private fun isGranted(value: Int): Boolean {
        return value == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private var androidPermissions: AndroidPermissions? = null

        val instance: AndroidPermissions = androidPermissions ?: AndroidPermissions()

        // Request Code for request Permissions Must be between 0 to 255.
        //Write External Storage Permission.
        val WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100
    }

}
