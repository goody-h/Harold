package com.orsteg.harold.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.alamkanak.weekview.WeekViewUtil
import java.util.*
import com.orsteg.harold.R
import com.orsteg.harold.activities.TemplateViewerActivity
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.result.Template
import com.orsteg.harold.utils.result.TemplateManager
import com.orsteg.harold.utils.user.AppUser

/**
 * Created by goodhope on 4/28/18.
 */
class DetailsDialog(context: Context, private val mTemplate: TemplateManager) : Dialog(context) {

    private val mListener: DetailsDialogInterface? = if (context is DetailsDialogInterface) context else null
    private var dialog: LoaderDialog = LoaderDialog(context)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.temp_details_dialog)

        val down = findViewById<Button>(R.id.down)
        val edit = findViewById<ImageButton>(R.id.edit)
        val delete = findViewById<ImageButton>(R.id.delete)

        val nameT = findViewById<TextView>(R.id.name)
        val byT = findViewById<TextView>(R.id.by)
        val dateT = findViewById<TextView>(R.id.date)
        val noteT = findViewById<TextView>(R.id.note)

        nameT.text = mTemplate.name
        byT.text = mTemplate.ownerName
        val cal = Calendar.getInstance()
        cal.timeInMillis = mTemplate.lastUpdate
        dateT.text = getDayText(cal)
        noteT.text = mTemplate.note

        down.setOnClickListener {
            val i = Intent(context, TemplateViewerActivity::class.java)
            i.action = mTemplate.iAction
            i.putExtra(TemplateViewerActivity.EXTRA_URL, mTemplate.fileUrl)
            val b = Bundle()
            mListener?.mUser?.saveUserState(b)
            mTemplate.saveState(b)
            i.putExtra(TemplateViewerActivity.EXTRA_TEMPLATE, b)
            i.putExtra("USER", b)

            dismiss()

            mListener?.startActivity(i)
        }

        if (mTemplate.isOwner) {
            delete.visibility = View.VISIBLE
            edit.visibility = View.VISIBLE
            edit.setOnClickListener {
                val save = SaveDialog(context)
                save.show()
                save.save?.setOnClickListener {
                    val name = save.filname
                    save.dismiss()
                    if (mListener?.networkTest() == true) {
                        mListener.uiThread {
                            dialog.show()
                            dialog.hideAbort()
                            dialog.setLoadMessage("Updating Template, please wait...")
                        }
                        updateTemplate(name, save.mNote)
                    } else {
                        dialog.show()
                        dialog.setFailed("No network connection!", { dialog.dismiss() }) {
                            dialog.dismiss()
                            dismiss()
                            mListener?.showDetails(mTemplate.id)
                        }
                    }
                }
            }
            delete.setOnClickListener { deleteTemplate() }

        }
        if (mTemplate.iAction == "") down.visibility = View.GONE
        
    }


    private fun deleteTemplate() {
        WarningDialog(context, "Sure to delete template?"){
            startDelete()
        }.show()
    }

    private fun startDelete() {
        if (mListener?.networkTest() == true) {
            mListener.uiThread {
                dialog.show()
                dialog.hideAbort()
                dialog.setLoadMessage("Deleting Template, please wait...")
            }
            val thisref = References.TEMPLATES_DATABASE.child(mTemplate.id)

            thisref.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val riversRef = References.TEMPLATES_STORAGE.child(mTemplate.id + "tmp.txt")
                    riversRef.delete().addOnCompleteListener { _ ->
                        if (task.isSuccessful) {
                            dismiss()
                            mListener.uiThread {
                                dialog.dismiss()
                                dismiss()
                                Toast.makeText(context, "Delete success", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            setDeleteError()
                        }
                    }
                } else {
                    setDeleteError()
                }
            }
        } else {
            dialog.show()
            dialog.setFailed("No network connection!", { dialog.dismiss() }) {
                dialog.dismiss()
                dismiss()
                mListener?.showDetails(mTemplate.id)
            }
        }

    }

    private fun setDeleteError() {
        mListener?.uiThread { dialog.setFailed("Error deleting template!", { dialog.dismiss() }) { deleteTemplate() } }

    }

    private fun updateTemplate(name: String, note: String) {

        val template = Template(name, note, mTemplate.fileUrl, mListener?.mUser!!.userName, mListener.mUser!!.userId, mListener.mUser!!.getEdKey())

        References.TEMPLATES_DATABASE.child(mTemplate.id).setValue(template)

        mListener.uiThread {
            Toast.makeText(context, "Update successful", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            dismiss()
        }

    }
    
    interface DetailsDialogInterface {
        val mUser: AppUser?
        
        fun networkTest(): Boolean
        fun showDetails(id: String)
        fun startActivity(intent: Intent)
        fun uiThread(run: () -> Unit)
    }


    companion object {

        fun getDayText(date: Calendar): String {

            val today = WeekViewUtil.today()
            if (WeekViewUtil.isSameDay(today, date)) {
                return "today"
            }
            today.add(Calendar.DATE, -1)
            if (WeekViewUtil.isSameDay(today, date)) {
                return "yesterday"
            }

            val months = arrayOf("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER")

            return (months[date.get(Calendar.MONTH)] + " " + date.get(Calendar.DAY_OF_MONTH)
                    + ", " + date.get(Calendar.YEAR))
        }
    }


}