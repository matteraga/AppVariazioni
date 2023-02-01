package me.matteo.appvariazioni

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.fragment.app.Fragment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.judemanutd.autostarter.AutoStartPermissionHelper
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Strings
import me.matteo.appvariazioni.classes.serializers.Settings
import me.matteo.appvariazioni.classes.serializers.SettingsSerializer
import me.matteo.appvariazioni.fragments.*
import me.matteo.appvariazioni.fragments.dataStore
import me.matteo.appvariazioni.workers.DeleteFilesWorker

val Context.dataStore: DataStore<me.matteo.appvariazioni.classes.serializers.Settings> by dataStore(
    fileName = "settings.json",
    serializer = SettingsSerializer()
)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge stuff ( not sure if in needing of this )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        // Set layout and top bar
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialize fragments and Home as first fragment
        val home = HomeFragment()
        val timetable = TimetableFragment()
        val settings = SettingsFragment()
        setCurrentFragment(home, "home")

        // Bottom nav listener
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> setCurrentFragment(home, "home")
                R.id.timetable -> setCurrentFragment(timetable, "timetable")
                R.id.settings -> setCurrentFragment(settings, "settings")
            }
            true
        }

        // Data flow
        val flow = this.dataStore.data
            .catch {
                emit(me.matteo.appvariazioni.classes.serializers.Settings())
            }

        // First check for showing or not the timetable button in the nav bar
        runBlocking {
            val settingsFlow = flow.first()
            val letters = resources.getStringArray(R.array.letters)
            updateBottomNav(settingsFlow.classNumber.toString() + letters[settingsFlow.classLetterPos])
        }

        // Check and request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 0)
            }
        }

        // Whitelist dialog
        val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()
        if (autoStartPermissionHelper.isAutoStartPermissionAvailable(this, true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Avvio automatico")
                .setMessage(
                    "Vuoi aggiungere l'app alla whitelist per poter eseguire il controllo delle variazioni automaticamente?"
                )
                .setNeutralButton("Informazioni") { dialog, _ ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://dontkillmyapp.com")
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    startActivity(Intent.createChooser(intent, "Apri con"))
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Si") { dialog, _ ->
                    autoStartPermissionHelper.getAutoStartPermission(this, true)
                    dialog.dismiss()
                }
                .show()
        }

        /*//Folder creation result
        val folderResult =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) {
                if(it.resultCode == Activity.RESULT_OK) {
                    contentResolver.takePersistableUriPermission(
                        it.data!!.data!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    findViewById<LinearLayout>(R.id.folderLayout).visibility = View.GONE
                    findViewById<ConstraintLayout>(R.id.appLayout).visibility = View.VISIBLE

                } else {
                    Toast.makeText(this, "Errore nella creazione della cartella", Toast.LENGTH_LONG).show()
                }
                return@registerForActivityResult
            }

        //Does already have permission for a folder
        if (this.contentResolver.persistedUriPermissions.size == 0 || this.contentResolver.persistedUriPermissions[0].uri == null) {
            val btnCreate = findViewById<Button>(R.id.createButton)
            btnCreate.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                folderResult.launch(intent)
            }
        } else {
            findViewById<LinearLayout>(R.id.folderLayout).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.appLayout).visibility = View.VISIBLE
        }*/

        /*val workRequest = OneTimeWorkRequestBuilder<AutomaticCheckWorker>()
            //.setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork("ciao", ExistingWorkPolicy.KEEP, workRequest)*/
    }

    // For setting the current fragment and based on that the top bar title
    private fun setCurrentFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment, tag)
            commit()
        }
        val topAppBar = findViewById<MaterialToolbar>(R.id.toolbar)
        topAppBar.title = when (tag) {
            "home" -> "Home"
            "timetable" -> "Orario"
            "settings" -> "Impostazioni"
            else -> "AppVariazioni"
        }
        //tag[0].uppercaseChar() +tag.substring(1)
        //invalidateOptionsMenu()
        //onPrepareOptionsMenu(topAppBar.menu)
        //onCreateOptionsMenu()
    }

    // Function to remove the timetable if the school class is different from 4G
    fun updateBottomNav(new: String) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.timetable).isVisible =
            !(new != "" && new != "4G")
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.top_bar_menu, menu)
        return true
    }*/

    /*override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        //super.onPrepareOptionsMenu(menu)
        val fragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
        if ((fragment != null) && (fragment.tag == "home")) {
            menu?.forEach {
                it.isVisible = true
            }
        } else {
            menu?.forEach {
                it.isVisible = false
            }
        }
        return true
    }*/
}