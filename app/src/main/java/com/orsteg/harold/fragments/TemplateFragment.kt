package com.orsteg.harold.fragments

import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import com.google.firebase.database.*

import com.orsteg.harold.R
import com.orsteg.harold.activities.DownloadActivity
import com.orsteg.harold.activities.TemplateViewerActivity
import com.orsteg.harold.dialogs.DetailsDialog
import com.orsteg.harold.utils.firebase.ChildListener
import com.orsteg.harold.utils.firebase.References
import com.orsteg.harold.utils.firebase.ValueListener
import com.orsteg.harold.utils.result.Template
import com.orsteg.harold.utils.result.TemplateManager
import com.orsteg.harold.utils.user.AppUser
import java.util.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [TemplateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TemplateFragment : BaseDownloadFragment() {
    override fun setUp(mInstitution: String, mDept: String) {
        cInstitution = mInstitution
        cDepartment = mDept
    }

    override fun onHiddenChanged(hidden: Boolean) {

        if (!hidden) {
            when (mLevel) {
                LEVEL_TOP -> activity?.setViews(LEVEL_TOP, mInstitution!!, mDepartment!!)
                LEVEL_BOTTOM -> activity?.setViews(LEVEL_BOTTOM, mInstitution!!, mDepartment!!)
                else -> activity?.setViews(LEVEL_BOTTOM, mInstitution!!, mDepartment!!)
            }
            if (mLevel == LEVEL_TOP && (cDepartment != mDepartment || cInstitution != mInstitution)) {
                mDepartment = cDepartment
                mInstitution = cInstitution

                initiate()
            }
        }
    }

    // TODO: Rename and change types of parameters
    private var mLevel: String? = null
    private var mInstitution: String? = null
    private var cInstitution = ""
    private var mDepartment: String? = null
    private var cDepartment = ""
    private var activity: DownloadActivity? = null
    private var fragment: Fragment? = null

    var mTemplateDatabase: Query? = null
    var mTemplatesListener: ChildListener? = null

    private var cats: ExpandableListView? = null
    private var adapter: CategoryAdapter? = null
    private var eView: View? = null
    private var emptyText: View? = null
    private var nonetwork: View? = null

    private var tempCount: Long? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mLevel = arguments!!.getString(ARG_LEVEL)
            mInstitution = arguments!!.getString(ARG_INSTITUTION)
            mDepartment = arguments!!.getString(ARG_DEPARTMENT)
        }

        if (savedInstanceState != null){
            mInstitution = savedInstanceState.getString(ARG_INSTITUTION)
            mDepartment = savedInstanceState.getString(ARG_DEPARTMENT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(ARG_INSTITUTION, mInstitution)
        outState.putString(ARG_DEPARTMENT, mDepartment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_template, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        activity = getActivity() as DownloadActivity
        fragment = this

        cats = view.findViewById(R.id.list)
        val empty = view.findViewById<TextView>(R.id.mes)
        eView = view.findViewById(R.id.empty)
        nonetwork = view.findViewById(R.id.nonetwork)
        emptyText = view.findViewById(R.id.empty_text)

        empty.text = "Loading templates..."

        initiate()

        if (!isHidden){
            when (mLevel) {
                LEVEL_TOP -> activity?.setViews(LEVEL_TOP, mInstitution!!, mDepartment!!)
                LEVEL_BOTTOM -> activity?.setViews(LEVEL_BOTTOM, mInstitution!!, mDepartment!!)
                else -> activity?.setViews(LEVEL_BOTTOM, mInstitution!!, mDepartment!!)
            }
        }
    }

    private fun initiate(){
        adapter = CategoryAdapter(context!!)

        cats?.setAdapter(adapter)
        adapter?.notifyDataSetChanged(0)

        cats?.setOnChildClickListener { expandableListView, _, i, i1, _ ->
            val t: TemplateManager = if (i == 0) {
                (expandableListView.expandableListAdapter as CategoryAdapter).deptTemplate[i1]
            } else {
                (expandableListView.expandableListAdapter as CategoryAdapter).myTemplate[i1]
            }

            val d = DetailsDialog(context!!, t)
            d.show()

            true
        }

        getDeptTemplates()
    }

    fun showDetails(id: String) {
        val i = (cats!!.expandableListAdapter as CategoryAdapter).deptTemplate.hasKey(id)
        if (i != -1) {
            val t = (cats!!.expandableListAdapter as CategoryAdapter).deptTemplate[i]

            val d = DetailsDialog(context!!, t)
            d.show()
        }
    }


    private fun getDeptTemplates() {
        mTemplateDatabase = References.TEMPLATES_DATABASE
                .orderByChild("edKey").equalTo(AppUser.getPublicEdKey(mInstitution!!, mDepartment!!))

        mTemplatesListener = object : ChildListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {
                if (dataSnapshot != null) {
                    onAdded(0, dataSnapshot)
                    val owner = dataSnapshot.child("ownerId").getValue(String::class.java)
                    if (mLevel == LEVEL_BOTTOM && owner != null && owner == activity?.mUser!!.userId) onAdded(1, dataSnapshot)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, s: String?) {
                if (dataSnapshot != null) {
                    onChanged(0, dataSnapshot)
                    val owner = dataSnapshot.child("ownerId").getValue(String::class.java)
                    if (mLevel == LEVEL_BOTTOM && owner != null && owner == activity?.mUser!!.userId) onChanged(1, dataSnapshot)
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot?) {
                if (dataSnapshot != null) {
                    onRemoved(0, dataSnapshot.key)
                    val owner = dataSnapshot.child("ownerId").getValue(String::class.java)
                    if (mLevel == LEVEL_BOTTOM && owner != null && owner == activity?.mUser!!.userId) onRemoved(1, dataSnapshot.key)
                }
            }
        }

        mTemplateDatabase?.addChildEventListener(mTemplatesListener)


        FirebaseDatabase.getInstance().getReference("Eds")
                .child(mInstitution?.replace("\\s+".toRegex(), "_"))
                .child(mDepartment?.replace("\\s+".toRegex(), "_")).child("count")
                .addListenerForSingleValueEvent(object : ValueListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot?) {
                        tempCount = if (dataSnapshot?.value != null) {
                            dataSnapshot.value as Long
                        } else 0L

                        adapter?.notifyDataSetChanged(0)
                    }
                })
    }

    private fun onAdded(type: Int, dataSnapshot: DataSnapshot) {
        val template = dataSnapshot.getValue(Template::class.java)
        val id = dataSnapshot.key

        if (template != null) {

            val temp = TemplateManager(template, id, activity!!.iAction)

            when (type) {
                0 -> if (adapter!!.deptTemplate.hasKey(id) == -1) {
                    adapter!!.deptTemplate.add(temp)
                }
                1 -> if (adapter!!.myTemplate.hasKey(id) == -1) {
                    adapter!!.myTemplate.add(temp)
                }
            }
            adapter?.notifyDataSetChanged(type)
        }
    }

    private fun onChanged(type: Int, dataSnapshot: DataSnapshot) {
        val template = dataSnapshot.getValue(Template::class.java)
        val id = dataSnapshot.key

        if (template != null) {

            val temp = TemplateManager(template, id, activity!!.iAction)
            val pos: Int

            when (type) {
                0 -> {
                    pos = adapter!!.deptTemplate.hasKey(id)
                    if (pos != -1) {
                        adapter!!.deptTemplate.removeAt(pos)
                        adapter!!.deptTemplate.add(pos, temp)
                    }
                }
                1 -> {
                    pos = adapter!!.myTemplate.hasKey(id)
                    if (pos != -1) {
                        adapter!!.myTemplate.removeAt(pos)
                        adapter!!.myTemplate.add(pos, temp)
                    }
                }
            }
            adapter?.notifyDataSetChanged(type)
        }
    }

    private fun onRemoved(type: Int, id: String) {
        val pos: Int
        when (type) {
            0 -> {
                pos = adapter!!.deptTemplate.hasKey(id)
                if (pos != -1) adapter!!.deptTemplate.removeAt(pos)
            }
            1 -> {
                pos = adapter!!.myTemplate.hasKey(id)
                if (pos != -1) adapter!!.myTemplate.removeAt(pos)
            }
        }
        adapter!!.notifyDataSetChanged(type)
    }

    private inner class CourseArray internal constructor() : ArrayList<TemplateManager>() {
        internal fun hasKey(key: String): Int {
            return (0 until size).firstOrNull { get(it).id == key }
                    ?: -1
        }
    }

    private inner class CategoryAdapter internal constructor(private val context: Context) : ExpandableListAdapter {

        internal var deptTemplate: CourseArray
        internal var myTemplate: CourseArray

        init {
            deptTemplate = CourseArray()
            myTemplate = CourseArray()

        }

        internal fun notifyDataSetChanged(Group: Int) {

            eView?.visibility = View.GONE
            nonetwork?.visibility = View.GONE
            emptyText?.visibility = View.GONE

            if (deptTemplate.size == 0) {

                if (activity?.networkTest() == true) {
                    if (tempCount == null)
                        eView?.visibility = View.VISIBLE
                    else
                        emptyText?.visibility = View.VISIBLE
                } else
                    nonetwork?.visibility = View.VISIBLE
            }
            cats?.collapseGroup(Group)
            cats?.expandGroup(Group)
        }


        override fun registerDataSetObserver(dataSetObserver: DataSetObserver?) {

        }


        override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver?) {

        }

        override fun getGroupCount(): Int {
            return if (mLevel == LEVEL_TOP) 1 else 2
        }

        override fun getChildrenCount(i: Int): Int {
            return if (i == 0)
                deptTemplate.size
            else
                myTemplate.size
        }

        override fun getGroup(i: Int): ArrayList<TemplateManager> {
            return if (i == 0)
                deptTemplate
            else
                myTemplate
        }

        override fun getChild(i: Int, i1: Int): TemplateManager {
            return if (i == 0)
                deptTemplate[i1]
            else
                myTemplate[i1]
        }

        override fun getGroupId(i: Int): Long {
            return (i * 100).toLong()
        }

        override fun getChildId(i: Int, i1: Int): Long {
            return (i * 100 + i1).toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(i: Int, b: Boolean, view: View?, viewGroup: ViewGroup?): View {
            val inflater = LayoutInflater.from(context)
            val view1 = inflater.inflate(R.layout.download_category_head, viewGroup, false)
            if (i == 0) {
                return View(getContext())
            } else {
                if (myTemplate.size == 0) return View(getContext())
                (view1.findViewById<View>(R.id.textView) as TextView).text = "My Uploads"
            }
            return view1
        }

        override fun getChildView(i: Int, i1: Int, b: Boolean, view: View?, viewGroup: ViewGroup?): View {
            var view = view

            if (view == null) view = LayoutInflater.from(context).inflate(R.layout.temp_download_item, viewGroup, false)

            (view!!.findViewById<View>(R.id.name) as TextView).text = getChild(i, i1).name
            (view.findViewById<View>(R.id.user) as TextView).text = "By: " + getChild(i, i1).ownerName

            view.findViewById<View>(R.id.image).setOnClickListener {
                val `in` = Intent(context, TemplateViewerActivity::class.java)
                `in`.action = activity!!.iAction
                `in`.putExtra(TemplateViewerActivity.EXTRA_URL, getChild(i, i1).fileUrl)
                val b = Bundle()
                activity?.mUser?.saveUserState(b)
                getChild(i, i1).saveState(b)
                `in`.putExtra(TemplateViewerActivity.EXTRA_TEMPLATE, b)
                `in`.putExtra("USER", b)

                startActivity(`in`)
            }
            return view
        }


        override fun isChildSelectable(i: Int, i1: Int): Boolean {
            return true
        }

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun isEmpty(): Boolean {
            return false
        }

        override fun onGroupExpanded(i: Int) {

        }

        override fun onGroupCollapsed(i: Int) {
            if (i == 0) cats!!.expandGroup(i)
        }

        override fun getCombinedChildId(l: Long, l1: Long): Long {
            return 0
        }

        override fun getCombinedGroupId(l: Long): Long {
            return 0
        }
    }


    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_LEVEL = "level"
        private val ARG_INSTITUTION = "institution"
        private val ARG_DEPARTMENT = "department"

        val LEVEL_BOTTOM = "bottom"
        val LEVEL_TOP = "top"

        private val TAG = "MYTAG"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param level Parameter 1.
         * @param institution Parameter 2.
         * @param department Parameter 2.
         * @return A new instance of fragment TemplateFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(level: String, institution: String, department: String): TemplateFragment {
            val fragment = TemplateFragment()
            val args = Bundle()
            args.putString(ARG_LEVEL, level)
            args.putString(ARG_INSTITUTION, institution)
            args.putString(ARG_DEPARTMENT, department)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
