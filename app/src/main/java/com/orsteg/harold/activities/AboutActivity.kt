package com.orsteg.harold.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.orsteg.harold.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)


        val items = arrayOf(InfoItem("Rate Us", View.OnClickListener {
            val uri = Uri.parse("market://details?id=" + packageName)
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
                        Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)))
            }
        }), InfoItem("Contact", View.OnClickListener { view ->
            val sendMail = Intent(Intent.ACTION_SEND)
            sendMail.putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.email)))
            sendMail.putExtra(Intent.EXTRA_SUBJECT, "Support Ticket")
            sendMail.type = "message/rfc822"
            try {
                startActivity(Intent.createChooser(sendMail, "Send email via"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(view.context, "No email app installed", Toast.LENGTH_SHORT).show()
            }
        }), InfoItem("Open source contribution", View.OnClickListener { view ->
            val intent = Intent(Intent.ACTION_VIEW)
            val url = view.context.resources.getString(R.string.github_repo)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }), InfoItem("Privacy Policy", View.OnClickListener { view ->
            val intent = Intent(Intent.ACTION_VIEW)
            val url = view.context.resources.getString(R.string.privacy_policy)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }), InfoItem("Terms and Conditions", View.OnClickListener { view ->
            val intent = Intent(Intent.ACTION_VIEW)
            val url = view.context.resources.getString(R.string.terms_and_conditions)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }))

        val list = findViewById<ListView>(R.id.options)

        list.adapter = InfoList(this, items)

        list.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, _ -> (adapterView.adapter as InfoList).items[i].listener.onClick(view) }
    }

    inner class InfoItem internal constructor(var text: String, var listener: View.OnClickListener)

    private inner class InfoList internal constructor(private val context: Context, val items: Array<InfoItem>) : BaseAdapter() {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(i: Int): Any {
            return items[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            var view = view
            if (view == null) view = LayoutInflater.from(context).inflate(R.layout.info_item, viewGroup, false)
            val t = view!!.findViewById<TextView>(R.id.text)
            t.text = (getItem(i) as InfoItem).text
            view.findViewById<View>(R.id.image).visibility = View.GONE
            return view
        }
    }

}
