package com.orsteg.harold.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.View

import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.user.AppUser

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BaseFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
abstract class BaseFragment : Fragment() {


    protected var mListener: OnFragmentInteractionListener? = null
    protected val mPreferences: Preferences by lazy { Preferences(context!!, mPrefType) }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    abstract val mPrefType: String

    abstract override fun onSaveInstanceState(outState: Bundle)

    abstract override fun onHiddenChanged(hidden: Boolean)

    abstract fun onBackPressed(): Boolean

    abstract fun refresh(option: Int)

    abstract fun setSharedElements(transaction: FragmentTransaction)


    interface OnFragmentInteractionListener {

        var mUser: AppUser?

        var authState: Boolean

        fun resetGroup(which: Int)

        fun setFragment(id: Int)

        fun showSnackBar(message: String, action: String, btn: View, listener: (View) -> Unit)

        fun showWarning(message: String, action: () -> Unit)

        fun showLoader(message: String, cancelable: Boolean = true): Dialog?

        fun refreshFragment(group: Int, option: Int)

    }

}
