package com.orsteg.harold.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.net.ConnectivityManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.orsteg.harold.R
import com.orsteg.harold.database.ResultDataBase
import com.orsteg.harold.dialogs.DetailsDialog
import com.orsteg.harold.dialogs.ListDialog
import com.orsteg.harold.dialogs.LoaderDialog
import com.orsteg.harold.dialogs.SaveDialog
import com.orsteg.harold.utils.app.AndroidPermissions
import com.orsteg.harold.utils.app.Preferences
import com.orsteg.harold.utils.firebase.BannerAd
import com.orsteg.harold.utils.result.*
import com.orsteg.harold.utils.user.AppUser
import com.orsteg.harold.utils.user.User
import kotlinx.android.synthetic.main.activity_template_viewer.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.*

class TemplateViewerActivity : AppCompatActivity(), DetailsDialog.DetailsDialogInterface {

    private var tFile: Uri? = null
    private var tUrl: String? = null
    private var tempExtra: TemplateManager? = null
    var temporaryF: Boolean = false
    private var action: FloatingActionButton? = null
    private var prefs: Preferences? = null
    
    override var mUser: AppUser? = null

    private var loader: LoaderDialog? = null
    private var listView: ExpandableListView? = null
    private var emptyView: View? = null

    private var task: FileDownloadTask? = null
    private var c: Context? = null
    private var auth: FirebaseAuth? = null
    private var li: FirebaseAuth.AuthStateListener? = null

    private var downloadable = false

    private lateinit var mInterstitialAd: InterstitialAd


    private val inputStream: InputStream?
        get() {
            var `in`: InputStream? = null

            try {
                `in` = contentResolver.openInputStream(tFile!!)

            } catch (e: FileNotFoundException) {

            }
            return `in`
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_viewer)

        // get views
        action = findViewById(R.id.action)
        listView = findViewById(R.id.list)
        emptyView = findViewById(R.id.emptyView)
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        supportActionBar?.title = "Template preview"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adView.loadAd(AdRequest.Builder().build())
        BannerAd.setListener(adView)
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.loadAd(AdRequest.Builder().build())


        loader = LoaderDialog(this)

        listView?.setGroupIndicator(null)

        // Init params
        prefs = Preferences(this, Preferences.RESULT_PREFERENCES)
        mUser = null
        c = this
        auth = FirebaseAuth.getInstance()

        // parse incoming intent
        val intent = intent

        if (intent != null) {
            // Check extras
            if (intent.hasExtra("USER"))
                mUser = AppUser.getSavedState(intent.getBundleExtra("USER"))
            else
                checkAuth()

            if (intent.data != null) {
                tFile = intent.data
                temporaryF = intent.getBooleanExtra(EXTRA_TEMPORARY, false)
            } else if (intent.hasExtra(EXTRA_URL)) {
                tUrl = intent.getStringExtra(EXTRA_URL)
                if (intent.hasExtra(EXTRA_TEMPLATE) && intent.getBundleExtra(EXTRA_TEMPLATE) != null)
                    tempExtra = TemplateManager.restoreState(intent.getBundleExtra(EXTRA_TEMPLATE))
            } else {
                endActivity()
            }

            // Check action
            if (intent.action != null) {
                when (intent.action) {
                    ACTION_UPLOAD -> setUpToUploadTemplate()
                    ACTION_SAVE -> setUpToSaveTemplate()
                    ACTION_DOWNLOAD_UPLOAD -> {
                        downloadable = true
                        setUpToUploadTemplate()
                    }
                    ACTION_DOWNLOAD_APPLY -> {
                        downloadable = true
                        setUpToApplyTemplate()
                    }
                    ACTION_APPLY -> setUpToApplyTemplate()
                    else -> setUpToApplyTemplate()
                }
            } else {
                setUpToApplyTemplate()
            }
        } else {
            endActivity()
        }


