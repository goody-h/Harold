package com.orsteg.harold.activities

import android.content.Context
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.orsteg.harold.R
import com.orsteg.harold.utils.firebase.EdName
import com.orsteg.harold.utils.firebase.EdRequest
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.firebase.ValueListener
import com.orsteg.harold.utils.user.User
import kotlinx.android.synthetic.main.activity_signup.*
import java.util.ArrayList

class SignUpActivity : AppCompatActivity() {

    private var email: EditText? = null
    private var pass: EditText? = null
    private var pass2: EditText? = null
    private var uname: EditText? = null
    private var inst: EditText? = null
    private var dept: EditText? = null
    private var layout: View? = null
    private var prog: View? = null

    private var spinInst: Spinner? = null

    private var spinDept: Spinner? = null


    private var mAuth: FirebaseAuth? = null
    private val mAuthListener: FirebaseAuth.AuthStateListener? = null


    private var mFirebaseInstance: FirebaseDatabase? = null
    private var mUserDatabase: DatabaseReference? = null

    private var institutions: DatabaseReference? = null
    private var department: DatabaseReference? = null
    private var requestInstance: DatabaseReference? = null

    private var request: Boolean = false

    private var deptlistener: ValueListener? = null

    private var instListener: ValueListener? = null

    private var context: Context? = null

    private var username: String = ""
    private var mail: String = ""
    private var password: String = ""
    private var instt: String = ""
    private var deptt: String = ""

    private var mUID: String = ""

