package com.orsteg.harold.utils.app

import android.content.Context
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentManager
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.orsteg.harold.fragments.*
import com.orsteg.harold.utils.event.NotificationScheduler
import com.orsteg.harold.utils.user.AppUser

/**
 * Created by goodhope on 4/23/18.
 */
class FragmentManager(context: Context, private val parent: View, private var tabLayout: TabLayout,
                      private val mFragmentManager: FragmentManager, private val actionBtn: FloatingActionButton) {

    private var mCurrentGroup: Int = -1
    private var mPreviousGroup: Int = -1
    private var mListener: OnFragmentManagerListener? = null

    private var initPosition = 0

    private var mGroups = arrayOf(HomeGroup(mFragmentManager),
            ResultGroup(mFragmentManager),
            EventGroup(context, mFragmentManager),
            ProfileGroup(mFragmentManager))

    private var mHistory = ArrayList<Int>()

    init {
        if (context is OnFragmentManagerListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentManagerListener")
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                setFragment(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
    }

    fun initFragment(){

        tabLayout.getTabAt(initPosition)?.select()
        if (initPosition == 0)setFragment(initPosition)

    }

    fun resetGroup(which: Int){
        if(mCurrentGroup == which){
            setFragment(mCurrentGroup, reset = true)
        } else {
            mGroups[which].pendingReset = true
        }
    }

    fun refreshFragment(group: Int){
        mGroups[group].getCurrentFragment()?.refresh()
    }

    fun setFragment(position: Int, reset: Boolean = false) {

        if (reset || mCurrentGroup != position) {

            val pFrag = if (mPreviousGroup != -1) {
                mGroups[mPreviousGroup].getCurrentFragment()
            } else {
                null
            }

            mCurrentGroup = position


            val frag = mGroups[mCurrentGroup].getFragment(reset)

            val new = mGroups[mCurrentGroup].newFrag

            if (frag.isHidden || new) {

                val transaction = mFragmentManager.beginTransaction()

                if (new) {

                    if (pFrag != null) {
                        transaction.hide(pFrag)
                    }

                    val tag = mGroups[mCurrentGroup].fragTag

                    transaction.add(parent.id, frag, tag)
                } else {
                    if (pFrag != null) {

                        transaction.hide(pFrag)
                    }

                    transaction.show(frag)
                }

                transaction.commit()
            }

            if (!reset) {
                mListener?.setToolbarTitle(mGroups[mCurrentGroup].title)

                mPreviousGroup = mCurrentGroup
                mHistory.remove(position)
                mHistory.add(position)
            }
        }
    }

    fun onBackPresses(actionBtn: FloatingActionButton): Boolean{

        val n = if (mHistory.size > 1) mHistory[mHistory.size - 2] else -1
        mHistory.removeAt(mHistory.size-1)

        return if (n != -1){
            if (mGroups[mCurrentGroup].onBackPressed(actionBtn)){
                tabLayout.getTabAt(n)?.select()
            }
            false
        }
            else true
    }

    fun onSaveInstanceState(outState: Bundle){

        outState.putInt("harold.fragment.current", mCurrentGroup)
        outState.putIntegerArrayList("harold.fragment.history", mHistory)
        for (i in 0 until mGroups.size){
            mGroups[i].onSaveInstanceState(outState)
        }
    }

    fun restoreState(inState: Bundle){
        initPosition = inState.getInt("harold.fragment.current",0)
        mHistory = inState.getIntegerArrayList("harold.fragment.history")

        for (i in 0 until mGroups.size){
            mGroups[i].onRestoreInstance(inState)
        }
    }

    companion object {
        val TAG = "THISTAG"
    }

    interface OnFragmentManagerListener {

        var mUser: AppUser?

        var authState: Boolean

        var mInstanceState: Bundle?

        fun setToolbarTitle(title: String)

    }

    class HomeGroup(mFragmentManager: FragmentManager): FragmentGroup(mFragmentManager) {

        override val title: String = "Home"

        init {
            fragTag = "com.harold.fragment.home"
        }

        override fun getFragment(reset: Boolean): BaseFragment {

            var fragment = getCurrentFragment()
            if (fragment == null) {
                fragment = HomeFragment.newInstance()
                newFrag = true
            } else {
                newFrag = false
            }

            return fragment
        }
    }

    class ResultGroup(mFragmentManager: FragmentManager): FragmentGroup(mFragmentManager) {

        override val title: String = "Result"

        init {
            fragTag = "com.harold.fragment.result"
        }

        override fun getFragment(reset: Boolean): BaseFragment {

            var fragment = getCurrentFragment()
            if (fragment == null) {
                fragment = ResultFragment.newInstance("", "")
                newFrag = true
            } else {
                newFrag = false
            }

            return fragment
        }
    }

    class EventGroup(context: Context, mFragmentManager: FragmentManager): FragmentGroup(mFragmentManager) {

        override val title: String = "Events"

        private val tags = arrayOf("com.harold.fragment.event", "com.harold.fragment.event.setup")

        private var pref = Preferences(context, Preferences.EVENT_PREFERENCES)

        init {
            val setup = pref.mPrefs.getBoolean("harold.event.setup", false)

            if (setup){
                Thread { NotificationScheduler.setAllReminders(context)}.start()
            }
        }

        override fun getFragment(reset: Boolean): BaseFragment {

            val setup = pref.mPrefs.getBoolean("harold.event.setup", false)

            val fragment = if (getCurrentFragment() != null && !reset && !pendingReset)
                getCurrentFragment()
            else if (setup){
                fragTag = tags[0]
                var mFrag = mFragmentManager.findFragmentByTag(fragTag)
                if (mFrag == null) {
                    mFrag = EventFragment.newInstance(0, "")
                    newFrag = true
                } else {
                    newFrag = false
                }
                mFrag
            } else {
                fragTag = tags[1]
                var mFrag = mFragmentManager.findFragmentByTag(fragTag)
                if (mFrag == null) {
                    mFrag = EventSetupFragment.newInstance("", "")
                    newFrag = true
                } else {
                    newFrag = false
                }
                mFrag
            }

            return fragment as BaseFragment
        }

    }

    inner class ProfileGroup(mFragmentManager: FragmentManager): FragmentGroup(mFragmentManager) {

        override val title: String = "Profile"

        private val tags = arrayOf("com.harold.fragment.user.offline", "com.harold.fragment.user.null",
                "com.harold.fragment.user.profile")

        override fun getFragment(reset: Boolean): BaseFragment {

            val fragment = if (getCurrentFragment() != null && !reset && !pendingReset)
                getCurrentFragment()
            else if (mListener?.authState == true){
                if (mListener?.mUser != null) {
                    fragTag = tags[2]
                    var mFrag = mFragmentManager.findFragmentByTag(fragTag)
                    if (mFrag == null) {
                        mFrag = ProfileFragment.newInstance("", "")
                        newFrag = true
                    } else {
                        newFrag = false
                    }
                    mFrag
                } else {
                    fragTag = tags[1]
                    var mFrag = mFragmentManager.findFragmentByTag(fragTag)
                    if (mFrag == null) {
                        mFrag = GetUserFragment.newInstance("", "")
                        newFrag = true
                    } else {
                        newFrag = false
                    }
                    mFrag
                }
            } else {
                fragTag = tags[0]
                var mFrag = mFragmentManager.findFragmentByTag(fragTag)
                if (mFrag == null) {
                    mFrag = OfflineFragment.newInstance("", "")
                    newFrag = true
                } else {
                    newFrag = false
                }
                mFrag
            }


            return fragment as BaseFragment
        }

    }

}

abstract class FragmentGroup(protected val mFragmentManager: FragmentManager) {

    abstract val title: String

    var pendingReset = false

    var fragTag: String = ""

    var newFrag: Boolean = true


    abstract fun getFragment(reset: Boolean): BaseFragment

    fun getCurrentFragment(): BaseFragment?{

        val frag = mFragmentManager.findFragmentByTag(fragTag) as BaseFragment?

        newFrag = frag == null

        return frag
    }

    fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString("harold.fragment.$title.current", fragTag)
    }

    fun onBackPressed(actionBtn: FloatingActionButton): Boolean {
        return getCurrentFragment()?.onBackPressed(actionBtn)?: true
    }

    fun onRestoreInstance(inState: Bundle){
        fragTag = inState.getString("harold.fragment.$title.current", "")
    }

}
