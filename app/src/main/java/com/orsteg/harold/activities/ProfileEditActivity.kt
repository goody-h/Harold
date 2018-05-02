package com.orsteg.harold.activities

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.alamkanak.weekview.WeekViewUtil
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.*
import com.orsteg.harold.R
import com.orsteg.harold.dialogs.ListDialog
import com.orsteg.harold.dialogs.PlainDialog
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.firebase.EdName
import com.orsteg.harold.utils.firebase.EdRequest
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.firebase.ValueListener
import com.orsteg.harold.utils.user.AppUser
import java.util.*

class ProfileEditActivity : AppCompatActivity() {

    private var mUserDatabase: DatabaseReference? = null

    private var mAuth: FirebaseAuth? = null

    private var prefs: Preferences? = null

    private var mUser: AppUser? = null

    private var context: Context? = null
    private var activity: Activity? = null

    private var depts: ArrayList<String>? = null
    private var dDialog: ListDialog? = null
    private var iDialog: ListDialog? = null
    private var insts: ArrayList<String>? = null

    private var iactive: Boolean = false
    private var dactive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        val bundle = intent.extras!!.getBundle("USER")

        depts = ArrayList()
        insts = ArrayList()

        dactive = false
        iactive = false

        mUser = AppUser.getSavedState(bundle)

        prefs = Preferences(this)

        mAuth = FirebaseAuth.getInstance()

        mUserDatabase = mUser?.getUserReference()

        context = this
        activity = this

        fetchData()

        setSupportActionBar(findViewById<View>(R.id.toolbar4) as Toolbar)
        supportActionBar?.setTitle("Edit Profile")

        val profile = findViewById<ListView>(R.id.details)

