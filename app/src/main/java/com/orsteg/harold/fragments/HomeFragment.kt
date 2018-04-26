package com.orsteg.harold.fragments


import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast

import com.orsteg.harold.R
import com.orsteg.harold.utils.app.Preferences


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : BaseFragment() {

    override val mPrefType: String = Preferences.APP_PREFERENCES

    private var mNav: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mNav = mListener?.getTools(arrayOf(R.id.homeTool_inflater))?.get(0)?.inflate()

        if (!pendingTransaction) mNav?.visibility = View.GONE

        pendingTransaction = false

        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    override fun onSaveInstanceState(outState: Bundle?) {

    }

    override fun onShow(actionBtn: View) {
        if (mNav != null){
            mNav?.visibility = View.VISIBLE
        } else {
            pendingTransaction = true
        }

        actionBtn.visibility = View.GONE
    }

    override fun onHide(actionBtn: View) {

        mNav?.visibility = View.GONE
    }

    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        return true
    }

    override fun refresh() {

    }


    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

}// Required empty public constructor
