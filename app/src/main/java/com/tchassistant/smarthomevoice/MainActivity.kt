package com.tchassistant.smarthomevoice

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tchassistant.smarthomevoice.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "MainActivity"
    private val voiceApiManager = VoiceApiManager()
    private val dateTimeParser = DateTimeParser()
    private lateinit var loadingOverlay: LoadingOverlay

    // Speech recognition launcher
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                Log.d(TAG, "Recognized speech: $spokenText")
                lifecycleScope.launch {
                    processVoiceCommand(spokenText)
                }
            }
        } else {
            Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Initializing app")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize loading overlay
        loadingOverlay = LoadingOverlay(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Request permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_RECORD_AUDIO
        )

        // Gán topAppBar làm ActionBar
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = "TCH Assistant"

        // Làm cho AppBar trong suốt và không bóng
        binding.topAppBar.setBackgroundColor(Color.TRANSPARENT)
        binding.topAppBar.elevation = 0f

        // Voice button click listener
        binding.fabVoice.setOnClickListener {
            startSpeechToText()
        }

        // Xử lý menu item click
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    signOut()
                    true
                }
                else -> false
            }
        }

        // Fragment mặc định
        replaceFragment(RemoteFragment())

        // Xử lý chuyển fragment qua bottom nav
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_remote -> {
                    replaceFragment(RemoteFragment())
                    true
                }
                R.id.nav_note -> {
                    replaceFragment(TakeNoteFragment())
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Make this function public so it can be called from fragments if needed
    fun startSpeechToText() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command for your smart home")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            Toast.makeText(this, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun processVoiceCommand(text: String) {
        loadingOverlay.show("Processing voice command...")
        try {
            showToast("Processing: $text")

            val result = voiceApiManager.getPrediction(text)

            if (result == null) {
                showToast("Could not understand the command")
                return
            }

            Log.d(TAG, "Prediction result: ${result.modelUsed} with predictions: ${result.wordPredictions}")

            // Extract entities from word predictions
            val entityMap = voiceApiManager.extractEntities(result.wordPredictions, result.modelUsed)
            Log.d(TAG, "Extracted entities: $entityMap")

            when (result.modelUsed) {
                "DEVICE" -> handleDeviceControlIntent(entityMap)
                "EVENT" -> handleEventCreationIntent(entityMap)
                "TASK" -> handleTaskCreationIntent(entityMap)
                else -> showToast("Unknown command type: ${result.modelUsed}")
            }
        } finally {
            loadingOverlay.hide()
        }
    }

    private fun handleDeviceControlIntent(entityMap: Map<String, String>) {
        val device = entityMap["devicename"]?.lowercase()
        val status = entityMap["devicestatus"]?.lowercase()

        Log.d(TAG, "Device control: Device=$device, Status=$status")

        if (device == "room") {
            // Special case for room selection
            handleRoomSelectionIntent(mapOf("room" to (entityMap["devicename"] ?: "")))
            return
        }

        if (!device.isNullOrEmpty() && !status.isNullOrEmpty()) {
            // Find the current RemoteFragment and update device state
            val remoteFragment = supportFragmentManager.fragments.find { it is RemoteFragment } as? RemoteFragment
            remoteFragment?.let { fragment ->
                when {
                    (device == "door" || device == "curtain") && (status == "open" || status.contains("open")) ->
                        fragment.updateDeviceState(device, true)
                    (device == "door" || device == "curtain") && (status == "close" || status.contains("close")) ->
                        fragment.updateDeviceState(device, false)
                    status == "on" || status.contains("on") -> fragment.updateDeviceState(device, true)
                    status == "off" || status.contains("off") -> fragment.updateDeviceState(device, false)
                    else -> showToast("Unknown status: $status for $device")
                }
            } ?: showToast("Remote control not available")
        } else {
            showToast("Device or status not recognized")
        }
    }

    private fun handleRoomSelectionIntent(entityMap: Map<String, String>) {
        val room = entityMap["room"]

        Log.d(TAG, "Room selection: Room=$room")

        if (!room.isNullOrEmpty()) {
            val remoteFragment = supportFragmentManager.fragments.find { it is RemoteFragment } as? RemoteFragment
            remoteFragment?.let { fragment ->
                fragment.selectRoom(room)
            } ?: showToast("Remote control not available")
        } else {
            showToast("Room not recognized")
        }
    }

    private fun handleEventCreationIntent(entityMap: Map<String, String>) {
        val time = entityMap["time"]
        val endTime = entityMap["endtime"]
        val location = entityMap["location"]
        val date = entityMap["date"]

        if (time != null && date != null) {
            // Parse date and time
            val parsedDate = dateTimeParser.parseDate(date)
            val parsedStartTime = time.let { dateTimeParser.parseTime(it) }

            if (parsedDate != null && parsedStartTime != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Get the user account
                        val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                        if (account == null) {
                            withContext(Dispatchers.Main) {
                                showToast("Please sign in with your Google account")
                            }
                            return@launch
                        }

                        // Create credential for Calendar API
                        val credential = GoogleAccountCredential.usingOAuth2(
                            this@MainActivity, listOf(CalendarScopes.CALENDAR)
                        ).apply {
                            selectedAccount = account.account
                        }

                        // Build Calendar service
                        val calendarService = com.google.api.services.calendar.Calendar.Builder(
                            NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            credential
                        ).setApplicationName("SmartHomeVoice").build()

                        // Create start time
                        val startTime = Calendar.getInstance().apply {
                            set(Calendar.YEAR, parsedDate.get(Calendar.YEAR))
                            set(Calendar.MONTH, parsedDate.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, parsedDate.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, parsedStartTime.first)
                            set(Calendar.MINUTE, parsedStartTime.second)
                        }

                        // Create end time
                        val endTime = if (endTime != null) {
                            // Check if endTime is a duration (e.g., "1 hour")
                            if (endTime.contains("hour", ignoreCase = true) || 
                                endTime.contains("min", ignoreCase = true)) {
                                val duration = endTime.split(" ")[0].toIntOrNull() ?: 1
                                val isHours = endTime.contains("hour", ignoreCase = true)
                                
                                startTime.clone() as Calendar
                                if (isHours) {
                                    startTime.add(Calendar.HOUR, duration)
                                } else {
                                    startTime.add(Calendar.MINUTE, duration)
                                }
                                startTime
                            } else {
                                // Parse as a specific time
                                val parsedEndTime = dateTimeParser.parseTime(endTime)
                                if (parsedEndTime != null) {
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, parsedDate.get(Calendar.YEAR))
                                        set(Calendar.MONTH, parsedDate.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, parsedDate.get(Calendar.DAY_OF_MONTH))
                                        set(Calendar.HOUR_OF_DAY, parsedEndTime.first)
                                        set(Calendar.MINUTE, parsedEndTime.second)
                                    }
                                } else {
                                    // Default to 1 hour if parsing fails
                                    startTime.clone() as Calendar
                                    startTime.add(Calendar.HOUR, 1)
                                    startTime
                                }
                            }
                        } else {
                            // Default to 1 hour if no end time specified
                            startTime.clone() as Calendar
                            startTime.add(Calendar.HOUR, 1)
                            startTime
                        }

                        val start = EventDateTime()
                            .setDateTime(DateTime(startTime.time))
                            .setTimeZone(TimeZone.getDefault().id)

                        val end = EventDateTime()
                            .setDateTime(DateTime(endTime.time))
                            .setTimeZone(TimeZone.getDefault().id)

                        // Create the event
                        val event = Event()
                            .setSummary(location)
                            .setStart(start)
                            .setEnd(end)

                        val createdEvent = calendarService.events()
                            .insert("primary", event)
                            .execute()

                        withContext(Dispatchers.Main) {
                            showToast("Event created successfully!")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating event", e)
                        withContext(Dispatchers.Main) {
                            when (e) {
                                is GoogleJsonResponseException -> {
                                    when (e.statusCode) {
                                        403 -> showToast("Calendar API is not enabled. Please enable it in Google Cloud Console.")
                                        401 -> showToast("Authentication failed. Please sign in again.")
                                        else -> showToast("Failed to create event: ${e.message}")
                                    }
                                }
                                is com.google.android.gms.auth.UserRecoverableAuthException -> {
                                    e.intent?.let { intent ->
                                        startActivityForResult(intent, REQUEST_AUTHORIZATION)
                                    }
                                }
                                else -> showToast("Failed to create event: ${e.message}")
                            }
                        }
                    }
                }
            } else {
                showToast("Could not parse date or time")
            }
        } else {
            showToast("Missing required event information")
        }
    }

    private fun handleTaskCreationIntent(entityMap: Map<String, String>) {
        val taskName = entityMap["taskname"]
        val date = entityMap["date"]

        if (taskName != null && date != null) {
            // Parse date
            val parsedDate = dateTimeParser.parseDate(date)

            if (parsedDate != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                        if (account == null) {
                            withContext(Dispatchers.Main) {
                                showToast("Please sign in with your Google account")
                            }
                            return@launch
                        }

                        val credential = GoogleAccountCredential.usingOAuth2(
                            this@MainActivity, listOf(TasksScopes.TASKS)
                        ).apply {
                            selectedAccount = account.account
                        }

                        val tasksService = com.google.api.services.tasks.Tasks.Builder(
                            NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            credential
                        ).setApplicationName("SmartHomeVoice").build()

                        // Get default task list
                        val taskLists = tasksService.tasklists().list().execute()
                        val defaultTaskList = taskLists.items.find { it.title == "@default" }
                            ?: taskLists.items.firstOrNull()
                            ?: throw Exception("No task lists found")

                        // Create task
                        val dueDate = DateTime(parsedDate.time).toStringRfc3339()
                        val task = Task()
                            .setTitle(taskName)
                            .setNotes("Created via voice command")
                            .setDue(dueDate)
                            .setStatus("needsAction")

                        val createdTask = tasksService.tasks()
                            .insert(defaultTaskList.id, task)
                            .execute()

                        withContext(Dispatchers.Main) {
                            showToast("Task created successfully!")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating task", e)
                        withContext(Dispatchers.Main) {
                            when (e) {
                                is GoogleJsonResponseException -> {
                                    when (e.statusCode) {
                                        403 -> showToast("Tasks API is not enabled. Please enable it in Google Cloud Console.")
                                        401 -> showToast("Authentication failed. Please sign in again.")
                                        else -> showToast("Failed to create task: ${e.message}")
                                    }
                                }
                                is com.google.android.gms.auth.UserRecoverableAuthException -> {
                                    e.intent?.let { intent ->
                                        startActivityForResult(intent, REQUEST_AUTHORIZATION)
                                    }
                                }
                                else -> showToast("Failed to create task: ${e.message}")
                            }
                        }
                    }
                }
            } else {
                showToast("Could not parse date")
            }
        } else {
            showToast("Missing required task information")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
        private const val REQUEST_AUTHORIZATION = 1002
    }
}