package com.orsteg.harold.fragments


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

import com.orsteg.harold.R
import com.orsteg.harold.activities.*
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.user.AppUser
import kotlinx.android.synthetic.main.fragment_profile.*


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : BaseFragment() {

    override val mPrefType: String = Preferences.APP_PREFERENCES

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            mListener?.hideActionBtn()

            userName.text = mListener?.mUser?.userName
        }

    }
    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        return true
    }

    override fun refresh() {
        userName.text = mListener?.mUser?.userName
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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = view.findViewById<TextView>(R.id.userName)

        username.text = mListener?.mUser?.userName

        val services = view.findViewById<ListView>(R.id.services)
        val others = view.findViewById<ListView>(R.id.others)

        val sl = arrayOf(ListItem("Download course Template", R.drawable.ic_file_download_black_24dp, View.OnClickListener {
            val intent = Intent(context, DownloadActivity::class.java)
            val bundle = Bundle()
            mListener?.mUser?.saveUserState(bundle)
            intent.putExtra("USER", bundle)
            startActivity(intent)
        }), ListItem("Upload course Template", R.drawable.ic_cloud_upload_black_24dp, View.OnClickListener {
            val intent = Intent(context, TemplateBrowserActivity::class.java)
            val bundle = Bundle()
            mListener?.mUser?.saveUserState(bundle)
            intent.putExtra("USER", bundle)
            //intent.action = TemplateViewerActivity.ACTION_UPLOAD
            startActivity(intent)
        }), ListItem("Edit profile", R.drawable.ic_edit_black_24dp, View.OnClickListener {
            val intent = Intent(context, ProfileEditActivity::class.java)
            val bundle = Bundle()
            mListener?.mUser?.saveUserState(bundle)
            intent.putExtra("USER", bundle)
            startActivity(intent)
        }), ListItem("Password update", R.drawable.ic_security_black_24dp, View.OnClickListener {
            val intent = Intent(context, PasswordUpdateActivity::class.java)
            startActivity(intent)
        }))


        val ol = arrayOf(ListItem("Rate us", R.drawable.ic_star_black_24dp, View.OnClickListener {
            val uri = Uri.parse("market://details?id=" + context?.packageName)
            val gotoMarket = Intent(Intent.ACTION_VIEW, uri)
            var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            flags = if (Build.VERSION.SDK_INT >= 21) {
                flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            } else {

                flags or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
            }
            gotoMarket.addFlags(flags)
            try {
                startActivity(gotoMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + context?.packageName)))
            }
        }), ListItem("Contact", R.drawable.ic_mail_black_24dp, View.OnClickListener {
            val sendMail = Intent(Intent.ACTION_SEND)
            sendMail.putExtra(Intent.EXTRA_EMAIL, arrayOf(context!!.resources.getString(R.string.email)))
            sendMail.putExtra(Intent.EXTRA_SUBJECT, "Support Ticket")
            sendMail.type = "message/rfc822"
            try {
                startActivity(Intent.createChooser(sendMail, "Send email via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
            }
        }), ListItem("Contribute to open source", R.drawable.ic_github, View.OnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = context!!.resources.getString(R.string.github_repo)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }), ListItem("Sign out", R.drawable.ic_sign_out, View.OnClickListener {
            AppUser.signOut(context!!)
        }))

        services.adapter = ListAdapter(context!!, sl)
        others.adapter = ListAdapter(context!!, ol)

        services.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ -> (adapterView.adapter as ListAdapter).items[i].listener.onClick(view) }

        others.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _, i, _ -> (adapterView.adapter as ListAdapter).items[i].listener.onClick(view) }

    }


    inner class ListItem internal constructor(var text: String, var res: Int, var listener: View.OnClickListener)

    private inner class ListAdapter internal constructor(private val context: Context, val items: Array<ListItem>) : BaseAdapter() {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(i: Int): Any {
            return items[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
            val v = LayoutInflater.from(context).inflate(R.layout.info_item, viewGroup, false)
            val t = v.findViewById<TextView>(R.id.text)
            t.text = (getItem(i) as ListItem).text

            if ((getItem(i) as ListItem).res != 0)
                (v.findViewById<View>(R.id.image) as ImageView).setImageResource((getItem(i) as ListItem).res)
            else
                v.findViewById<View>(R.id.image).visibility = View.INVISIBLE

            return v
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
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

}// Required empty public constructor
