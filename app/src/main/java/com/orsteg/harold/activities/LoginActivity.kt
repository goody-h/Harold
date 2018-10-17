package com.orsteg.harold.activities

import android.content.Context
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.orsteg.harold.R
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {


    private var e_mail: EditText? = null
    private var pass: EditText? = null
    private var context: Context? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var prog: View? = null
    private var form: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        e_mail = findViewById<View>(R.id.email) as EditText
        pass = findViewById<View>(R.id.password) as EditText
        prog = findViewById(R.id.prog)
        form = findViewById(R.id.form)

        context = this

        mAuth = FirebaseAuth.getInstance()

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                mAuth!!.removeAuthStateListener(mAuthListener!!)
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)

                finish()
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            // ...
        }
        mAuth?.addAuthStateListener(mAuthListener!!)

        login.setOnClickListener{_ ->
            hideInput()
            login()
        }

    }

    private fun hideInput(){
        val v = this.currentFocus
        if (v != null){
            val inp = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inp?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }


    private fun login() {

        if (validate()) {
            if (networkTest()) {
                prog?.visibility = View.VISIBLE
                form?.visibility = View.GONE
                val email = e_mail?.text.toString()
                val password = pass?.text.toString()
                mAuth?.signInWithEmailAndPassword(email, password)
                        ?.addOnCompleteListener(this) { task ->
                            if (!task.isSuccessful) {
                                when {
                                    task.exception is FirebaseAuthInvalidUserException -> Toast.makeText(context, "This user does not exists. Do you want to sign up", Toast.LENGTH_SHORT).show()
                                    task.exception is FirebaseAuthInvalidCredentialsException -> Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                                    else -> Toast.makeText(context, "Sign in failed: Network error", Toast.LENGTH_SHORT).show()
                                }

                                prog?.visibility = View.GONE
                                form?.visibility = View.VISIBLE
                            }
                        }
            } else {
                Toast.makeText(context, "Sign in failed: No network connection", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please complete the login form!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validate(): Boolean {
        return e_mail?.text.toString() != "" && pass?.text.toString() != ""
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
