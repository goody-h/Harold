package com.orsteg.harold.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.Toast

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

    abstract fun onBackPressed(actionBtn: FloatingActionButton): Boolean

    abstract fun refresh()


    interface OnFragmentInteractionListener {

        var mUser: AppUser?

        var authState: Boolean

        fun resetGroup(which: Int)

        fun hideActionBtn()

        fun shoWActionBtn(listener: View.OnClickListener?)

        fun setActionBtn(resId: Int, listener: View.OnClickListener?)

        fun hideTabs()

        fun showTabs()

        fun showBottomSheet(menuId: Int, listener: (DialogInterface, Int) -> Unit)

        fun showSnackBar(message: String, action: String, listener: (View) -> Unit)

        fun showWarning(message: String, action: () -> Unit)

        fun showLoader(message: String, cancelable: Boolean = true): Dialog?

        fun getTools(stubIds: Array<Int>): ArrayList<ViewStub>

    }

}
