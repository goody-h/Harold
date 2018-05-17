package com.orsteg.harold.activities

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.ImageView
import com.cocosw.bottomsheet.BottomSheet
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.orsteg.harold.R
import com.orsteg.harold.dialogs.UpdateDialog
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.fragments.BaseFragment
import com.orsteg.harold.utils.app.FragmentManager
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.app.TimeConstants
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.firebase.ValueListener
import com.orsteg.harold.utils.user.AppUser
import com.orsteg.harold.utils.user.User
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), BaseFragment.OnFragmentInteractionListener, FragmentManager.OnFragmentManagerListener {

    override var mUser: AppUser? = null
    override var authState: Boolean = false
    override var mInstanceState: Bundle? = null

    private var mFragmentManager: FragmentManager? = null

    private var mUserRef: DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var mUserListener: ValueListener? = null

    private var mTabIconsSelected = arrayOf(
            R.drawable.ic_school_black_24dp,
            R.drawable.ic_date_range_black_24dp,
            R.drawable.ic_person_black_24dp
    )


    private var sheet: BottomSheet? = null

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

        mFragmentManager = FragmentManager(this, container, bottomTab, supportFragmentManager, actionBtn)

        if (savedInstanceState != null){
            mUser = AppUser.getSavedState(savedInstanceState)
            authState = savedInstanceState.getBoolean("user.hasState", false)

            mInstanceState = savedInstanceState
            mFragmentManager?.restoreState(savedInstanceState)
        }

        setSupportActionBar(toolbar)
        initTabs()

        mAuth = FirebaseAuth.getInstance()

        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser

            if (user != null) {
                // User is signed in

                // Set Activity Parameters
                if (!authState) {
                    authState = true
                    resetGroup(2)
                }
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
                    resetGroup(2)
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

                            resetGroup(2)
                        } else if (mUser?.updateUser(nUser) == true){

                            mUser = nUser
                            mUser?.persistUser(this@MainActivity)

                            refreshFragment(2)

                        }
                    } else {

                        AppUser.signOut(this@MainActivity)

                        authState = false
                        mUser = null
                        resetGroup(2)

                    }
                }
            }

            mUserRef?.addValueEventListener(mUserListener)
        }
    }

    private fun initTabs(){
            for (i in 0..2) {
                val tab = bottomTab.getTabAt(i)
                if (tab != null)
                    tab.customView = getTabView(i)
            }
    }

    private fun getTabView(position: Int): View{
        val view = LayoutInflater.from(this).inflate(R.layout.tab_item_bottom, null)
        val icon: ImageView = view.findViewById(R.id.tab_icon)
        icon.setImageDrawable(setDrawableSelector(this, mTabIconsSelected[position], mTabIconsSelected[position]))
        return view
    }

    fun refreshFragment(group: Int) {
        mFragmentManager?.refreshFragment(group)
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mUser?.saveUserState(outState)
            outState.putBoolean("user.hasState", authState)
            mFragmentManager?.onSaveInstanceState(outState)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {

        if (mFragmentManager?.onBackPresses(actionBtn!!) == true)
        super.onBackPressed()

    }

    override fun onCreateDialog(id: Int): Dialog? {

        return sheet
    }

    override fun resetGroup(which: Int) {
        mFragmentManager?.resetGroup(which)
    }

    override fun hideActionBtn() {
        actionBtn.visibility = View.GONE
    }

    override fun shoWActionBtn(listener: View.OnClickListener?) {
        actionBtn.visibility = View.VISIBLE
        actionBtn.setOnClickListener(listener)
    }


    override fun setActionBtn(resId: Int, listener: View.OnClickListener?) {
        actionBtn.setImageResource(resId)
        actionBtn.setOnClickListener(listener)
    }

    override fun hideTabs() {
        bottom.visibility = View.GONE
        val frameParam = container.layoutParams as ViewGroup.MarginLayoutParams

        container.tag = frameParam.bottomMargin

        frameParam.bottomMargin = 0
    }

    override fun showTabs() {
        bottom.visibility = View.VISIBLE
        val frameParam = container.layoutParams as ViewGroup.MarginLayoutParams

        frameParam.bottomMargin = container.tag as Int
    }

    override fun showBottomSheet(menuId: Int, listener: (DialogInterface, Int) -> Unit) {
        sheet = BottomSheet.Builder(this).sheet(menuId).listener(listener).grid().build()
        showDialog(menuId)
    }

    override fun showSnackBar(message: String, action: String, listener: (View) -> Unit) {
        Snackbar.make(actionBtn!!, message, Snackbar.LENGTH_LONG)
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

    override fun getTools(stubIds: Array<Int>): ArrayList<ViewStub> {

        val views = ArrayList<ViewStub>()
        stubIds.mapTo(views) { appBar.findViewById(it) }

        return views
    }

    companion object {

        fun setDrawableSelector(context: Context, normal: Int, selected: Int): Drawable{

            val stateNormal = ContextCompat.getDrawable(context, normal)

            val statePressed = ContextCompat.getDrawable(context, selected)


            val stateNormalBitmap = Bitmap.createBitmap(
                    stateNormal!!.intrinsicWidth,
                    stateNormal.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas1 = Canvas(stateNormalBitmap)
            stateNormal.setBounds(0, 0, canvas1.width, canvas1.height)
            stateNormal.draw(canvas1)


            // Setting alpha directly just didn't work, so we draw a new bitmap!
            val disabledBitmap = Bitmap.createBitmap(
                    stateNormal.intrinsicWidth,
                    stateNormal.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(disabledBitmap)

            val paint = Paint()
            paint.alpha = 126
            canvas.drawBitmap(stateNormalBitmap, 0f, 0f, paint)

            val stateNormalDrawable = BitmapDrawable(context.resources, disabledBitmap)


            val drawable = StateListDrawable()

            drawable.addState(intArrayOf(android.R.attr.state_selected),
                    statePressed)
            drawable.addState(intArrayOf(android.R.attr.state_enabled),
                    stateNormalDrawable)

            return drawable
        }

    }


}
