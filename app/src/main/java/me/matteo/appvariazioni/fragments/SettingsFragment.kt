package me.matteo.appvariazioni.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import me.matteo.appvariazioni.MainActivity
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Utils
import me.matteo.appvariazioni.workers.AutomaticCheckWorker
import me.matteo.appvariazioni.workers.DeleteFilesWorker
import me.matteo.appvariazioni.workers.ManualCheckWorker
import me.matteo.appvariazioni.workers.ShitSiteCheckWorker
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val utils = Utils()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val sharedPref =
            activity.getSharedPreferences(Key.SETTINGS_PREFERENCES, Context.MODE_PRIVATE)

        if (sharedPref != null && activity is MainActivity) {
            //Background check
            val backgroundSwitch = view.findViewById<SwitchMaterial>(R.id.backgroundSwitch)
            backgroundSwitch.isChecked = sharedPref.getBoolean(Key.BACKGROUND_CHECK, false)
            backgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    with(sharedPref.edit()) {
                        putBoolean(Key.BACKGROUND_CHECK, true)
                        apply()
                    }

                    if (!utils.isWorkScheduled(WorkManager.getInstance(activity), "automatic")) {
                        val currentDate = Calendar.getInstance()
                        val dueDate = Calendar.getInstance()
                        dueDate.set(Calendar.HOUR_OF_DAY, 19)
                        dueDate.set(Calendar.MINUTE, 0)
                        dueDate.set(Calendar.SECOND, 0)
                        if (dueDate.before(currentDate)) {
                            dueDate.add(Calendar.HOUR_OF_DAY, 24)
                        }
                        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
                        val dailyWorkRequest = OneTimeWorkRequestBuilder<AutomaticCheckWorker>()
                            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                            .addTag("automatic")
                            .build()
                        WorkManager.getInstance(activity).enqueue(dailyWorkRequest)
                    }

                } else {
                    with(sharedPref.edit()) {
                        putBoolean(Key.BACKGROUND_CHECK, false)
                        apply()
                    }

                    if (utils.isWorkScheduled(WorkManager.getInstance(activity), "automatic")) {
                        WorkManager.getInstance(activity).cancelAllWorkByTag("automatic")
                    }
                }
            }

            //Classroom spinner
            val classroomSpinner = view.findViewById<Spinner>(R.id.classroomSpinner)
            ArrayAdapter.createFromResource(
                activity,
                R.array.classrooms_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                classroomSpinner.adapter = adapter
            }
            classroomSpinner.setSelection(sharedPref.getInt(Key.CLASSROOM_POS, 0))

            //Classroom spinner listener
            class SpinnerActivity() : Activity(), AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (pos != sharedPref.getInt(Key.CLASSROOM_POS, -1)) {
                        with(activity.getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE).edit()) {
                            clear()
                            apply()
                        }
                        activity.getSharedPreferences(Key.SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putInt(Key.CLASSROOM_POS, pos)
                            putString(Key.CLASSROOM, parent.selectedItem.toString())
                            apply()
                        }
                        activity.updateBottomNav()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    with(activity.getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE).edit()) {
                        clear()
                        apply()
                    }
                    activity.getSharedPreferences(Key.SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putInt(Key.CLASSROOM_POS, 0)
                        putString(Key.CLASSROOM, parent.getItemAtPosition(0).toString())
                        apply()
                    }
                    activity.updateBottomNav()
                }
            }
            classroomSpinner.onItemSelectedListener = SpinnerActivity()

            //Notification permission
            val notificationLayout = view.findViewById<LinearLayout>(R.id.notificationLayout)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationText = view.findViewById<TextView>(R.id.notificationText)
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    notificationText.text = "Negato"
                } else {
                    notificationText.text = "Concesso"
                }

                notificationLayout.setOnClickListener {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    if (intent.resolveActivity(activity.packageManager) != null) {
                        startActivity(intent)
                    }
                }
            } else {
                notificationLayout.visibility = View.GONE
            }

            val workManager = WorkManager.getInstance(activity)

            //Delete file
            val fileManagerBtn = view.findViewById<LinearLayout>(R.id.fileLayout)
            fileManagerBtn.setOnClickListener {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Conferma")
                    .setMessage(
                        "Sei sicuro di voler eliminare tutti i file PDF in \"" + DocumentFile.fromTreeUri(
                            activity,
                            activity.contentResolver.persistedUriPermissions[0].uri
                        )!!.name + "\""
                    )
                    .setNegativeButton("No") { _, _ ->

                    }
                    .setPositiveButton("Si") { _, _ ->
                        val workRequest = OneTimeWorkRequestBuilder<DeleteFilesWorker>()
                            .build()
                        workManager.enqueueUniqueWork(
                            "delete",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                        val snackbar = Snackbar.make(
                            view,
                            "Eliminazione in corso...",
                            Snackbar.LENGTH_LONG
                        )
                        snackbar.anchorView =
                            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                        snackbar.show()
                        val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
                        val observer = object : androidx.lifecycle.Observer<WorkInfo> {
                            override fun onChanged(workInfo: WorkInfo?) {
                                if (workInfo != null) {
                                    if (workInfo.state.isFinished) {
                                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                            view.findViewById<TextView>(R.id.occupiedSpaceTxt).text = "0.0MB"
                                        }
                                        liveData.removeObserver(this)
                                    }
                                }
                            }
                        }
                        liveData.observe(viewLifecycleOwner, observer)
                    }.show()
            }

            val checkButton = view.findViewById<Button>(R.id.checkButton)
            checkButton.setOnClickListener {
                val workRequest = OneTimeWorkRequestBuilder<ManualCheckWorker>()
                    .build()
                workManager.enqueueUniqueWork("manual", ExistingWorkPolicy.KEEP, workRequest)
                checkButton.isEnabled = false
                checkButton.text = "attendi..."
                val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
                val observer = object : androidx.lifecycle.Observer<WorkInfo> {
                    override fun onChanged(workInfo: WorkInfo?) {
                        if (workInfo != null) {
                            if (workInfo.state.isFinished) {
                                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                    val snackbar = Snackbar.make(
                                        activity.findViewById(android.R.id.content),
                                        "Controllo eseguito (swipe down)",
                                        Snackbar.LENGTH_LONG
                                    )
                                    snackbar.anchorView =
                                        activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                                    snackbar.show()
                                } else {
                                    val snackbar = Snackbar.make(
                                        activity.findViewById(android.R.id.content),
                                        "Errore durante il controllo",
                                        Snackbar.LENGTH_LONG
                                    )
                                    snackbar.anchorView =
                                        activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                                    snackbar.show()
                                }
                                liveData.removeObserver(this)
                                checkButton.isEnabled = true
                                checkButton.text = "controlla sito scuola"
                            }
                        }
                    }
                }
                //liveData.observe(viewLifecycleOwner, observer)
                liveData.observeForever(observer)
            }

            val checkShitButton = view.findViewById<Button>(R.id.checkShitButton)
            checkShitButton.setOnClickListener {
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    val workRequest = OneTimeWorkRequestBuilder<ShitSiteCheckWorker>()
                        .build()
                    workManager.enqueueUniqueWork("shitSite", ExistingWorkPolicy.KEEP, workRequest)
                    checkShitButton.isEnabled = false
                    checkShitButton.text = "attendi..."
                    val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
                    val observer = object : androidx.lifecycle.Observer<WorkInfo> {
                        override fun onChanged(workInfo: WorkInfo?) {
                            if (workInfo != null) {
                                if (workInfo.state.isFinished) {
                                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                        val snackbar = Snackbar.make(
                                            activity.findViewById(android.R.id.content),
                                            "Controllo eseguito (swipe down)",
                                            Snackbar.LENGTH_LONG
                                        )
                                        snackbar.anchorView =
                                            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                                        snackbar.show()
                                    } else {
                                        val snackbar = Snackbar.make(
                                            activity.findViewById(android.R.id.content),
                                            "Errore durante il controllo, riprovare",
                                            Snackbar.LENGTH_LONG
                                        )
                                        snackbar.anchorView =
                                            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                                        snackbar.show()
                                    }
                                    liveData.removeObserver(this)
                                    checkShitButton.isEnabled = true
                                    checkShitButton.text = "controlla sito di \uD83D\uDCA9"
                                }
                            }
                        }
                    }
                    //liveData.observe(viewLifecycleOwner, observer)
                    liveData.observeForever(observer)
                } else {
                    val snackbar = Snackbar.make(
                        view,
                        "Usa controllo normale (bottone sopra)",
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.anchorView =
                        activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                    snackbar.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val activity = requireActivity()

        if (activity is MainActivity) {
            //Update notification
            val notificationLayout = view?.findViewById<LinearLayout>(R.id.notificationLayout)
            if (notificationLayout?.visibility == View.VISIBLE) {
                val notifyStatusText = view?.findViewById<TextView>(R.id.notificationText)
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    notifyStatusText?.text = "Negato"
                } else {
                    notifyStatusText?.text = "Concesso"
                }
            }

            //Folder permission
            val selectedFolderText = view?.findViewById<TextView>(R.id.selectedFolderText)
            if (activity.contentResolver.persistedUriPermissions.size > 0) {
                selectedFolderText?.text = DocumentFile.fromTreeUri(
                    activity,
                    activity.contentResolver.persistedUriPermissions[0].uri
                )!!.name
            } else {
                view?.findViewById<LinearLayout>(R.id.folderLayout)?.visibility = View.VISIBLE
                view?.findViewById<LinearLayout>(R.id.home)?.visibility = View.GONE
            }
        }
        updateOccupiedSpace()
    }

    private fun updateOccupiedSpace() {
        val activity = requireActivity()

        if (activity is MainActivity) {
            //Occupied space
            val occupiedSpaceTxt = view?.findViewById<TextView>(R.id.occupiedSpaceTxt)
            if (activity.contentResolver.persistedUriPermissions.size > 0) {
                val directory = DocumentFile.fromTreeUri(
                    activity,
                    activity.contentResolver.persistedUriPermissions[0].uri
                )
                var size = 0.0
                for (file in directory!!.listFiles()) {
                    size += file.length() / 1e+6
                }

                occupiedSpaceTxt?.text = (Math.round(size * 10.0) / 10.0).toString() + "MB"
            }
        }
    }
}