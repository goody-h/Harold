package com.orsteg.harold.activities

import android.content.Context
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.activity_password_update.*

class PasswordUpdateActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var oldP: EditText? = null
    private var newP: EditText? = null
    private var confirmP: EditText? = null
    private var prog: View? = null
    private var form: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_update)

        setSupportActionBar(findViewById<View>(R.id.toolbar5) as Toolbar)
        supportActionBar?.title = "Password update"

        mAuth = FirebaseAuth.getInstance()

        oldP = findViewById(R.id.passold)
        newP = findViewById(R.id.passnew)
        confirmP = findViewById(R.id.confirm)

        prog = findViewById(R.id.prog)
        form = findViewById(R.id.form)

        update.setOnClickListener { v ->
            hideInput()
            change(v)
        }
    }


    private fun hideInput(){
        val v = this.currentFocus
        if (v != null){
            val inp = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inp?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun change(v: View) {
        val o = oldP?.text.toString()
        val n = newP?.text.toString()
        val c = confirmP?.text.toString()

        if (o == "") {
            Toast.makeText(this, "Please put your old password", Toast.LENGTH_SHORT).show()
            return
        }
        if (n == "") {
            Toast.makeText(this, "Please put your new password", Toast.LENGTH_SHORT).show()
            return
        }
        if (n.length < 6) {
            Toast.makeText(this, "Password must be more than 5 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (c == "") {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
            return
        }
        if (n != c) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (networkTest()) {
            prog?.visibility = View.VISIBLE
            form?.visibility = View.GONE

            val a = EmailAuthProvider.getCredential(mAuth?.currentUser?.email!!, o)
            mAuth?.currentUser?.reauthenticate(a)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mAuth?.currentUser?.updatePassword(n)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(v.context, "Password updated", Toast.LENGTH_SHORT).show()
                            finish()
                        } else if (task.exception is FirebaseAuthWeakPasswordException) {
                            Toast.makeText(v.context, "Weak password", Toast.LENGTH_SHORT).show()
                        }
                        prog?.visibility = View.GONE
                        form?.visibility = View.VISIBLE
                    }
                } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(v.context, "Incorrect password", Toast.LENGTH_SHORT).show()
                    prog?.visibility = View.GONE
                    form?.visibility = View.VISIBLE
                }
            }

        } else {
            Toast.makeText(v.context, "Password not set: No network connection", Toast.LENGTH_SHORT).show()
        }

    }

    private fun networkTest(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }
}