        val items = arrayOf(ListItem("Username", mUser!!.userName, View.OnClickListener {
            startPlainDialog("Enter your userName", object : PlainDialogClick {
                override fun onClick(dialog: PlainDialog) {
                    if (networkTest()) {
                        val u = dialog.value?.text.toString()
                        if (u != "")
                            mUserDatabase!!.child("userName").setValue(u)
                                    .addOnSuccessListener {
                                        val t = Calendar.getInstance().timeInMillis
                                        mUserDatabase?.child("lastUpdate")?.setValue(t)
                                        mUser?.userName = u
                                        mUser?.lastUpdate = t
                                        ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).items[0].value = u
                                        runOnUiThread {
                                            setLastUpdate()
                                            ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).notifyDataSetChanged()
                                            (findViewById<View>(R.id.userName) as TextView).text = mUser?.userName
                                        }
                                        Toast.makeText(context, "Username updated", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { Toast.makeText(context, "Username not set: Connection error", Toast.LENGTH_SHORT).show() }
                        dialog.dismiss()
                        Toast.makeText(context, "Updating userName", Toast.LENGTH_SHORT).show()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(dialog.context, "Username not set: No network connection", Toast.LENGTH_SHORT).show()
                    }
                }
            }, mUser!!.userName)
        }), ListItem("Email", if (mAuth?.currentUser != null) mAuth?.currentUser?.email!! else "not available", View.OnClickListener {
            startPlainDialog("Enter your email", object : PlainDialogClick {
                override fun onClick(dialog: PlainDialog) {
                    if (networkTest()) {

                        val user = mAuth!!.currentUser
                        if (user != null) {
                            dialog.save?.text = "Verify"
                            dialog.head?.text = "Verify your password"
                            val e = dialog.value?.text.toString()
                            dialog.value?.visibility = View.GONE
                            dialog.auth?.visibility = View.VISIBLE
                            dialog.save?.setOnClickListener {
                                val a = EmailAuthProvider.getCredential(mAuth?.currentUser?.email!!, dialog.auth?.text.toString())
                                mAuth?.currentUser?.reauthenticate(a)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        user.updateEmail(e).addOnCompleteListener { task ->
                                            when {
                                                task.isSuccessful -> {
                                                    val t = Calendar.getInstance().timeInMillis
                                                    mUserDatabase!!.child("lastUpdate").setValue(t)
                                                    mUser!!.lastUpdate = t
                                                    ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).items[1].value = e
                                                    activity!!.runOnUiThread {
                                                        setLastUpdate()
                                                        ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).notifyDataSetChanged()
                                                    }

                                                    Toast.makeText(context, "Email Address updated", Toast.LENGTH_SHORT).show()

                                                }
                                                task.exception is FirebaseAuthInvalidCredentialsException -> Toast.makeText(context, "Email not set: Invalid email", Toast.LENGTH_SHORT).show()
                                                task.exception is FirebaseAuthUserCollisionException -> Toast.makeText(context, "Email not set: Email already used", Toast.LENGTH_SHORT).show()
                                                else -> Toast.makeText(context, "Email not set: Connection error", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        dialog.dismiss()
                                        Toast.makeText(context, "Updating email", Toast.LENGTH_SHORT).show()

                                    } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()

                                    }
                                }
                            }
                        }
                    } else {
                        dialog.dismiss()
                        Toast.makeText(dialog.context, "Email not set: No network connection", Toast.LENGTH_SHORT).show()
                    }
                }
            }, mAuth!!.currentUser!!.email)
        }), ListItem("Institution", mUser!!.institution, View.OnClickListener { view ->
            if (networkTest()) {
                iDialog = ListDialog(view.context, "Pick your institution", insts!!) { adapterView, i ->
                    iactive = false

                    if (i == adapterView.count - 1) {

                        startPlainDialog("Institution full name", object : PlainDialogClick {
                            override fun onClick(dialog: PlainDialog) {
                                val `in` = dialog.value?.text.toString()
                                val request = EdRequest(mAuth?.uid!!, `in`, "", false)
                                val requestInstance = FirebaseDatabase.getInstance().getReference("Requests")
                                val id = requestInstance.push().key
                                requestInstance.child(id).setValue(request)
                                dialog.dismiss()
                                Toast.makeText(context, "Institution requested", Toast.LENGTH_SHORT).show()
                            }
                        }, "")
                        return@ListDialog
                    }
                    val `in` = iDialog!!.listItems[i]
                    mUserDatabase?.child("institution")?.setValue(`in`)
                            ?.addOnSuccessListener {
                                val t = Calendar.getInstance().timeInMillis
                                mUserDatabase?.child("lastUpdate")?.setValue(t)
                                mUser?.institution = `in`
                                mUser?.lastUpdate = t
                                ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).items[2].value = `in`
                                runOnUiThread {
                                    setLastUpdate()
                                    ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).notifyDataSetChanged()
                                }
                                Toast.makeText(context, "Institution updated", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { Toast.makeText(context, "Institution not set: Connection error", Toast.LENGTH_SHORT).show() }
                    Toast.makeText(context, "Updating institution", Toast.LENGTH_SHORT).show()
                }
                iDialog?.show()

                iactive = true
            } else {
                Toast.makeText(view.context, "Institution not set: No network connection", Toast.LENGTH_SHORT).show()

            }
        }), ListItem("Department", mUser!!.department, View.OnClickListener { view ->
            if (networkTest()) {
                dDialog = ListDialog(view.context, "Pick your department", depts!!) { adapterView, i ->
                    dactive = false

                    if (i == adapterView.count - 1) {

                        startPlainDialog("Department full name", object : PlainDialogClick {
                            override fun onClick(dialog: PlainDialog) {
                                val d = dialog.value?.text.toString()
                                val request = EdRequest(mAuth?.uid!!, "", d, false)
                                val requestInstance = FirebaseDatabase.getInstance().getReference("Requests")
                                val id = requestInstance.push().key
                                requestInstance.child(id).setValue(request)
                                dialog.dismiss()
                                Toast.makeText(context, "Department requested", Toast.LENGTH_SHORT).show()
                            }
                        }, "")
                        return@ListDialog
                    }
                    val d = dDialog!!.listItems[i]
                    mUserDatabase?.child("department")?.setValue(d)
                            ?.addOnSuccessListener {
                                val t = Calendar.getInstance().timeInMillis
                                mUserDatabase?.child("lastUpdate")?.setValue(t)
                                mUser?.department = d
                                mUser?.lastUpdate = t
                                ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).items[3].value = d
                                runOnUiThread {
                                    setLastUpdate()
                                    ((findViewById<View>(R.id.details) as ListView).adapter as ListAdapter).notifyDataSetChanged()
                                }
                                Toast.makeText(context, "Department updated", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { Toast.makeText(context, "Department not set: Connection error", Toast.LENGTH_SHORT).show() }
                    Toast.makeText(context, "Updating department", Toast.LENGTH_SHORT).show()
                }
                dDialog?.show()

                dactive = true
            } else {
                Toast.makeText(view.context, "Department not set: No network connection", Toast.LENGTH_SHORT).show()

            }
        }))

        profile.adapter = ListAdapter(this, items)

        profile.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l -> (adapterView.adapter as ListAdapter).items[i].listener.onClick(view) }

        (findViewById<View>(R.id.userName) as TextView).text = mUser!!.userName
        setLastUpdate()
    }

    private fun fetchData() {
        References.DEPARTMENT_LIST.addListenerForSingleValueEvent(object : ValueListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                val snapI = dataSnapshot?.children
                val iterator = snapI?.iterator()

                val dept = ArrayList<String>()

                while (iterator?.hasNext() == true) {
                    val snap = iterator.next()
                    val departments = snap.getValue(EdName::class.java)
                    val name = departments!!.name

                    dept.add(name)
                }
                dept.add("Other...")

                depts = dept

                if (dactive) {
                    dDialog?.listItems = depts!!
                    dDialog?.mList?.adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, depts)
                }

            }
        })
        References.INSTITUTION_LIST.addListenerForSingleValueEvent(object : ValueListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                val snapI = dataSnapshot?.children
                val iterator = snapI?.iterator()

                val inst = ArrayList<String>()

                while (iterator?.hasNext() == true) {
                    val snap = iterator.next()
                    val institution = snap.getValue(EdName::class.java)
                    val name = institution!!.name

                    inst.add(name)
                }

                inst.add("Other...")

                insts = inst

                if (iactive) {
                    iDialog?.listItems = insts!!
                    iDialog?.mList?.adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, insts)
                }

            }

        })
    }


    private interface PlainDialogClick {
        fun onClick(dialog: PlainDialog)
    }

    private fun startPlainDialog(title: String, clicker: PlainDialogClick, def: String?) {
        val dialog = PlainDialog(this, title, 1, def!!)
        dialog.show()
        dialog.save?.setOnClickListener { clicker.onClick(dialog) }
    }

    inner class ListItem internal constructor(var title: String, var value: String, var listener: View.OnClickListener)

    private inner class ListAdapter internal constructor(private val context: Context, val items: Array<ListItem>) : BaseAdapter() {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(i: Int): Any {
            return items[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            val v = LayoutInflater.from(context).inflate(R.layout.profile_item, viewGroup, false)
            val t = v.findViewById<TextView>(R.id.title)
            val va = v.findViewById<TextView>(R.id.value)
            t.text = (getItem(i) as ListItem).title
            va.text = (getItem(i) as ListItem).value
            return v
        }
    }


    private fun setLastUpdate() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = mUser!!.lastUpdate
        (findViewById<View>(R.id.upd_time) as TextView).text = getDayText(cal)

        mUser?.persistUser(this)
    }

    private fun networkTest(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    companion object {
        private val TAG = "MYTAG"

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
