package com.orsteg.harold.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.orsteg.harold.R


/**
 * A simple [Fragment] subclass.
 */
abstract class BaseDownloadFragment : Fragment() {

    abstract fun setUp(mInstitution: String, mDept: String)

    abstract override fun onHiddenChanged(hidden: Boolean)

}// Required empty public constructor
