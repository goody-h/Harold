package com.orsteg.harold.fragments


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

import com.orsteg.harold.R
import com.orsteg.harold.activities.AboutActivity
import com.orsteg.harold.utils.app.Preferences


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : BaseFragment() {

    var web: WebView? = null

    private var filter: String? = null

    private var link: String? = null
    private var copyright: String? = null
    private var displayname: String? = null
    private var downloadFilters: String? = null

    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null

    private var prog: View? = null

    var pageError = ""

    private var stateButton: ImageButton? = null

    
    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden){
            mNav?.visibility = View.GONE

        } else {
            mNav?.visibility = View.VISIBLE

            mListener?.hideActionBtn()
        }
    }

    override val mPrefType: String = Preferences.APP_PREFERENCES

    private var mNav: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mNav = mListener?.getTools(arrayOf(R.id.homeTool_inflater))?.get(0)?.inflate()

        if (isHidden) mNav?.visibility = View.GONE
        else mListener?.hideActionBtn()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        web = view.findViewById(R.id.homeWeb)

        prog = view.findViewById(R.id.prog)

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        link = mFirebaseRemoteConfig!!.getString("Home_Page")

        filter = mFirebaseRemoteConfig!!.getString("Home_Filter")

        copyright = mFirebaseRemoteConfig!!.getString("Home_Copyright_Owner")

        displayname = mFirebaseRemoteConfig!!.getString("Home_Display_Name")

        downloadFilters = mFirebaseRemoteConfig!!.getString("Home_Download_Filters")

        setNavigation()
        setWebView()

        web?.loadUrl(link)


    }

    private fun setWebView() {
        val webSet = web?.settings
        webSet?.javaScriptEnabled = true
        webSet?.setSupportZoom(true)
        webSet?.allowContentAccess = true
        webSet?.allowFileAccessFromFileURLs = true
        webSet?.allowUniversalAccessFromFileURLs = true
        webSet?.allowFileAccess = true
        webSet?.setAppCacheEnabled(true)
        webSet?.databaseEnabled = true
        webSet?.domStorageEnabled = true
        webSet?.javaScriptCanOpenWindowsAutomatically = true

        web?.webViewClient = Client()

    }

    private fun setNavigation() {

        stateButton = mNav?.findViewById(R.id.refresh)

        mNav?.findViewById<View>(R.id.backward)?.setOnClickListener { if (web?.canGoBack() == true) web?.goBack() }
        mNav?.findViewById<View>(R.id.foward)?.setOnClickListener { if (web?.canGoForward() == true) web?.goForward() }
        mNav?.findViewById<View>(R.id.homePage)?.setOnClickListener { web?.loadUrl(link) }
    }

    private inner class WebInterface internal constructor(internal var mContext: Context) {

        val host: String?
            @JavascriptInterface
            get() = filter

        val copyRight: String?
            @JavascriptInterface
            get() = copyright

        val displayName: String?
            @JavascriptInterface
            get() = displayname

        @JavascriptInterface
        fun reloadPage(): String {
            return pageError
        }

        @JavascriptInterface
        fun aboutApp() {
            val intent = Intent(mContext, AboutActivity::class.java)
            startActivity(intent)
        }

    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            var url = url

            if (Uri.parse(url).host == "") web?.removeJavascriptInterface("Android")
            prog!!.visibility = View.VISIBLE
            stateButton!!.setImageResource(R.drawable.ic_close_black_24dp)
            stateButton!!.setOnClickListener {
                web?.stopLoading()
                prog!!.visibility = View.GONE
            }

            if (Uri.parse(url).host.isEmpty() || Uri.parse(url).host.endsWith(filter!!))
                if (!isDownloadable(url)) return false

            val intent = Intent(Intent.ACTION_VIEW)

            if (!url.startsWith("https://") && !url.startsWith("http://"))
                url = "http://" + url

            intent.data = Uri.parse(url)

            startActivity(intent)

            return true
        }

        override fun onPageFinished(view2: WebView, url: String) {
            super.onPageFinished(view2, url)

            prog!!.visibility = View.GONE

            stateButton!!.setImageResource(R.drawable.ic_refresh_black_24dp)
            stateButton!!.setOnClickListener {
                web?.reload()
                prog!!.visibility = View.VISIBLE
            }

        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (Uri.parse(view.url).host != "")
                pageError = view.url
            web?.loadUrl("file:///android_asset/www/error.html")
            web?.addJavascriptInterface(WebInterface(view.context), "Android")
        }
    }

    private fun isDownloadable(url: String): Boolean {

        val regExp = "\\S*$downloadFilters\\S*"

        return url.matches(regExp.toRegex())
    }



    override fun onSaveInstanceState(outState: Bundle) {

    }


    override fun onBackPressed(actionBtn: FloatingActionButton): Boolean {

        if (web?.url == "file:///android_asset/www/error.html")
            return true
        if (web?.canGoBack() == true) {
            web?.goBack()
            return false
        }
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