    private var iReady: Boolean = false
    private var dReady: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)


        bindViews()

        iReady = false
        dReady = false
        request = false

        context = this

        instt = ""
        deptt = ""

        mAuth = FirebaseAuth.getInstance()

        mFirebaseInstance = FirebaseDatabase.getInstance()

        institutions = mFirebaseInstance!!.getReference("InstitutionList")
        department = mFirebaseInstance!!.getReference("DepartmentList")


        val c = this

        val instu = ArrayList<String>()
        instu.add("Loading institutions...")
        spinInst?.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, instu)


        instListener = object : ValueListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {

                val snapI = dataSnapshot?.children
                val iterator = snapI?.iterator()

                val inst = ArrayList<String>()
                inst.add("Select Institution")

                while (iterator?.hasNext() == true) {
                    val snap = iterator.next()
                    val institution = snap.getValue(EdName::class.java)
                    val name = institution!!.name

                    inst.add(name)
                }

                inst.add("Other...")
                iReady = true

                spinInst?.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, inst)

            }
        }

        val deptn = ArrayList<String>()
        deptn.add("Loading departments...")

        spinDept?.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, deptn)


        deptlistener = object : ValueListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                val snapI = dataSnapshot?.children
                val iterator = snapI?.iterator()

                val dept = ArrayList<String>()
                dept.add("Select Department")

                while (iterator?.hasNext() == true) {
                    val snap = iterator.next()
                    val departments = snap.getValue(EdName::class.java)
                    val name = departments!!.name

                    dept.add(name)
                }
                dept.add("Other...")

                dReady = true

                spinDept?.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, dept)

            }
        }

        spinInst?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != 0 && iReady) {
                    if (parent!= null && position == parent.count - 1) {
                        instt = "N/A"
                        inst?.visibility = View.VISIBLE
                    } else {
                        inst?.visibility = View.GONE
                        instt = parent?.selectedItem as String
                    }
                } else
                    instt = ""

                validate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        spinDept?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != 0 && dReady)
                    if (parent != null && position == parent.count - 1) {
                        deptt = "N/A"
                        dept?.visibility = View.VISIBLE
                    } else {
                        dept?.visibility = View.GONE
                        deptt = parent?.selectedItem as String
                    }
                else
                    deptt = ""

                validate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }


        institutions?.addValueEventListener(instListener)
        department?.addValueEventListener(deptlistener)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                validate()
            }

            override fun afterTextChanged(s: Editable) {

            }
        }

        validate()

        if (!networkTest()) {
            Toast.makeText(this, "Failed to load data: No network connection", Toast.LENGTH_SHORT).show()
        }

        signup.setOnClickListener { v ->
            hideInput()
            signUp(v)
        }

        for(f in arrayOf(email, pass, pass2, uname, dept, inst)){
            f?.addTextChangedListener(watcher)
        }

    }


    private fun hideInput(){
        val v = this.currentFocus
        if (v != null){
            val inp = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inp?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }


    private fun bindViews() {
        email = findViewById(R.id.email)
        pass = findViewById(R.id.password)
        pass2 = findViewById(R.id.password2)
        uname = findViewById(R.id.uname)
        inst = findViewById(R.id.institution)
        dept = findViewById(R.id.department)
        layout = findViewById(R.id.lay1)
        prog = findViewById(R.id.prog)
        spinInst = findViewById(R.id.instn)
        spinDept = findViewById(R.id.deptm)

    }


    override fun onStop() {
        super.onStop()

        institutions?.removeEventListener(instListener!!)
        department?.removeEventListener(deptlistener!!)

    }


    fun signUp(v: View) {

        if (pass?.text.toString() != pass2?.text.toString()) {
            Toast.makeText(this, "Sign up failed: Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        mail = email?.text.toString()
        password = pass?.text.toString()

        if (inst?.visibility == View.VISIBLE) {
            instt = inst?.text.toString()
            request = true
        }
        if (dept?.visibility == View.VISIBLE) {
            deptt = dept?.text.toString()
            request = true
        }

        val Chat = true

        username = uname?.text.toString()

        if (networkTest()) {
            layout?.visibility = View.GONE

            prog?.visibility = View.VISIBLE

            mAuth!!.createUserWithEmailAndPassword(mail, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {

                            mUID = task.result.user.uid

                            mFirebaseInstance = FirebaseDatabase.getInstance()


                            mUserDatabase = mFirebaseInstance!!.getReference("Users").child(mUID)

                            if (request) {
                                val request = EdRequest(mUID, instt, deptt, false)
                                requestInstance = mFirebaseInstance!!.getReference("Requests")
                                val id = requestInstance!!.push().key

                                requestInstance?.child(id)?.setValue(request)
                            }
                            if (inst?.visibility == View.VISIBLE) {
                                References.INSTITUTION_LIST.child(instt.replace("\\s+".toRegex(), "_")).setValue(EdName(instt))
                            }
                            if (dept?.visibility == View.VISIBLE) {
                                References.DEPARTMENT_LIST.child(deptt.replace("\\s+".toRegex(), "_")).setValue(EdName(deptt))
                            }

                            val user = User(username, deptt, instt, Chat)

                            mUserDatabase?.setValue(user)

                            finish()

                        } else {
                            when {
                                task.exception is FirebaseAuthWeakPasswordException -> Toast.makeText(context, "Sign up failed: Weak password", Toast.LENGTH_SHORT).show()
                                task.exception is FirebaseAuthInvalidCredentialsException -> Toast.makeText(context, "Sign up failed: Invalid email", Toast.LENGTH_SHORT).show()
                                task.exception is FirebaseAuthUserCollisionException -> Toast.makeText(context, "Sign up failed: Email already exists", Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(context, "Sign up failed: Network error", Toast.LENGTH_SHORT).show()
                            }
                            layout?.visibility = View.VISIBLE
                            prog?.visibility = View.GONE
                        }
                    }
        } else {
            Toast.makeText(v.context, "Sign up failed: No network connection", Toast.LENGTH_SHORT).show()

        }

    }

    private fun validate() {

        var valid = true

        if (email?.text.toString() == "") {
            valid = false
        }
        if (inst?.visibility == View.VISIBLE && inst?.text.toString() == "") {
            valid = false
        }
        if (dept?.visibility == View.VISIBLE && dept?.text.toString() == "") {
            valid = false
        }
        if (pass?.text.toString() == "") {
            valid = false
        }
        if (pass2?.text.toString() == "") {
            valid = false
        }
        if (instt == "") {
            valid = false
        }
        if (deptt == "") {
            valid = false
        }
        if (uname?.text.toString() == "") {
            valid = false
        }

        findViewById<View>(R.id.signup).isEnabled = valid
    }

    private fun networkTest(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    companion object {
        private val TAG = "MYTAG"
    }

}
