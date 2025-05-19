package com.tchassistant.smarthomevoice

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tchassistant.smarthomevoice.databinding.FragmentCreateTaskBinding
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
import java.text.SimpleDateFormat
import java.util.*

class CreateTaskFragment : Fragment() {
    private val TAG = "CreateTaskFragment"
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private var selectedDateTime: Calendar? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val REQUEST_AUTHORIZATION = 1002

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardTask.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.cardTask.radius = 24f

        binding.btnSaveTask.text = "SAVE TO GOOGLE TASKS"

        binding.etTaskDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSaveTask.setOnClickListener {
            coroutineScope.launch {
                saveTaskToGoogleTasks()
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                selectedDateTime = calendar.clone() as Calendar
                updateDateField()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateDateField() {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etTaskDate.setText(format.format(calendar.time))
    }

    private suspend fun saveTaskToGoogleTasks() {
        val title = binding.etTaskTitle.text.toString().trim()
        val notes = binding.etTaskNotes.text.toString().trim()
        val date = binding.etTaskDate.text.toString().trim()
        val completed = binding.cbTaskCompleted.isChecked

        if (title.isEmpty() || notes.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter all required information", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDateTime == null) {
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (account == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Please sign in with your Google account", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), listOf(TasksScopes.TASKS)
                ).apply {
                    selectedAccount = account.account
                }

                val tasksService = com.google.api.services.tasks.Tasks.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("SmartHomeVoice").build()

                try {
                    val taskLists = tasksService.tasklists().list().execute()
                    val defaultTaskList = taskLists.items.find { it.title == "@default" }
                        ?: taskLists.items.firstOrNull()
                        ?: throw Exception("No task lists found")

                    Log.d(TAG, "Using task list: ${defaultTaskList.title} (${defaultTaskList.id})")

                    val dueDate = DateTime(selectedDateTime!!.time).toStringRfc3339()

                    val task = Task()
                        .setTitle(title)
                        .setNotes(notes)
                        .setDue(dueDate)
                        .setStatus(if (completed) "completed" else "needsAction")

                    val createdTask = tasksService.tasks()
                        .insert(defaultTaskList.id, task)
                        .execute()

                    Log.d(TAG, "Task created successfully: ${createdTask.id}")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Task created successfully!", Toast.LENGTH_SHORT).show()
                        clearInputs()
                    }
                } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                    Log.d(TAG, "Need to request permissions: ${e.message}")
                    withContext(Dispatchers.Main) {
                        startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                    }
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
                    is com.google.android.gms.auth.UserRecoverableAuthException -> {
                        withContext(Dispatchers.Main) {
                            startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                        }
                        return@withContext
                    }
                    else -> "Failed to create task: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun clearInputs() {
        binding.etTaskTitle.text.clear()
        binding.etTaskNotes.text.clear()
        binding.etTaskDate.text.clear()
        binding.cbTaskCompleted.isChecked = false
        selectedDateTime = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTHORIZATION && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "User granted tasks permissions, retrying operation")
            coroutineScope.launch {
                saveTaskToGoogleTasks()
            }
        } else if (requestCode == REQUEST_AUTHORIZATION) {
            Log.d(TAG, "User denied tasks permissions")
            Toast.makeText(
                requireContext(),
                "Google Tasks permissions are required to create tasks",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}