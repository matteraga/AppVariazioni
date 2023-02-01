package me.matteo.appvariazioni.fragments

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.matteo.appvariazioni.MainActivity
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Utils
import me.matteo.appvariazioni.classes.serializers.SettingsSerializer
import me.matteo.appvariazioni.workers.AutomaticCheckWorker
import me.matteo.appvariazioni.workers.DeleteFilesWorker
import java.util.*
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<me.matteo.appvariazioni.classes.serializers.Settings> by dataStore(
    fileName = "settings.json",
    serializer = SettingsSerializer()
)

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val utils = Utils()

    // Array letter from resources
    private lateinit var letters: Array<String>

    // Local settings object
    private lateinit var settings: me.matteo.appvariazioni.classes.serializers.Settings

    // Views used multiple times
    private lateinit var backgroundSwitch: MaterialSwitch
    private lateinit var timeLayout: LinearLayout
    private lateinit var timeText: TextView
    private lateinit var schoolClassLayout: LinearLayout
    private lateinit var schoolClassText: TextView
    private lateinit var notificationLayout: LinearLayout
    private lateinit var notificationText: TextView
    private lateinit var folderLayout: LinearLayout
    private lateinit var folderText: TextView
    private lateinit var fileText: TextView
    private lateinit var saveFileSwitch: MaterialSwitch
    private lateinit var fileLayout: LinearLayout
    private lateinit var occupiedSpaceText: TextView

    // Occupied space
    private fun updateOccupiedSpace(activity: Activity) {
        try {
            if (activity.contentResolver.persistedUriPermissions.size > 0) {
                val directory = DocumentFile.fromTreeUri(
                    activity,
                    activity.contentResolver.persistedUriPermissions[0].uri
                )
                var size = 0.0
                for (file in directory!!.listFiles()) {
                    if (file.name?.contains(".pdf") == true) {
                        size += file.length() / 1e+6
                    }
                }
                occupiedSpaceText.text = (Math.round(size * 10.0) / 10.0).toString() + " MB"
            } else {
                occupiedSpaceText.text = "0.0 MB"
            }
        } catch (thrown: Throwable) {
            occupiedSpaceText.text = "Errore"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get data flow, if an error occurs create new settings data
        val flow = requireContext().dataStore.data
            .catch {
                emit(me.matteo.appvariazioni.classes.serializers.Settings())
            }

        backgroundSwitch = view.findViewById(R.id.backgroundSwitch)
        timeLayout = view.findViewById(R.id.timeLayout)
        timeText = view.findViewById(R.id.timeText)
        schoolClassLayout = view.findViewById(R.id.schoolClassLayout)
        schoolClassText = view.findViewById(R.id.schoolClassText)
        notificationLayout = view.findViewById(R.id.notificationLayout)
        notificationText = view.findViewById(R.id.notificationText)
        folderLayout = view.findViewById(R.id.folderLayout)
        folderText = view.findViewById(R.id.folderText)
        fileText = view.findViewById(R.id.fileText)
        saveFileSwitch = view.findViewById(R.id.saveFileSwitch)
        fileLayout = view.findViewById(R.id.fileLayout)
        occupiedSpaceText = view.findViewById(R.id.occupiedSpaceText)

        val activity = requireActivity()
        val workManager = WorkManager.getInstance(activity)

        if (!this::letters.isInitialized) {
            // Read the letters from the resources
            letters = resources.getStringArray(R.array.letters)
        }

        // Set ui status as saved
        runBlocking {
            // Get actual settings object
            settings = flow.first()

            backgroundSwitch.isChecked = settings.backgroundCheck

            timeLayout.isVisible = settings.backgroundCheck

            timeText.text =
                settings.hour.toString() + ":" + settings.minute.toString() + if (settings.minute.toString().length == 1) "0" else ""

            schoolClassText.text =
                settings.classNumber.toString() + letters[settings.classLetterPos]

            saveFileSwitch.isChecked = settings.savePDF
        }

        // Background check
        backgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            runBlocking {
                // Update saved value
                activity.dataStore.updateData { it.copy(backgroundCheck = isChecked) }
                // Update current object
                settings = flow.first()
            }
            // Show if true the timelayout
            timeLayout.isVisible = isChecked
            if (isChecked) {
                // If there isn't a worker tagged "automatic" add it, with delay to start at user set time
                if (!utils.isWorkScheduled(workManager, "automatic")) {
                    val currentDate = Calendar.getInstance()
                    val dueDate = Calendar.getInstance()
                    dueDate.set(Calendar.HOUR_OF_DAY, settings.hour)
                    dueDate.set(Calendar.MINUTE, settings.minute)
                    dueDate.set(Calendar.SECOND, 0)
                    if (dueDate.before(currentDate)) {
                        dueDate.add(Calendar.HOUR_OF_DAY, 24)
                    }
                    val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
                    val dailyWorkRequest = OneTimeWorkRequestBuilder<AutomaticCheckWorker>()
                        .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                        .addTag("automatic")
                        .build()
                    workManager.enqueue(dailyWorkRequest)
                }
            } else {
                // If there is a worker tagged "automatic" remove it
                if (utils.isWorkScheduled(workManager, "automatic")) {
                    workManager.cancelAllWorkByTag("automatic")
                }
            }
        }

        // Time picker
        // Listener on layout for better usage
        timeLayout.setOnClickListener {
            val picker =
                MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(settings.hour)
                    .setMinute(settings.minute)
                    .setTitleText("Seleziona quando eseguire il controllo")
                    .setInputMode(INPUT_MODE_CLOCK)
                    .build()

            picker.show(childFragmentManager, "timePicker")

            picker.addOnPositiveButtonClickListener {
                runBlocking {
                    // Update saved value
                    activity.dataStore.updateData {
                        it.copy(
                            hour = picker.hour,
                            minute = picker.minute
                        )
                    }
                    // Update current object and displayed text
                    settings = flow.first()
                    timeText.text =
                        settings.hour.toString() + ":" + settings.minute.toString() + if (settings.minute.toString().length == 1) "0" else ""
                }
            }
        }

        // School class selection dialog
        // Listener on layout for better usage
        schoolClassLayout.setOnClickListener {
            // Inflate dialog custom view
            val dialogCustomView = LayoutInflater.from(activity)
                .inflate(R.layout.school_class_picker_dialog, null, false)
            val numberPicker =
                dialogCustomView.findViewById<View>(R.id.numberPicker) as NumberPicker
            val letterPicker =
                dialogCustomView.findViewById<View>(R.id.letterPicker) as NumberPicker

            // Set numbers value and initial value to saved one
            numberPicker.maxValue = 5
            numberPicker.minValue = 1
            numberPicker.value = settings.classNumber

            // Set letters value and initial value to saved one
            letterPicker.maxValue = letters.size - 1
            letterPicker.minValue = 0
            letterPicker.displayedValues = letters
            letterPicker.value = settings.classLetterPos

            MaterialAlertDialogBuilder(activity)
                .setView(dialogCustomView)
                .setTitle("Seleziona la classe")
                .setPositiveButton("Seleziona") { dialog, _ ->
                    runBlocking {
                        // Update saved value
                        activity.dataStore.updateData {
                            it.copy(
                                classNumber = numberPicker.value,
                                classLetterPos = letterPicker.value
                            )
                        }
                        // Update current object and displayed text
                        settings = flow.first()
                        schoolClassText.text =
                            settings.classNumber.toString() + letters[settings.classLetterPos]
                        // Update bottom nav, not the best way but it works
                        (activity as MainActivity).updateBottomNav(schoolClassText.text.toString())
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Annulla") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // Old school class selection spinner
        /*
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
        */

        // Notification permission
        // Show this only if android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                notificationText.text = "Negato"
            } else {
                notificationText.text = "Concesso"
            }

            // Listener on layout for better usage
            notificationLayout.setOnClickListener {
                // Open app notification settings
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                if (intent.resolveActivity(activity.packageManager) != null) {
                    startActivity(intent)
                }
            }
        } else {
            notificationLayout.visibility = View.GONE
        }

        //Folder creation result
        val folderResult =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    activity.contentResolver.takePersistableUriPermission(
                        it.data!!.data!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    // Remove click listener
                    folderLayout.setOnClickListener(null)
                    // Update folder name
                    folderText.text = DocumentFile.fromTreeUri(
                        activity,
                        activity.contentResolver.persistedUriPermissions[0].uri
                    )!!.name
                    // Show file options
                    fileText.visibility = View.VISIBLE
                    saveFileSwitch.visibility = View.VISIBLE
                    fileLayout.visibility = View.VISIBLE
                }
                return@registerForActivityResult
            }

        // Folder permission
        if (activity.contentResolver.persistedUriPermissions.size > 0) {
            // Get folder name
            folderText.text = DocumentFile.fromTreeUri(
                activity,
                activity.contentResolver.persistedUriPermissions[0].uri
            )!!.name
            // Show file options
            fileText.visibility = View.VISIBLE
            saveFileSwitch.visibility = View.VISIBLE
            fileLayout.visibility = View.VISIBLE
        } else {
            folderText.text = "Nessuna"
            // Remove file options
            fileText.visibility = View.GONE
            saveFileSwitch.visibility = View.GONE
            fileLayout.visibility = View.GONE
            // Set click listener for folder selection menu
            folderLayout.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                folderResult.launch(intent)
            }
        }

        // Save file
        saveFileSwitch.setOnCheckedChangeListener { _, isChecked ->
            runBlocking {
                // Update saved value
                activity.dataStore.updateData { it.copy(backgroundCheck = isChecked) }
                // Update current object
                settings = flow.first()
            }
        }

        updateOccupiedSpace(activity)

        // Delete file
        // Listener on layout for better usage
        fileLayout.setOnClickListener {
            // Dialog for confirmation
            MaterialAlertDialogBuilder(activity)
                .setTitle("Conferma")
                .setMessage(
                    "Sei sicuro di voler eliminare tutti i file PDF in \"" + folderText.text + "\""
                )
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Si") { dialog, _ ->
                    val workRequest = OneTimeWorkRequestBuilder<DeleteFilesWorker>()
                        .build()
                    workManager.enqueueUniqueWork(
                        "delete",
                        ExistingWorkPolicy.KEEP,
                        workRequest
                    )
                    val snackbar = Snackbar.make(
                        view,
                        "Eliminazione in corso...",
                        Snackbar.LENGTH_LONG
                    )
                    // To show the snackbar on top of the navbar
                    snackbar.anchorView =
                        activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                    snackbar.show()
                    // Possibly to remove
                    /*val liveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
                    val observer = object : androidx.lifecycle.Observer<WorkInfo> {
                        override fun onChanged(workInfo: WorkInfo?) {
                            if (workInfo != null) {
                                if (workInfo.state.isFinished) {
                                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                        view.findViewById<TextView>(R.id.occupiedSpaceText).text =
                                            "0.0MB"
                                    }
                                    liveData.removeObserver(this)
                                }
                            }
                        }
                    }
                    liveData.observe(viewLifecycleOwner, observer)*/
                    dialog.dismiss()
                }
                .show()
        }

        // Using refresh layout instead of this
        /*val checkButton = view.findViewById<Button>(R.id.checkButton)
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
        }*/
    }

    override fun onResume() {
        super.onResume()
        try {
            val activity = requireActivity()

            // Update notification permission
            if (notificationLayout.isVisible) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    notificationText.text = "Negato"
                } else {
                    notificationText.text = "Concesso"
                }
            }

            // Update folder text, not complete
            if (activity.contentResolver.persistedUriPermissions.size == 0) {
                folderText.text = "Nessuna"
            }

            // Update file options
            val haveFolderPermission: Boolean =
                activity.contentResolver.persistedUriPermissions.size > 0
            fileText.isVisible = haveFolderPermission
            saveFileSwitch.isVisible = haveFolderPermission
            fileLayout.isVisible = haveFolderPermission

            if (haveFolderPermission) {
                updateOccupiedSpace(activity)
            }
        } catch (_: Throwable) {
        }
    }
}