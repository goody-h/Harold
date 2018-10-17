package com.orsteg.harold.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.orsteg.harold.R
import com.orsteg.harold.activities.*
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.result.Course
import com.orsteg.harold.utils.result.Semester

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] facjtory method to
 * create an instance of this fragment.
 */
class HomeFragment : BaseFragment() {

    override val mPrefType: String = Preferences.RESULT_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {

    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            cgpaCalc()
        }
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    override fun refresh(option: Int) {
        if(!isHidden) {
            cgpaCalc()
        }
    }


    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    private var reset: Boolean = false

    private var cgpaTxt: TextView? = null

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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cgpaTxt = view.findViewById(R.id.cgpa)

        view.findViewById<View>(R.id.result).setOnClickListener{
            mListener?.setFragment(1)
        }
        view.findViewById<View>(R.id.grading).setOnClickListener{
            reset = true
            mListener?.refreshFragment(1, 1)

            val i = Intent(context, GradingActivity::class.java)
            activity?.startActivity(i)
        }
        view.findViewById<View>(R.id.template).setOnClickListener{
            reset = true
            mListener?.refreshFragment(1, 1)

            val intent = Intent(context, TemplateBrowserActivity::class.java)
            val bundle = Bundle()
            mListener?.mUser?.saveUserState(bundle)
            intent.putExtra("USER", bundle)
            intent.action = TemplateViewerActivity.ACTION_APPLY
            startActivity(intent)
        }

        cgpaCalc()
    }

    override fun onStart() {
        super.onStart()

        if(reset || mPreferences.mPrefs.getBoolean("result.changed", false)) {
            cgpaCalc()
        }
        reset = false
    }

    private fun cgpaCalc() {

        var cgpa = 0f
        var tcu = 0f
        var tqp = 0f

        val levelCount = 9
        for (i in 1 until levelCount + 1) {
            val levelId = i * 1000
            val semId = 3
            for (j in 1 until semId + 1) {
                val sId = levelId + j * 100
                val courseCount = Semester.courseCount(context!!, sId)
                if (courseCount != 0) {
                    for (k in 1 until courseCount + 1) {
                        val courseId = sId + k
                        tcu += mPreferences.mPrefs.getFloat(Course.unitPref(courseId), 0f)
                        tqp += mPreferences.mPrefs.getFloat(Course.qpPref(courseId), 0f)
                    }
                }
            }
        }

        if (tcu != 0f) {
            cgpa = tqp / tcu
        }
        mPreferences.mEditor.putFloat("CGPA", cgpa).commit()

        cgpaTxt?.text = cgpa.string()

    }

    fun Float.string(): String {
        val d = this.toString().indexOf(".")

        if (d + 3 > this.toString().length) return this.toString() + "0"

        return this.toString().substring(0..d+2)
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
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor