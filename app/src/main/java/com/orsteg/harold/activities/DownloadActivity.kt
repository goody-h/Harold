package com.orsteg.harold.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import com.google.android.gms.ads.AdRequest
import com.orsteg.harold.R
import com.orsteg.harold.dialogs.DetailsDialog
import com.orsteg.harold.fragments.BaseDownloadFragment
import com.orsteg.harold.fragments.TemplateFragment
import com.orsteg.harold.fragments.TemplateLevelFragment
import com.orsteg.harold.utils.firebase.BannerAd
import com.orsteg.harold.utils.user.AppUser
import kotlinx.android.synthetic.main.activity_download.*

class DownloadActivity : AppCompatActivity(), DetailsDialog.DetailsDialogInterface {

    override var mUser: AppUser? = null
    var iAction = TemplateViewerActivity.ACTION_DOWNLOAD_APPLY
    private var mFragManager: FragManager? = null

    private var others: View? = null
    private var ins: TextView? = null
    private var dept: TextView? = null
    private var fab: View? = null

    override fun uiThread(run: () -> Unit) {
        runOnUiThread{ run() }
    }

    override fun networkTest(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }


    override fun showDetails(id: String) {
        (mFragManager?.getCurrentFragment() as TemplateFragment?)?.showDetails(id)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        supportActionBar?.title = "Online Templates"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adView.loadAd(AdRequest.Builder().build())

        BannerAd.setListener(adView)

        others = findViewById(R.id.others)
        ins = findViewById(R.id.institution)
        dept = findViewById(R.id.department)
        fab = findViewById(R.id.fab)


        mUser = AppUser.getPersistentUser(this)

        // parse incoming intent
        val intent = intent

        if (intent != null) {
            if (intent.action != null)
                when (intent.action) {
                    TemplateViewerActivity.ACTION_APPLY -> iAction = TemplateViewerActivity.ACTION_DOWNLOAD_APPLY
                    TemplateViewerActivity.ACTION_UPLOAD -> iAction = TemplateViewerActivity.ACTION_DOWNLOAD_UPLOAD
                }
            if (intent.hasExtra("USER")) mUser = AppUser.getSavedState(intent.getBundleExtra("USER"))
        }

        mFragManager = FragManager(supportFragmentManager)

        if (savedInstanceState != null) mFragManager?.restoreState(savedInstanceState)

        others?.setOnClickListener { setFragment(1, "", "") }

        fab?.setOnClickListener { view ->
            val intent1 = Intent(view.context, TemplateBrowserActivity::class.java)
            val bundle = Bundle()
            mUser?.saveUserState(bundle)
            intent1.putExtra("USER", bundle)
            intent1.action = TemplateViewerActivity.ACTION_UPLOAD
            startActivity(intent1)
        }

        mFragManager?.initfragment()

    }

    override fun onBackPressed() {
        if (mFragManager?.backPressed() == true)super.onBackPressed()
    }

    inner class FragManager(val mManager: FragmentManager){

        val tags = arrayOf("downloadActivity.Fragment.bottom", "downloadActivity.Fragment.institutions",
                "downloadActivity.Fragment.departments", "downloadActivity.Fragment.top")

        val levels = arrayOf(TemplateFragment.LEVEL_BOTTOM, TemplateLevelFragment.LEVEL_INSTITUTION,
                TemplateLevelFragment.LEVEL_DEPARTMENT, TemplateFragment.LEVEL_TOP)

        var mCurrent = -1
        var mInit = 0
        var initInst = ""
        var initDept = ""
        var min = 0


        init {
            if (mUser?.canUpload == true){
                initDept = mUser!!.department
                initInst = mUser!!.institution
            }
            else{
                min = 1
                mInit = 1
            }
        }

        fun getCurrentFragment(): BaseDownloadFragment?{
            return mManager.findFragmentByTag(tags[mCurrent]) as BaseDownloadFragment?
        }

        fun initfragment(){

            setFragment(mInit, initInst, initDept)
        }

        fun restoreState(inState: Bundle){
            mInit = inState.getInt("currLevel")
            initDept = inState.getString("initDept")
            initInst = inState.getString("initInst")
        }

        fun saveState(outState: Bundle){
            outState.putInt("currLevel", mCurrent)
            outState.putString("initDept", initDept)
            outState.putString("initInst", initInst)
        }

        fun backPressed(): Boolean{
            if (mCurrent > min){
                setFragment(mCurrent -1)
                return false
            }
            return true
        }

        fun setFragment(level: Int, inst: String = "", dept: String = ""){

            val frag: BaseDownloadFragment? = when(level){
                0, 3 -> {
                    (mManager.findFragmentByTag(tags[level]) as BaseDownloadFragment?)?:
                            TemplateFragment.newInstance(levels[level], inst, dept)
                }
                1, 2 -> {
                    (mManager.findFragmentByTag(tags[level]) as BaseDownloadFragment?)?:
                            TemplateLevelFragment.newInstance(levels[level], inst)
                }
                else -> {
                    null
                }
            }

            val pFrag = if (mCurrent != -1) mManager.findFragmentByTag(tags[mCurrent]) else null

            frag?.setUp(inst, dept)

            if (frag != null && (frag.isHidden || !frag.isAdded)){
                val trans = mManager.beginTransaction()

                if (!frag.isAdded){
                    if (pFrag != null){
                        trans.hide(pFrag)
                    }
                    trans.add(R.id.pager, frag, tags[level])
                } else {
                    if (pFrag != null){
                        trans.hide(pFrag)
                    }
                    trans.show(frag)
                }

                trans.commit()
            }

            mCurrent = level
        }

    }

    fun setFragment(level: Int, mInstitution: String, mDept: String) {

        mFragManager?.setFragment(level, mInstitution, mDept)
    }

    fun setViews(level: String, mInstitution: String, mDept: String) {
        when (level) {
            TemplateLevelFragment.LEVEL_DEPARTMENT -> {
                ins?.text = mInstitution
                dept?.text = mDept
                dept?.visibility = View.VISIBLE
                others?.visibility = View.GONE
                fab?.visibility = View.GONE
            }
            TemplateFragment.LEVEL_BOTTOM -> {
                ins?.text = mInstitution
                dept?.text = mDept
                dept?.visibility = View.VISIBLE
                others?.visibility = View.VISIBLE
                fab?.visibility = View.VISIBLE
            }
            TemplateFragment.LEVEL_TOP -> {
                ins?.text = mInstitution
                dept?.text = mDept
                dept?.visibility = View.VISIBLE
                others?.visibility = View.GONE
                fab?.visibility = View.GONE
            }
            TemplateLevelFragment.LEVEL_INSTITUTION -> {
                ins?.text = TemplateLevelFragment.INSTITUTION_ALL
                dept?.visibility = View.GONE
                others?.visibility = View.GONE
                fab?.visibility = View.GONE
            }
            else -> {
                ins?.text = TemplateLevelFragment.INSTITUTION_ALL
                dept?.visibility = View.GONE
                others?.visibility = View.GONE
                fab?.visibility = View.GONE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null)mFragManager?.saveState(outState)

    }

}
