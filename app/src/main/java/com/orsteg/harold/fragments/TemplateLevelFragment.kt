package com.orsteg.harold.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.*

import com.orsteg.harold.R
import com.orsteg.harold.activities.DownloadActivity
import com.orsteg.harold.utils.firebase.ValueListener
import java.util.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [TemplateLevelFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TemplateLevelFragment : BaseDownloadFragment() {
    override fun setUp(mInstitution: String, mDept: String) {
        cInstitution = mInstitution
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            when (mLevel) {
                LEVEL_DEPARTMENT -> activity?.setViews(LEVEL_DEPARTMENT, mInstitution!!, "Departments")
                LEVEL_INSTITUTION -> activity?.setViews(LEVEL_INSTITUTION, INSTITUTION_ALL, "")
                else -> activity?.setViews(LEVEL_INSTITUTION, INSTITUTION_ALL, "")
            }

            if (mLevel == LEVEL_DEPARTMENT && cInstitution != mInstitution && cInstitution != "") {
                mInstitution = cInstitution

                initialise()
            }
        }
    }

    // TODO: Rename and change types of parameters
    private var mLevel: String? = null
    private var mInstitution: String? = null
    private var cInstitution = ""

    private var activity: DownloadActivity? = null

    private var list: ListView? = null
    private var empty: TextView? = null
    private var emptyText: View? = null
    private var adapter: ListAdapter? = null
    private var loading: View? = null
    private var nonetwork: View? = null

    private var levelRef: DatabaseReference? = null
    private var levelListener: ValueListener? = null

    private var listenerSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mLevel = arguments!!.getString(ARG_LEVEL)
            mInstitution = arguments!!.getString(ARG_INSTITUTION)
        }
        if (savedInstanceState != null){
            mInstitution = savedInstanceState.getString(ARG_INSTITUTION)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_template_level, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list = view.findViewById(R.id.list)
        empty = view.findViewById(R.id.mes)
        emptyText = view.findViewById(R.id.empty_text)
        loading = view.findViewById(R.id.empty)
        nonetwork = view.findViewById(R.id.nonetwork)

        activity = getActivity() as DownloadActivity

        initialise()

        if(!isHidden){
            when (mLevel) {
                LEVEL_DEPARTMENT -> activity?.setViews(LEVEL_DEPARTMENT, mInstitution!!, "Departments")
                LEVEL_INSTITUTION -> activity?.setViews(LEVEL_INSTITUTION, INSTITUTION_ALL, "")
                else -> activity?.setViews(LEVEL_INSTITUTION, INSTITUTION_ALL, "")
            }

        }
    }

    private fun initialise(){
        adapter = ListAdapter(context!!, ArrayList())

        list?.adapter = adapter

        if (networkTest())
            list?.emptyView = loading
        else
            list?.emptyView = nonetwork


        when (mLevel) {
            LEVEL_DEPARTMENT -> getAllDepartments()
            LEVEL_INSTITUTION -> getAllInstitutions()
            else -> getAllInstitutions()
        }

    }

    private fun getAllInstitutions() {
        empty?.text = "Loading institutions..."

        levelRef = FirebaseDatabase.getInstance().getReference("Institutions")

        list?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            val adapter = adapterView.adapter as ListAdapter
            activity?.setFragment(2, adapter.getItem(i).name, "Departments")
        }

        fetchData()

    }

    private fun getAllDepartments() {
        empty?.text = "Loading departments..."

        levelRef = FirebaseDatabase.getInstance().getReference("Eds").child(mInstitution!!.replace("\\s+".toRegex(), "_"))
        list?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ ->
            val adapter = adapterView.adapter as ListAdapter
            activity!!.setFragment(3, mInstitution!!, adapter.getItem(i).name)
        }

        fetchData()
    }

    private fun fetchData() {

        levelListener =  object : ValueListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {

                val snapI = dataSnapshot?.children
                val iterator = snapI?.iterator()

                val it = ArrayList<ListItem>()

                while (iterator?.hasNext() == true) {
                    val snap = iterator.next()
                    val nameRef = snap.child("name")
                    val size = snap.child("count")

                    if (nameRef.value != null && size.value != null) {

                        val item = ListItem(nameRef.getValue(String::class.java)!!, size.value as Long)
                        if (item.size != 0L) it.add(item)

                    }
                }

                adapter?.items = it


                if (it.size == 0) {
                    nonetwork?.visibility = View.GONE
                    loading?.visibility = View.GONE
                    list?.emptyView = emptyText
                }

                adapter?.notifyDataSetChanged()

            }
        }

        levelRef?.addValueEventListener(levelListener)
        listenerSet = true
    }

    override fun onStart() {
        super.onStart()
        if (!listenerSet)
            levelRef?.addValueEventListener(levelListener)
    }

    override fun onStop() {
        super.onStop()
        levelRef?.removeEventListener(levelListener)
        listenerSet = false
    }

    private inner class ListItem internal constructor(internal var name: String, internal var size: Long)
    private inner class ListAdapter internal constructor(private val context: Context, internal var items: ArrayList<ListItem>) : BaseAdapter() {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(i: Int): ListItem {
            return items[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            var view = view
            if (view == null) view = LayoutInflater.from(context).inflate(R.layout.frag_level_item, viewGroup, false)
            val n = view!!.findViewById<TextView>(R.id.name)
            val s = view.findViewById<TextView>(R.id.size)
            n.text = getItem(i).name
            s.text = getItem(i).size.toString()
            return view
        }
    }

    private fun networkTest(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(ARG_INSTITUTION, mInstitution)

    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_LEVEL = "level"
        private val ARG_INSTITUTION = "institution"

        val LEVEL_INSTITUTION = "institution"
        val LEVEL_DEPARTMENT = "department"
        val INSTITUTION_ALL = "All institutions"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param level Parameter 1.
         * @param institution Parameter 2.
         * @return A new instance of fragment TemplateLevelFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(level: String, institution: String): TemplateLevelFragment {
            val fragment = TemplateLevelFragment()
            val args = Bundle()
            args.putString(ARG_LEVEL, level)
            args.putString(ARG_INSTITUTION, institution)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
