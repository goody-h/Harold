package com.orsteg.harold.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.orsteg.harold.R
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.PlainDialog
import com.orsteg.harold.dialogs.WarningDialog
import com.orsteg.harold.utils.app.AndroidPermissions
import com.orsteg.harold.utils.result.FileHandler
import com.orsteg.harold.utils.user.AppUser
import java.io.File
import java.util.ArrayList

class TemplateBrowserActivity : AppCompatActivity() {

    private var iAction: String? = TemplateViewerActivity.ACTION_APPLY
    private var listView: ListView? = null
    private var mUser: AppUser? = null
    private var loader: LoaderDialog? = null
    private var c: Context? = null
    private var isInMultiSelect = false
    private var multiSelectView: View? = null
    private var optionsView: View? = null
    private var selectedFiles: ArrayList<Int>? = null


    private val files: Array<File>?
        get() {
            val f = FileHandler.TEMPLATES_DIR

            return f.listFiles { _, name -> name.endsWith(".tmp.txt") }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_browser)

        c = this

        // get views
        listView = findViewById(R.id.list)
        multiSelectView = findViewById(R.id.multiselect)
        optionsView = findViewById(R.id.options)

        loader = LoaderDialog(this)
        setSupportActionBar(findViewById<View>(R.id.toolbar4) as Toolbar)
        supportActionBar?.title = "Template browser"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mUser = AppUser.getPersistentUser(this)
        selectedFiles = ArrayList()

        // parse incoming intent
        val intent = intent

        if (intent != null) {
            if (intent.action != null) iAction = intent.action
            if (intent.hasExtra("USER")) mUser = AppUser.getSavedState(intent.getBundleExtra("USER"))
        }

        findViewById<View>(R.id.online).setOnClickListener { goOnline() }

        findViewById<View>(R.id.storage).setOnClickListener {
            val openIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            openIntent.addCategory(Intent.CATEGORY_OPENABLE)
            openIntent.type = "*/*"
            startActivityForResult(openIntent, 45)
        }

