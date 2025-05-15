package com.tchassistant.smarthomevoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tchassistant.smarthomevoice.databinding.FragmentHistoryBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.tasks.TasksScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private val TAG = "HistoryFragment"
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var allItems = mutableListOf<HistoryItem>()
    private var filterType: String = "All" // Default filter

    // Add ActivityResultLauncher for handling auth consent
    private val authConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User granted permission, retry loading data
            loadHistoryItems()
        } else {
            // User denied permission
            showError("Permission denied. Some features may not be available.")
            showLoading(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterSpinner()
        setupRefreshButton()
        loadHistoryItems()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(emptyList())
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All", "Events", "Tasks", "Completed Tasks")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterType = filterOptions[position]
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            loadHistoryItems()
        }
    }

    private fun applyFilter() {
        val filteredItems = when (filterType) {
            "Events" -> allItems.filter { it.type == HistoryItem.ItemType.EVENT }
            "Tasks" -> allItems.filter { it.type == HistoryItem.ItemType.TASK }
            "Completed Tasks" -> allItems.filter { it.type == HistoryItem.ItemType.TASK && it.isCompleted }
            else -> allItems
        }

        adapter.updateItems(filteredItems)

        // Update the counter
        val formattedCount = "${filteredItems.size} item${if (filteredItems.size != 1) "s" else ""}"
        binding.tvItemCount.text = formattedCount
    }

    private fun loadHistoryItems() {
        showLoading(true)
        coroutineScope.launch {
            try {
                allItems.clear()

                // Get events from Google Calendar
                try {
                    val events = fetchCalendarEvents()
                    allItems.addAll(events.map { event ->
                        val eventDate = when {
                            event.start.dateTime != null -> {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = event.start.dateTime.value
                                calendar.time
                            }
                            event.start.date != null -> {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = event.start.date.value
                                calendar.time
                            }
                            else -> Date()
                        }
                        HistoryItem(
                            id = event.id,
                            title = event.summary ?: "Untitled Event",
                            description = event.description ?: "",
                            date = eventDate,
                            type = HistoryItem.ItemType.EVENT
                        )
                    })
                    Log.d(TAG, "Fetched ${events.size} calendar events")
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching calendar events", e)
                    // Handle UserRecoverableAuthIOException specifically
                    if (e is UserRecoverableAuthIOException) {
                        withContext(Dispatchers.Main) {
                            // Launch the consent activity
                            authConsentLauncher.launch(e.intent)
                            return@withContext // Exit the coroutine to prevent showing the loading indicator
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showError("Failed to load calendar events: ${e.message}")
                        }
                    }
                }

                // Get tasks from Google Tasks
                try {
                    val tasks = fetchTasks()
                    allItems.addAll(tasks.map { task ->
                        val taskDate = when {
                            task.due != null -> {
                                try {
                                    // Parse the due date string (RFC 3339 format)
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                                    dateFormat.parse(task.due.toString()) ?: Date()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing task due date: ${task.due}", e)
                                    Date() // Use current date as fallback
                                }
                            }
                            else -> Date()
                        }
                        HistoryItem(
                            id = task.id,
                            title = task.title ?: "Untitled Task",
                            description = task.notes ?: "",
                            date = taskDate,
                            type = HistoryItem.ItemType.TASK,
                            isCompleted = task.status == "completed"
                        )
                    })
                    Log.d(TAG, "Fetched ${tasks.size} tasks")
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching tasks", e)
                    // Handle UserRecoverableAuthIOException specifically
                    if (e is UserRecoverableAuthIOException) {
                        withContext(Dispatchers.Main) {
                            // Launch the consent activity
                            authConsentLauncher.launch(e.intent)
                            return@withContext // Exit the coroutine to prevent showing the loading indicator
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showError("Failed to load tasks: ${e.message}")
                        }
                    }
                }

                // Sort items by date (earliest first)
                allItems.sortWith(compareBy { it.date })

                // Apply current filter
                withContext(Dispatchers.Main) {
                    applyFilter()

                    // Show success message
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val startDate = dateFormat.format(Date())
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, 30)
                    val endDate = dateFormat.format(calendar.time)

                    binding.tvDateRange.text = "Showing data from $startDate to $endDate"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading history items", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load history: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvHistory.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutControls.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private suspend fun fetchCalendarEvents(): List<com.google.api.services.calendar.model.Event> = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            ?: throw Exception("Please sign in with your Google account")

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf(CalendarScopes.CALENDAR_READONLY)
        ).apply {
            selectedAccount = account.account
        }

        val calendarService = com.google.api.services.calendar.Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("SmartHomeVoice").build()

        // Get events for the next 30 days
        val now = DateTime(System.currentTimeMillis())
        val thirtyDaysLater = DateTime(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)

        // First, get the list of calendars
        val calendarList = calendarService.calendarList().list().execute()
        val calendars = calendarList.items ?: emptyList()

        // Fetch events from each calendar
        val allEvents = mutableListOf<com.google.api.services.calendar.model.Event>()
        for (calendar in calendars) {
            try {
                val events = calendarService.events().list(calendar.id)
                    .setTimeMin(now)
                    .setTimeMax(thirtyDaysLater)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setMaxResults(100)  // Limit results per calendar
                    .execute()
                    .items ?: emptyList()
                allEvents.addAll(events)
                Log.d(TAG, "Added ${events.size} events from calendar ${calendar.summary}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching events from calendar ${calendar.id}", e)
                // Rethrow UserRecoverableAuthIOException to handle in the calling function
                if (e is UserRecoverableAuthIOException) {
                    throw e
                }
                // Continue with other calendars even if one fails
            }
        }

        allEvents
    }

    private suspend fun fetchTasks(): List<com.google.api.services.tasks.model.Task> = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            ?: throw Exception("Please sign in with your Google account")

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf(TasksScopes.TASKS_READONLY)
        ).apply {
            selectedAccount = account.account
        }

        val tasksService = com.google.api.services.tasks.Tasks.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("SmartHomeVoice").build()

        // Get all task lists
        val taskLists = tasksService.tasklists().list().execute()
        val allTasks = mutableListOf<com.google.api.services.tasks.model.Task>()

        // Get next 30 days cutoff
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Fetch tasks from each task list
        for (taskList in taskLists.items ?: emptyList()) {
            try {
                val tasks = tasksService.tasks().list(taskList.id)
                    .setShowCompleted(true)
                    .setShowHidden(false)
                    .setMaxResults(100)  // Limit results per task list
                    .execute()
                    .items ?: emptyList()

                // Filter tasks manually for next 30 days
                // (Google Tasks API doesn't support dueMin/dueMax filters as reliably)
                val filteredTasks = tasks.filter { task ->
                    if (task.due == null) {
                        // Include tasks with no due date
                        true
                    } else {
                        try {
                            // Parse the due date and compare
                            val dueDate = dateFormat.parse(task.due.toString())
                            dueDate != null && dueDate.before(calendar.time)
                        } catch (e: Exception) {
                            // If parsing fails, include the task anyway
                            Log.w(TAG, "Error parsing due date: ${task.due}", e)
                            true
                        }
                    }
                }

                allTasks.addAll(filteredTasks)
                Log.d(TAG, "Added ${filteredTasks.size} tasks from task list ${taskList.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tasks from list ${taskList.id}", e)
                // Rethrow UserRecoverableAuthIOException to handle in the calling function
                if (e is UserRecoverableAuthIOException) {
                    throw e
                }
                // Continue with other task lists even if one fails
            }
        }

        allTasks
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}