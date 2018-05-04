package com.orsteg.harold.fragments


import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.orsteg.harold.R
import com.orsteg.harold.activities.LoginActivity
import com.orsteg.harold.activities.SignUpActivity
import com.orsteg.harold.utils.app.Preferences


/**
 * A simple [Fragment] subclass.
 * Use the [OfflineFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineFragment : BaseFragment() {

    override val mPrefType: String = Preferences.APP_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) mListener?.hideActionBtn()
    }

    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        return true
    }

    override fun refresh() {

    }


    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        if (!isHidden) mListener?.hideActionBtn()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val login = view.findViewById<View>(R.id.login) as Button
        val signUp = view.findViewById<View>(R.id.signup) as Button


        login.setOnClickListener {
            val intent = Intent(context!!, LoginActivity::class.java)

            startActivity(intent)
        }

        signUp.setOnClickListener {
            val intent = Intent(context, SignUpActivity::class.java)

            startActivity(intent)
        }

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
         * @return A new instance of fragment OfflineFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): OfflineFragment {
            val fragment = OfflineFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
