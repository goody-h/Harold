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
 * Use the [EventFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EventFragment : BaseFragment() {
    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden){
            eventSet?.visibility = View.GONE
        } else {
            eventSet?.visibility = View.VISIBLE

            mListener?.shoWActionBtn(mAction)
        }
    }

    override val mPrefType: String = Preferences.EVENT_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        return true
    }

    override fun refresh() {

    }


    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    
    private var eventSet: View? = null
    private var mAction: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        eventSet = mListener?.getTools(arrayOf(R.id.homeTool_inflater))?.get(0)?.inflate()

        if (isHidden) {
            eventSet?.visibility = View.GONE
        } else {
            mListener?.shoWActionBtn(mAction)
        }

        return inflater.inflate(R.layout.fragment_event, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EventFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): EventFragment {
            val fragment = EventFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
