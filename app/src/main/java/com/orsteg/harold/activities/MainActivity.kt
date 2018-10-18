package com.orsteg.harold.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.orsteg.harold.R
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.UpdateDialog
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.fragments.BaseFragment
import com.orsteg.harold.utils.app.FragmentManager
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.firebase.ValueListener
import com.orsteg.harold.utils.result.FileHandler
import com.orsteg.harold.utils.result.ResultEditor
import com.orsteg.harold.utils.result.Semester
import com.orsteg.harold.utils.user.AppUser
import com.orsteg.harold.utils.user.User
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.FileInputStream
import java.util.*

class MainActivity : AppCompatActivity(), BaseFragment.OnFragmentInteractionListener,
        FragmentManager.OnFragmentManagerListener, NavigationView.OnNavigationItemSelectedListener  {

    override var mUser: AppUser? = null
    override var authState: Boolean = false
    override var mInstanceState: Bundle? = null

    private var mFragmentManager: FragmentManager? = null
    private var loader: LoaderDialog? = null

    private var mUserRef: DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var mUserListener: ValueListener? = null
    private var mUserNav: UserNavManager? = null

    private val mRand = Random()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme_NoActionBar)
        setContentView(R.layout.activity_main)

        val prefs = Preferences(this)
        FirebaseRemoteConfig.getInstance().setDefaults(R.xml.remote_config_defaults)

        MobileAds.initialize(this, resources.getString(R.string.admob_app_id))


        mUser = AppUser.getPersistentUser(this)
        authState = prefs.mPrefs.getBoolean("user.hasState", false)

        mFragmentManager = FragmentManager(this, container, nav_view, supportFragmentManager)

        if (savedInstanceState != null){
            mUser = AppUser.getSavedState(savedInstanceState)
            authState = savedInstanceState.getBoolean("user.hasState", false)

            mInstanceState = savedInstanceState
            mFragmentManager?.restoreState(savedInstanceState)
        }

        setSupportActionBar(toolbar)
        initNav()

        mUserNav = UserNavManager(this, nav_view, mUser)

        loader = LoaderDialog(this)

        mAuth = FirebaseAuth.getInstance()

        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser

            if (user != null) {
                // User is signed in

                // Set Activity Parameters
                if (!authState) {
                    authState = true
                    mUserNav?.loadUser()
                }
                mUserNav?.updateEmail(user.email)
                authState = true

                getCurrentUser(user)

            } else {
                // User is signed out

                if (mUser != null) {
                    mUser = null
                    AppUser.clearPersistence(this)
                }

                if (authState) {
                    authState = false
                    mUserNav?.updateUser(mUser)
                    mUserRef?.removeEventListener(mUserListener)
                    mUserRef = null
                    mUserListener = null
                }

            }

        }

        mFragmentManager?.initFragment()

        FirebaseRemoteConfig.getInstance().fetch(TimeConstants.DAY/1000)

        checkVersion()


    }

    class UserNavManager(private val context: Context, navView: NavigationView, private var user: AppUser?) {
        private val header = navView.getHeaderView(0)
        private val email = header.findViewById<TextView>(R.id.email)
        private val username = header.findViewById<TextView>(R.id.userName)
        private val login = header.findViewById<View>(R.id.login)
        private val signup = header.findViewById<View>(R.id.signup)
        private val offlineView = header.findViewById<View>(R.id.offline)
        private val onlineView = navView.menu.findItem(R.id.user)
        private val loader = header.findViewById<View>(R.id.loader)

        init {
            login.setOnClickListener {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }

            signup.setOnClickListener {
                val intent = Intent(context, SignUpActivity::class.java)
                context.startActivity(intent)
            }

            setViews()
        }

        fun loadUser() {
            offlineView.visibility = View.GONE
            loader.visibility = View.VISIBLE
        }

        fun updateUser(user: AppUser?){
            this.user = user
            loader.visibility = View.GONE
            setViews()
        }

        fun updateEmail(email: String?){
            this.email.text = email
        }

        private fun setViews() {
            if(user != null) {
                onlineView.isVisible = true
                offlineView.visibility = View.GONE
                username.text = user?.userName
            } else {
                onlineView.isVisible = false
                offlineView.visibility = View.VISIBLE
                email.text = "orsteg.apps@gmail.com"
                username.text = "Harold"
            }
        }
    }

    private fun initNav() {
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun checkVersion(){
        val new = FirebaseRemoteConfig.getInstance().getLong("version")
        val current = resources.getString(R.string.version).toLong()

        if (current < new && mRand.nextInt(100) < 60){
            UpdateDialog(this).show()
        }
    }

    private fun getCurrentUser(user: FirebaseUser?) {
        mUserRef?.removeEventListener(mUserListener)

        val mUId = user?.uid
        if (authState && mUId != null) {

            mUserRef = References.USERS_REF.child(mUId)

            mUserListener = object : ValueListener {

                override fun onDataChange(dataSnapshot: DataSnapshot?) {
                    val user1 = dataSnapshot?.getValue<User>(User::class.java)

                    val nUser: AppUser?
                    nUser = if (user1 != null)
                        AppUser(user1, mUId)
                    else
                        null

                    if (nUser != null) {
                        if (mUser == null) {
                            mUser = nUser
                            mUser?.persistUser(this@MainActivity)

                            mUserNav?.updateUser(mUser)
                        } else if (mUser?.updateUser(nUser) == true){

                            mUser = nUser
                            mUser?.persistUser(this@MainActivity)

                            mUserNav?.updateUser(mUser)
                        }
                    } else {

                        AppUser.signOut(this@MainActivity)

                        authState = false
                        mUser = null
                        mUserNav?.updateUser(mUser)

                    }
                }
            }

            mUserRef?.addValueEventListener(mUserListener)
        }
    }


    override  fun refreshFragment(group: Int, option: Int) {
        mFragmentManager?.refreshFragment(group, option)
    }

    override fun onStart() {
        super.onStart()
        mAuth?.addAuthStateListener(mAuthListener!!)
        mUserRef?.addValueEventListener(mUserListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth?.removeAuthStateListener(mAuthListener!!)
        mUserRef?.removeEventListener(mUserListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId

        when (id) {
            R.id.action_help -> {
                val intent = Intent(Intent.ACTION_VIEW)
                val url = FirebaseRemoteConfig.getInstance().getString("help_site")
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_dashboard -> {
                mFragmentManager?.setFragment(0)
            }
            R.id.nav_result -> {
                mFragmentManager?.setFragment(1)
            }
            R.id.nav_grading -> {
                refreshFragment(0, 1)
                refreshFragment(1, 1)
                val i = Intent(this, GradingActivity::class.java)
                startActivity(i)
            }
            R.id.nav_template -> {
                refreshFragment(0, 1)
                refreshFragment(1, 1)
                val intent = Intent(this, TemplateBrowserActivity::class.java)
                val bundle = Bundle()
                mUser?.saveUserState(bundle)
                intent.putExtra("USER", bundle)
                intent.action = TemplateViewerActivity.ACTION_APPLY
                startActivity(intent)
            }
            R.id.nav_save -> Thread {
                val res = ResultEditor(this).saveResultState()
                if (res.optBoolean("success", false)) {
                    runOnUiThread{
                        Toast.makeText(this, "Save Success", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread{
                        Toast.makeText(this, "Failed to save state", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
            R.id.nav_restore -> {
                resetResultState()
            }
            R.id.nav_profile ->{
                val intent = Intent(this, ProfileEditActivity::class.java)
                val bundle = Bundle()
                mUser?.saveUserState(bundle)
                intent.putExtra("USER", bundle)
                startActivity(intent)
            }
            R.id.nav_password -> {
                val intent = Intent(this, PasswordUpdateActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                AppUser.signOut(this)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mUser?.saveUserState(outState)
            outState.putBoolean("user.hasState", authState)
            mFragmentManager?.onSaveInstanceState(outState)
        }
    }

    override fun onBackPressed() {

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            if (mFragmentManager?.onBackPresses() == true)
                super.onBackPressed()
        }

    }

    override fun setFragment(id: Int) {
        mFragmentManager?.setFragment(id)
    }

    override fun resetGroup(which: Int) {
        mFragmentManager?.resetGroup(which)
    }

    override fun showSnackBar(message: String, action: String, btn: View, listener: (View) -> Unit) {
        Snackbar.make(btn, message, Snackbar.LENGTH_LONG)
                .setAction(action, View.OnClickListener(listener))
                .setActionTextColor(resources.getColor(R.color.colorAccent))
                .show()
    }

    override fun showWarning(message: String, action: () -> Unit) {
        WarningDialog(this, message, action).show()
    }

    override fun showLoader(message: String, cancelable: Boolean): Dialog? {

        return null
    }

    override fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    private fun resetResultState() {
        loader!!.show()
        loader!!.hideAbort()
        loader!!.setLoadMessage("Resetting result, please wait...")
        val td = Thread(Runnable {
            val editor = ResultEditor(this)

            val file = FileHandler.getResultFile(this)

            val worked = try {
                editor.addTemplate(FileInputStream(file), true, false)
            } catch (e: Exception) {
                false
            }

            if (worked) {

                runOnUiThread {
                    loader!!.dismiss()
                    refreshFragment(0, 0)
                    refreshFragment(1, 0)

                    Toast.makeText(this, "Reset Success", Toast.LENGTH_SHORT).show()
                }
            } else
                runOnUiThread {
                    loader!!.dismiss()
                    Toast.makeText(this, "Sorry unable to reset result", Toast.LENGTH_SHORT).show()
                }
        })
        td.start()

    }

    fun clearResult() {

        val td = Thread(Runnable {

            ResultEditor(this).saveResultState()

            for (i in 1..9) {
                for (j in 1..3) {

                    val s = i * 1000 + j * 100
                    if (Semester.courseCount(this, s) != 0) {
                        val helper = ResultDataBase(this, s)

                        Semester.decreaseCount(this, s, Semester.courseCount(this, s))
                        helper.onUpgrade(helper.writableDatabase, 1, 1)

                    }
                }
            }

            runOnUiThread {
                refreshFragment(0, 0)
                refreshFragment(1, 0)
            }
        })

        td.start()

    }



}
