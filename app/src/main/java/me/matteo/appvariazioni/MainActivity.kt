package me.matteo.appvariazioni

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Utils
import me.matteo.appvariazioni.fragments.HomeFragment
import me.matteo.appvariazioni.fragments.SettingsFragment
import me.matteo.appvariazioni.fragments.TimetableFragment


class MainActivity : AppCompatActivity() {

    private val utils = Utils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.topAppBar))

        val home = HomeFragment()
        val timetable = TimetableFragment()
        val settings = SettingsFragment()
        setCurrentFragment(home, "home")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.home -> setCurrentFragment(home, "home")
                R.id.timetable -> setCurrentFragment(timetable, "timetable")
                R.id.settings -> setCurrentFragment(settings, "settings")
            }
            true
        }
        updateBottomNav()

        //Check and request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        //Folder creation result
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
        }

        /*val workRequest = OneTimeWorkRequestBuilder<AutomaticCheckWorker>()
            //.setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork("ciao", ExistingWorkPolicy.KEEP, workRequest)*/
    }

    private fun setCurrentFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment, tag)
            commit()
        }
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
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

    fun updateBottomNav() {
        val sharedPref = this.getSharedPreferences(Key.SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
        val schoolClass = sharedPref.getString(Key.CLASSROOM, "")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.timetable).isVisible =
            !(schoolClass != "" && schoolClass != "4G")
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