        if (iAction == TemplateViewerActivity.ACTION_UPLOAD) {
            findViewById<View>(R.id.active).visibility = View.VISIBLE
            findViewById<View>(R.id.active).setOnClickListener { getCurrentTemplate() }
        }

    }

    override fun onBackPressed() {
        if (isInMultiSelect)
            exitMultiSelect()
        else
            super.onBackPressed()
    }

    private interface PlainDialogClick {
        fun onClick(dialog: PlainDialog)
    }


    private fun startPlainDialog(title: String, clicker: PlainDialogClick, def: String) {
        val dialog = PlainDialog(this, title, 1, def)
        dialog.show()
        dialog.save?.setOnClickListener { clicker.onClick(dialog) }
    }

    fun startMultiSelect(i: Int) {
        if (!isInMultiSelect) {
            optionsView?.visibility = View.GONE
            multiSelectView?.visibility = View.VISIBLE
            isInMultiSelect = true

            findViewById<View>(R.id.share).setOnClickListener {

                val files = ArrayList<Uri>()

                val f = (listView?.adapter as ListAdapter).items

                for (ind in selectedFiles!!.indices) {
                    try {

                        val uri = Uri.fromFile(f[selectedFiles!![ind]])

                        if (uri != null){
                            files.add(uri)
                        }
                    } catch (e: IllegalArgumentException){
                        Log.e("Template Browser", "This file can't be shared: ${f[selectedFiles!![ind]].name}")
                    }
                }


                if (files.size != 0) {
                    val share = Intent(Intent.ACTION_SEND_MULTIPLE)
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    share.type = "text/*"

                    share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)

                    startActivity(Intent.createChooser(share, "Share via."))
                }
                runOnUiThread { exitMultiSelect() }
            }

            findViewById<View>(R.id.delete).setOnClickListener {
                WarningDialog(this, "Sure to delete?"){
                    val f = (listView?.adapter as ListAdapter).items

                    for (i in selectedFiles!!.indices) {
                        f[selectedFiles!![i]].delete()
                    }

                    runOnUiThread { exitMultiSelect() }

                }.show()
            }
            findViewById<View>(R.id.remame).setOnClickListener {
                startPlainDialog("Rename template", object : PlainDialogClick {
                    override fun onClick(dialog: PlainDialog) {
                        val s = dialog.value?.text.toString()

                        var nf = FileHandler.getTemplateFile(s)

                        var c = 1
                        while (nf.exists()) {
                            nf = FileHandler.getTemplateFile(s + c)
                            c++
                        }

                        (listView?.adapter as ListAdapter).items[selectedFiles!![0]].renameTo(nf)

                        dialog.dismiss()
                        runOnUiThread { exitMultiSelect() }
                    }
                }, (listView?.adapter as ListAdapter).items[selectedFiles!![0]].name.replace(".tmp.txt", ""))
            }
            findViewById<View>(R.id.exit).setOnClickListener { exitMultiSelect() }
        }

        if (selectedFiles?.contains(i) == false) {
            selectedFiles?.add(i)
        } else {
            selectedFiles?.remove(i)
            if (selectedFiles?.size == 0) exitMultiSelect()
        }

        if (selectedFiles!!.size > 1)
            findViewById<View>(R.id.remame).visibility = View.GONE
        else
            findViewById<View>(R.id.remame).visibility = View.VISIBLE

        (findViewById<View>(R.id.count) as TextView).text = selectedFiles?.size.toString() + " Selected"

        (listView?.adapter as ListAdapter).notifyDataSetChanged()

    }



    private fun exitMultiSelect() {
        selectedFiles = ArrayList()
        optionsView?.visibility = View.VISIBLE
        multiSelectView?.visibility = View.GONE
        isInMultiSelect = false
        setupList()
    }

    override fun onStart() {
        super.onStart()
        val isExternalStorage = AndroidPermissions.instance.checkWriteExternalStoragePermission(this)
        if (!isExternalStorage) {
            AndroidPermissions.instance.requestForWriteExternalStoragePermission(this)
        } else setupList()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            AndroidPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupList()
                } else {
                    Toast.makeText(this, "Storage permission not granted!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setupList() {
        var f: Array<File>? = files
        if (f == null) f = Array(0, {_ -> File("") })

        listView?.adapter = ListAdapter(this, f)

        listView?.emptyView = findViewById(R.id.empty)

        listView?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if (!isInMultiSelect) {
                val uri = Uri.fromFile((adapterView.adapter as ListAdapter).getItem(i))
                val it = Intent(adapterView.context, TemplateViewerActivity::class.java)
                it.data = uri
                it.action = iAction

                if (mUser != null) {
                    val b = Bundle()
                    mUser?.saveUserState(b)
                    it.putExtra("USER", b)
                }

                startActivity(it)
            } else {
                startMultiSelect(i)
            }
        }

        listView?.onItemLongClickListener = AdapterView.OnItemLongClickListener { adapterView, view, i, l ->
            startMultiSelect(i)

            true
        }
    }

    private inner class ListAdapter internal constructor(private val context: Context, val items: Array<File>) : BaseAdapter() {

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(i: Int): File {
            return items[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
            var view = view
            if (view == null) view = LayoutInflater.from(context).inflate(R.layout.temp_item, viewGroup, false)
            val t = view!!.findViewById<TextView>(R.id.title)
            val va = view.findViewById<TextView>(R.id.value)
            t.text = getItem(i).name
            val s = getItem(i).length() / 1024.0

            val ss = s.toString()
            val point = ss.lastIndexOf(".")

            val fs = if (point == -1 || ss.length < point + 4) ss else ss.substring(0, point + 3)

            va.text = "size $fs KB"

            if (selectedFiles!!.contains(i))
                (view.findViewById<View>(R.id.icon) as ImageView).setImageResource(R.drawable.ic_done_black_24dp)
            else
                (view.findViewById<View>(R.id.icon) as ImageView).setImageResource(R.drawable.ic_format_paint_black_24dp)
            return view
        }
    }

    private fun goOnline() {
        startDownloadActivity()
    }

    private fun startDownloadActivity() {
        val intent = Intent(this, DownloadActivity::class.java)
        intent.action = iAction
        if (mUser != null) {
            val b = Bundle()
            mUser?.saveUserState(b)
            intent.putExtra("USER", b)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId

        when (id) {
            R.id.action_settings -> return true
            R.id.action_help -> {

            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getCurrentTemplate() {

        val td = Thread(Runnable {
            val handler = FileHandler()

            val file = FileHandler.getTemporaryTemp(this)
            handler.createTemporaryFile(this)

            val i = Intent(c, TemplateViewerActivity::class.java)
            i.data = Uri.fromFile(file)
            i.action = TemplateViewerActivity.ACTION_UPLOAD
            i.putExtra(TemplateViewerActivity.EXTRA_TEMPORARY, true)

            startActivity(i)
        })

        td.start()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 45 && resultCode == Activity.RESULT_OK) {
            val uri: Uri?
            if (data != null) {
                uri = data.data
                if (uri != null) {
                    val i = Intent(this, TemplateViewerActivity::class.java)
                    i.data = uri
                    i.action = iAction

                    if (mUser != null) {
                        val b = Bundle()
                        mUser?.saveUserState(b)
                        i.putExtra("USER", b)
                    }

                    startActivity(i)
                }
            }
        }
    }
}
