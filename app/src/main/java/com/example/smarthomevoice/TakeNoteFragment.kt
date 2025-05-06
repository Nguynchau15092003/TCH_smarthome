package com.example.smarthomevoice

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.ActivityTakeNoteBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TakeNoteFragment : Fragment() {

    private val TAG = "TakeNoteFragment"
    private var _binding: ActivityTakeNoteBinding? = null
    private val binding get() = _binding!!
    private var selectedDateTime: Calendar? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTakeNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update button text to match functionality
        binding.btnSaveNote.text = "SAVE TO GOOGLE TASKS"

        // Select date and time
        binding.tvSelectedDateTime.setOnClickListener {
            showDateTimePicker()
        }

        // Save note to tasks
        binding.btnSaveNote.setOnClickListener {
            coroutineScope.launch {
                saveNoteToGoogleTasks()
            }
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(Calendar.YEAR, y)
            newCalendar.set(Calendar.MONTH, m)
            newCalendar.set(Calendar.DAY_OF_MONTH, d)

            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), { _, h, min ->
                newCalendar.set(Calendar.HOUR_OF_DAY, h)
                newCalendar.set(Calendar.MINUTE, min)
                newCalendar.set(Calendar.SECOND, 0)

                selectedDateTime = newCalendar

                val formatted = String.format(
                    "%02d/%02d/%04d - %02d:%02d",
                    d, m + 1, y, h, min
                )
                binding.tvSelectedDateTime.text = "Selected: $formatted"

            }, hour, minute, true).show()

        }, year, month, day).show()
    }

    private suspend fun saveNoteToGoogleTasks() {
        val noteTitle = binding.etNoteTitle.text.toString().trim()
        val noteDescription = binding.etNoteDescription.text.toString().trim()

        // Validate inputs
        if (noteTitle.isEmpty() || noteDescription.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both title and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDateTime == null) {
            Toast.makeText(requireContext(), "Please select a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                // Get the user account
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Please sign in with your Google account", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                // Create credential for Tasks API
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), listOf(TasksScopes.TASKS)
                ).apply {
                    selectedAccount = account.account
                }

                // Build Tasks service
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

                Log.d(TAG, "Using task list: ${defaultTaskList.title} (${defaultTaskList.id})")

                // Format the date for the task
                val dueDate = DateTime(selectedDateTime!!.time).toStringRfc3339()

                // Create a task
                val task = Task()
                    .setTitle(noteTitle)
                    .setNotes(noteDescription)
                    .setDue(dueDate)
                    .setStatus("needsAction")

                // Insert the task into the default task list
                val createdTask = tasksService.tasks()
                    .insert(defaultTaskList.id, task)
                    .execute()

                Log.d(TAG, "Task created successfully: ${createdTask.id}")

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Task created successfully!", Toast.LENGTH_SHORT).show()
                    // Clear inputs after successful creation
                    binding.etNoteTitle.text.clear()
                    binding.etNoteDescription.text.clear()
                    binding.tvSelectedDateTime.text = "Select Date and Time"
                    selectedDateTime = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating task", e)
                val errorMessage = when (e) {
                    is GoogleJsonResponseException -> {
                        when (e.statusCode) {
                            403 -> "Tasks API is not enabled. Please enable it in Google Cloud Console."
                            401 -> "Authentication failed. Please sign in again."
                            else -> "Failed to create task: ${e.message}"
                        }
                    }
                    else -> "Failed to create task: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}