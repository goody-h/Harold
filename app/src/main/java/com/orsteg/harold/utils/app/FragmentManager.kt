package com.orsteg.harold.utils.app

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.transition.*
import android.support.v4.app.FragmentManager
import android.transition.Transition
import android.view.View
import com.orsteg.harold.R
import com.orsteg.harold.fragments.*
import com.orsteg.harold.utils.user.AppUser

/**
 * Created by goodhope on 4/23/18.
 */
class FragmentManager(context: Context, private val parent: View, private var navView: NavigationView,
                      private val mFragmentManager: FragmentManager) {

    private var mCurrentGroup: Int = -1
    private var mPreviousGroup: Int = -1
    private var mListener: OnFragmentManagerListener? = null

    private var initPosition = 0

    private var mGroups = arrayOf(
            HomeGroup(mFragmentManager),
            ResultGroup(mFragmentManager)
    )

    private var mHistory = ArrayList<Int>()

    init {
        if (context is OnFragmentManagerListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentManagerListener")
        }
    }

    fun initFragment() {
        setFragment(initPosition)
    }

    fun resetGroup(which: Int) {
        if (mCurrentGroup == which) {
            setFragment(mCurrentGroup, reset = true)
        } else {
            mGroups[which].pendingReset = true
        }
    }

    fun refreshFragment(group: Int, option: Int) {
        mGroups[group].getCurrentFragment()?.refresh(option)
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
                pFrag?.setSharedElements(transaction)

                if (new) {

                    val tag = mGroups[mCurrentGroup].fragTag

                    frag.sharedElementEnterTransition = DetailsTransition()
                    frag.enterTransition = Fade()
                    frag.exitTransition = Fade()

                    if (pFrag != null) {

                        transaction.replace(parent.id, frag, tag)
                    } else {
                        transaction.add(parent.id, frag, tag)
                    }

                } else {

                    if (pFrag != null) {

                        transaction.replace(parent.id, frag)
                    } else {
                        transaction.add(parent.id, frag)
                    }
                }
                transaction.disallowAddToBackStack()
                transaction.commit()
            }

            if (!reset) {
                mListener?.setToolbarTitle(mGroups[mCurrentGroup].title)
                navView.menu.findItem(arrayOf(R.id.nav_dashboard, R.id.nav_result)[mCurrentGroup]).isChecked = true

                mPreviousGroup = mCurrentGroup
                mHistory.remove(position)
                mHistory.add(position)
            }
        }
    }

    class DetailsTransition: TransitionSet() {
        init {
            ordering = ORDERING_TOGETHER
            addTransition(ChangeBounds()).addTransition(ChangeTransform()).addTransition(ChangeImageTransform())
        }
    }

    fun onBackPresses(): Boolean {

        return if (mGroups[mCurrentGroup].onBackPressed()) {
            val n = if (mHistory.size > 1) mHistory[mHistory.size - 2] else -1
            mHistory.removeAt(mHistory.size - 1)

            return if (n != -1) {
                setFragment(n)
                false
            } else true
        } else false

    }

    fun onSaveInstanceState(outState: Bundle) {

        outState.putInt("harold.fragment.current", mCurrentGroup)
        outState.putIntegerArrayList("harold.fragment.history", mHistory)
        for (i in 0 until mGroups.size) {
            mGroups[i].onSaveInstanceState(outState)
        }
    }

    fun restoreState(inState: Bundle) {
        initPosition = inState.getInt("harold.fragment.current", 0)
        mHistory = inState.getIntegerArrayList("harold.fragment.history")

        for (i in 0 until mGroups.size) {
            mGroups[i].onRestoreInstance(inState)
        }
    }

    interface OnFragmentManagerListener {

        var mUser: AppUser?

        var authState: Boolean

        var mInstanceState: Bundle?

        fun setToolbarTitle(title: String)

    }

    class ResultGroup(mFragmentManager: FragmentManager) : FragmentGroup(mFragmentManager) {

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

    class HomeGroup(mFragmentManager: FragmentManager) : FragmentGroup(mFragmentManager) {

        override val title: String = "Harold"

        init {
            fragTag = "com.harold.fragment.home"
        }

        override fun getFragment(reset: Boolean): BaseFragment {

            var fragment = getCurrentFragment()
            if (fragment == null) {
                fragment = HomeFragment.newInstance("", "")
                newFrag = true
            } else {
                newFrag = false
            }

            return fragment
        }
    }
}

abstract class FragmentGroup(private val mFragmentManager: FragmentManager) {

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

    fun onBackPressed(): Boolean {
        return getCurrentFragment()?.onBackPressed()?: true
    }

    fun onRestoreInstance(inState: Bundle){
        fragTag = inState.getString("harold.fragment.$title.current", "")
    }

}