        val isExternalStorage = AndroidPermissions.instance.checkWriteExternalStoragePermission(this)
        if (!isExternalStorage) {
            AndroidPermissions.instance.requestForWriteExternalStoragePermission(this)
        } else initiate()

    }

    fun initiate(){
        if (downloadable) {
            startDownload()
        } else
            fileValidation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            AndroidPermissions.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initiate()
                } else {
                    Toast.makeText(this, "Storage permission not granted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun checkAuth() {
        li = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                auth!!.removeAuthStateListener(li!!)
                mUser = AppUser.getPersistentUser(this)
                getUser(user.uid, false)
            } else
                mUser = null
        }

        auth!!.addAuthStateListener(li!!)

    }

    private fun endActivity() {
        Toast.makeText(this, "Activity started wrongly, use specified intent filters", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun startDownload() {
        loader!!.show()
        if (networkTest()) {
            loader!!.setLoadMessage("Downloading template, please wait...")
            download()
        } else {
            setDownloadError("No network connection!")
        }
    }

    fun download() {

        val inTd = Thread(Runnable {
            val storage = FirebaseStorage.getInstance()

            try {
                val storageRef = storage.getReferenceFromUrl(tUrl!!)

                var f = FileHandler.getTemplateFile(tempExtra!!.name)

                var i = 1

                while (f.exists()) {
                    f = FileHandler.getTemplateFile(tempExtra!!.name + i) 
                    i++
                }

                val file = f


                task = storageRef.getFile(file)
                task!!.addOnSuccessListener {
                    tFile = Uri.fromFile(file)
                    runOnUiThread {
                        loader!!.dismiss()
                        fileValidation()
                    }
                }.addOnFailureListener {
                    tFile = null
                    runOnUiThread { setDownloadError("Failed to download template!") }
                }
                runOnUiThread {
                    loader!!.setCancelable {
                        task!!.cancel()
                        finish()
                    }
                }

            } catch (e: Exception) {
                tFile = null
                runOnUiThread { setDownloadError("Error downloading template!") }
            }
        })

        inTd.start()
    }

    private fun setDownloadError(mes: String) {
        loader!!.setFailed(mes, { finish() }) {
            loader!!.setLoading("Downloading template, please wait...")
            startDownload()
        }
    }

    private fun setUpToApplyTemplate() {
        action!!.setImageResource(R.drawable.ic_format_paint_black_24dp)
        action!!.setOnClickListener { applyTemplate() }
    }

    private fun applyTemplate() {
        ListDialog(this, "Add Courses", arrayListOf("Append", "Override")) { _, position ->
            when(position) {
                0 -> startApplying(false)
                1 -> startApplying (true)
            }
        }.show()

    }

    private fun startApplying(how: Boolean) {
        runOnUiThread {
            loader!!.show()
            loader!!.setLoading("Applying template, please wait")
            loader!!.hideAbort()
        }

        val td = Thread(Runnable {

            val mEditor = ResultEditor(this)

            if (mEditor.addTemplate(inputStream!!, how))
                runOnUiThread {
                    Toast.makeText(c, "Template set success", Toast.LENGTH_SHORT).show()
                    finish()
                }
            else
                runOnUiThread {
                    loader!!.setFailed("Error applying template!", { loader!!.dismiss() }) {
                        loader!!.dismiss()
                        applyTemplate()
                    }
                }
        })
        td.start()
    }

    private fun setUpToSaveTemplate() {
        action!!.setImageResource(R.drawable.ic_save_black_24dp)
        action!!.setOnClickListener { saveTemplate() }
    }

    private fun saveTemplate() {
        val save: SaveDialog

        save = SaveDialog(this)

        save.show()
        save.hideNote()
        save.save?.setOnClickListener { v ->
            val name = save.filname
            save.dismiss()
            runOnUiThread {
                loader!!.show()
                loader!!.setLoading("Saving template, please wait")
                loader!!.hideAbort()
            }
            Toast.makeText(v.context, "Saving template", Toast.LENGTH_SHORT).show()
            save(name, object : OnCompleteListener {
                override fun onComplete() {
                    runOnUiThread {
                        loader!!.dismiss()
                        Toast.makeText(c, "Save success", Toast.LENGTH_SHORT).show()

                        showAd()
                    }
                }

                override fun onFailure() {
                    runOnUiThread {
                        loader!!.setFailed("Error saving template!", { loader!!.dismiss() }) {
                            loader!!.dismiss()
                            saveTemplate()
                        }
                        Toast.makeText(c, "Save failure", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

    }

    fun showAd(){
        if (mInterstitialAd.isLoaded){
            mInterstitialAd.show()
        }
    }

    fun save(name: String, complete: OnCompleteListener) {

        val td = Thread(Runnable {
            var file = FileHandler.getTemplateFile(name)

            var i = 1

            while (file.exists()) {
                file = FileHandler.getTemplateFile(name + i)
                i++
            }


            try {
                
                val `in` = inputStream
                val outputStream = FileOutputStream(file)
                
                var c: Int = `in`!!.read()
                
                while (c  != -1) {
                    outputStream.write(c)

                    c = `in`.read()
                }

                `in`.close()
                outputStream.close()

                complete.onComplete()

            } catch (e: IOException) {

                complete.onFailure()
            }
        })

        td.start()
    }

    interface OnCompleteListener {
        fun onComplete()
        fun onFailure()
    }

    private fun fileValidation() {
        loader!!.show()
        loader!!.hideAbort()
        loader!!.setLoading("Checking template, please wait...")

        val td = Thread(Runnable {
            val handler = FileHandler()
            var b: JSONObject? = null
            var valid: Boolean? = false

            if (tFile != null) {
                try {
                    val inputStream = contentResolver.openInputStream(tFile!!)
                    b = handler.validateFile(inputStream)

                } catch (e: IOException) {
                    Log.d(TAG, "getInputData: Read data error")
                }
            }

            if (b != null)
                valid = b.getBoolean("validity")

            if (valid == true)
                loadTemplate(b!!.getJSONArray("courses"))
            else {
                runOnUiThread {
                    Toast.makeText(c, "Invalid file format", Toast.LENGTH_SHORT).show()
                    finish()
                }

            }
        })

        td.start()

    }

    private fun loadTemplate(obj: JSONArray) {

        val semesters = ArrayList<TemplateSemester>()

        val ss = arrayOf("1st", "2nd", "3rd")
        
        try {

            val ar = (0 until obj.length())
                    .mapTo(ArrayList<JSONObject>()) { obj.getJSONObject(it) }

            ar.sortWith(Comparator { o1, o2 ->
                val i1 = o1.getInt("semId")
                val i2 = o2.getInt("semId")
                if (i1 > i2) 1 else if (i1 < i2) -1 else 0
            })

            var semId = 0
            var semester: TemplateSemester? = null


            (0 until ar.size)
                    .map { ar[it] }
                    .forEach {
                        if (it.getInt("semId") != semId) {

                            if (semester != null)semesters.add(semester!!)
                            
                            semId = it.getInt("semId")

                            val lelN = Math.floor(semId /1000.0).toInt()
                            val semN = (semId % 1000) / 100 - 1
                            
                            semester = TemplateSemester("Level ${lelN*100}  ${ss[semN]} Semester")
                        }

                        semester?.add(TemplateCourse(it.getString("title"), it.getString("code")
                                , it.getDouble("unit")))
                    }

            if (semester != null)semesters.add(semester!!)

        } catch (e: JSONException){

        }
        
        val adapter = ListAdapter(this, semesters)

        runOnUiThread {
            listView!!.setAdapter(adapter)

            if (semesters.size == 0) {
                emptyView!!.visibility = View.VISIBLE
            } else {

                for (i in semesters.indices) {
                    listView!!.expandGroup(i)
                }
                val v = layoutInflater.inflate(R.layout.message, listView, false)
                v.findViewById<View>(R.id.close).setOnClickListener { listView!!.removeHeaderView(v) }
                (v.findViewById<View>(R.id.message) as TextView).text = "This is a template file. Use it to populate your course list, or upload it so others can use"
                listView!!.addHeaderView(v)
            }

            loader!!.dismiss()
        }

    }


    private fun setUpToUploadTemplate() {
        action!!.setImageResource(R.drawable.ic_cloud_upload_black_24dp)
        action!!.setOnClickListener { uploadTemplate() }
    }

    fun uploadTemplate() {
        if (mUser != null) {
            if (mUser!!.canUpload)
                if (mUser!!.tempCount < AppUser.MAX_UPLOADS)
                    startUpload()
                else {
                    Snackbar.make(action!!, "Max uploads reached. Delete previous ones to continue", Snackbar.LENGTH_LONG)
                            .setAction("Delete") { view ->
                                val i = Intent(view.context, DownloadActivity::class.java)
                                i.action = ACTION_UPLOAD
                                val b = Bundle()
                                mUser!!.saveUserState(b)
                                i.putExtra("USER", b)
                                startActivity(i)
                            }.setActionTextColor(resources.getColor(R.color.colorAccent)).show()
                }
            else
                Toast.makeText(this, "Sorry you are currently not allowed to upload templates", Toast.LENGTH_SHORT).show()
        } else {
            val ls = ArrayList<String>()
            ls.add("Login")
            ls.add("SignUp")
            
            val dialog = ListDialog(this, "You are Offline!", ls) { _, position ->
                val intent: Intent
                li = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        auth!!.removeAuthStateListener(li!!)

                        loader!!.show()
                        loader!!.hideAbort()
                        loader!!.setLoading("Fetching user data, please wait...")
                        getUser(user.uid, true)
                    } else
                        mUser = null
                }

                auth!!.addAuthStateListener(li!!)
                when (position) {
                    0 -> {
                        intent = Intent(c, LoginActivity::class.java)

                        startActivity(intent)
                    }
                    1 -> {
                        intent = Intent(c, SignUpActivity::class.java)

                        startActivity(intent)
                    }
                }
            }

            dialog.show()
        }
    }

    private fun getUser(id: String, act: Boolean) {
        FirebaseDatabase.getInstance().getReference("Users").child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user1 = dataSnapshot.getValue(User::class.java)

                val user: AppUser?
                if (user1 != null)
                    user = AppUser(user1, id)
                else
                    user = null

                if (act)
                    runOnUiThread { loader!!.dismiss() }

                if (user != null) {
                    mUser = user
                    mUser!!.persistUser(this@TemplateViewerActivity)
                    if (act)
                        uploadTemplate()
                } else {
                    Toast.makeText(this@TemplateViewerActivity, "You are offline. Try again or contact admin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun startUpload() {
        val task = ResultUploader(this, this, tFile, null, mUser!!)
        task.execute()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.template_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_help -> {
                val intent = Intent(Intent.ACTION_VIEW)
                val url = FirebaseRemoteConfig.getInstance().getString("help_site")
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
            R.id.apply -> applyTemplate()
            R.id.upload -> uploadTemplate()
            R.id.details -> showDetails("")
        }

        return super.onOptionsItemSelected(item)
    }

    private inner class TemplateCourse internal constructor(internal var title: String, internal var code: String, internal var cu: Double)

    private inner class TemplateSemester internal constructor(internal var header: String) {
        private val courses: ArrayList<TemplateCourse> = ArrayList()

        internal fun add(course: TemplateCourse) {
            courses.add(course)
        }

        internal operator fun get(i: Int): TemplateCourse {
            return courses[i]
        }

        internal fun size(): Int {
            return courses.size
        }
    }

    private inner class ListAdapter(internal var context: Context, internal var semesters: ArrayList<TemplateSemester>) : ExpandableListAdapter {

        override fun registerDataSetObserver(dataSetObserver: DataSetObserver) {

        }

        override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver) {

        }

        override fun getGroupCount(): Int {
            return semesters.size
        }

        override fun getChildrenCount(i: Int): Int {
            return semesters[i].size()
        }

        override fun getGroup(i: Int): TemplateSemester {
            return semesters[i]
        }

        override fun getChild(i: Int, i1: Int): TemplateCourse {
            return semesters[i][i1]
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

        override fun getGroupView(i: Int, b: Boolean, view: View?, viewGroup: ViewGroup): View {
            val v: View
            if (view == null)
                v = LayoutInflater.from(context).inflate(R.layout.temp_head, viewGroup, false)
            else
                v = view

            (v.findViewById<View>(R.id.title) as TextView).text = getGroup(i).header
            return v
        }

        override fun getChildView(i: Int, i1: Int, b: Boolean, view: View?, viewGroup: ViewGroup): View {
            val v: View
            if (view == null)
                v = LayoutInflater.from(context).inflate(R.layout.temp_course_item, viewGroup, false)
            else
                v = view

            (v.findViewById<View>(R.id.Course_title) as TextView).text = getChild(i, i1).title
            (v.findViewById<View>(R.id.Course_code) as TextView).text = getChild(i, i1).code
            val s =
                    if (getChild(i, i1).cu % 1 == 0.0) getChild(i, i1).cu.toInt().toString() + " CU"
                    else getChild(i, i1).cu.toString() + " CU"

            (v.findViewById<View>(R.id.cu) as TextView).text = s
            (v.findViewById<View>(R.id.index) as TextView).text = (i1 + 1).toString()
            return v
        }

        override fun isChildSelectable(i: Int, i1: Int): Boolean {
            return false
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
            listView!!.expandGroup(i)
        }

        override fun getCombinedChildId(l: Long, l1: Long): Long {
            return 0
        }

        override fun getCombinedGroupId(l: Long): Long {
            return 0
        }
    }

    override fun networkTest(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    override fun showDetails(id: String) {
        if (tempExtra != null) {
            DetailsDialog(this,  tempExtra!!).show()
        } else
            Toast.makeText(this, "No details available", Toast.LENGTH_SHORT).show()
    }

    override fun uiThread(run: () -> Unit) {
        runOnUiThread { run() }
    }


    companion object {

        // Intent actions
        val ACTION_APPLY = "com.orsteg.harold.template.action.ACTION_APPLY"
        val ACTION_UPLOAD = "com.orsteg.harold.template.action.ACTION_UPLOAD"
        val ACTION_SAVE = "com.orsteg.harold.template.action.ACTION_SAVE"
        val ACTION_DOWNLOAD_UPLOAD = "com.orsteg.harold.template.action.ACTION_DOWNLOAD_UPLOAD"
        val ACTION_DOWNLOAD_APPLY = "com.orsteg.harold.template.action.ACTION_DOWNLOAD_APPLY"

        // Intent extras
        val EXTRA_URL = "com.orsteg.harold.template.extra.EXTRA_URL"
        val EXTRA_TEMPORARY = "com.orsteg.harold.template.extra.EXTRA_TEMPORARY"
        val EXTRA_TEMPLATE = "com.orsteg.harold.template.extra.EXTRA_TEMPLATE"

        // Activity parameters
        private val TAG = "mytag"
    }

}
