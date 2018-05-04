package com.orsteg.harold.utils.result

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.storage.UploadTask
import com.orsteg.harold.activities.TemplateViewerActivity
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.SaveDialog
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.user.AppUser
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Created by goodhope on 4/30/18.
 */
class ResultUploader(private val context: Context, private val activity: Activity, private val uri: Uri?, private val file: File?, private val mUser: AppUser) : AsyncTask<Void, Void, Boolean>() {
    private val dialog: LoaderDialog
    private var task: UploadTask? = null

    init {
        this.dialog = LoaderDialog(context)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        dialog.show()
        dialog.setLoadMessage("Validating file, please wait...")
    }

    override fun doInBackground(vararg voids: Void): Boolean? {
        val handler = FileHandler()
        var b: JSONObject? = null
        var valid: Boolean? = false

        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                b = handler.validateFile(inputStream)

            } catch (e: IOException) {
                Log.d(TAG, "getInputData: Read data error")
            }

        } else if (file != null) {
            b = handler.validateFile(FileInputStream(file))
        }

        if (b != null)
            valid = b.getBoolean("validity")

        return valid
    }

    override fun onPostExecute(aBoolean: Boolean?) {
        super.onPostExecute(aBoolean)
        dialog.dismiss()

        if (aBoolean!!) {
            startUpload()
        } else {
            Toast.makeText(context, "Invalid file selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUpload() {
        val save = SaveDialog(context)
        save.show()
        save.save?.setOnClickListener {
            val name = save.filname
            save.dismiss()
            if (networkTest()) {
                activity.runOnUiThread {
                    dialog.show()
                    dialog.hideAbort()
                    dialog.setLoadMessage("Uploading Template, please wait...")
                }
                uploadFile(name, save.mNote, uri)
            } else {
                dialog.show()
                dialog.setFailed("No network connection!", { dialog.dismiss() }) {
                    dialog.dismiss()
                    (activity as TemplateViewerActivity).uploadTemplate()
                }
            }
        }
    }

    private fun uploadFile(name: String, note: String, uri: Uri?) {

        val key = References.TEMPLATES_DATABASE.push().key

        val riversRef = References.TEMPLATES_STORAGE.child(key + "tmp.txt")

        task = riversRef.putFile(uri!!)
        task?.addOnSuccessListener {
            activity.runOnUiThread { dialog.hideAbort() }
            Toast.makeText(context, "upload success", Toast.LENGTH_SHORT).show()

            riversRef.downloadUrl.addOnSuccessListener { uri -> createTemplate(name, uri.toString(), note, key, (activity as TemplateViewerActivity).temporaryF) }
        }?.addOnFailureListener {
            activity.runOnUiThread {
                dialog.setFailed("Template upload failed!", { dialog.dismiss() }) {
                    dialog.dismiss()
                    startUpload()
                }
            }
            Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
        }
        activity.runOnUiThread {
            dialog.setCancelable {
                task?.cancel()
                dialog.dismiss()
            }
        }
    }

    private fun createTemplate(name: String, uri: String, note: String, key: String, save: Boolean) {

        val template = Template(name, note, uri, mUser.userName, mUser.userId, mUser.getEdKey())

        References.TEMPLATES_DATABASE.child(key).setValue(template)

        if (save)
            (activity as TemplateViewerActivity).save(name, object : TemplateViewerActivity.OnCompleteListener {
                override fun onComplete() {
                    activity.runOnUiThread {
                        dialog.dismiss()
                        activity.showAd()
                    }
                }

                override fun onFailure() {
                    activity.runOnUiThread { dialog.dismiss() }
                }
            })
        else
            activity.runOnUiThread {
                dialog.dismiss()
                (activity as TemplateViewerActivity).showAd()
            }

    }

    private fun networkTest(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    companion object {
        private val TAG = "MYTAG"
    }


}
