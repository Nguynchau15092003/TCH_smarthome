package com.example.smarthomevoice

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.FragmentCreateEventBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CreateEventFragment : Fragment() {
    private val TAG = "CreateEventFragment"
    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Calendar? = null
    private var startTime: Calendar? = null
    private var endTime: Calendar? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardEvent.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.cardEvent.radius = 24f

        binding.tvEventDate.setOnClickListener {
            pickDate()
        }

        binding.tvStartTime.setOnClickListener {
            pickTime(isStart = true)
        }

        binding.tvEndTime.setOnClickListener {
            pickTime(isStart = false)
        }

        // Add new button for Google Calendar integration
        binding.btnSaveEvent.text = "SAVE TO GOOGLE CALENDAR"

        binding.btnSaveEvent.setOnClickListener {
            val useGoogleApi = true // Toggle between native intent (false) or Google API (true)

            if (useGoogleApi) {
                coroutineScope.launch {
                    saveEventToGoogleCalendar()
                }
            } else {
                saveEventToCalendarViaIntent()
            }
        }
    }

    private fun pickDate() {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selected = Calendar.getInstance()
            selected.set(year, month, day)
            selectedDate = selected

            val formatted = String.format("%02d/%02d/%04d", day, month + 1, year)
            binding.tvEventDate.text = formatted
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime(isStart: Boolean) {
        val now = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val time = Calendar.getInstance()
            if (selectedDate != null) {
                time.set(
                    selectedDate!!.get(Calendar.YEAR),
                    selectedDate!!.get(Calendar.MONTH),
                    selectedDate!!.get(Calendar.DAY_OF_MONTH),
                    hour,
                    minute,
                    0
                )
            } else {
                time.set(Calendar.HOUR_OF_DAY, hour)
                time.set(Calendar.MINUTE, minute)
                time.set(Calendar.SECOND, 0)
            }

            val formatted = String.format("%02d:%02d", hour, minute)
            if (isStart) {
                startTime = time
                binding.tvStartTime.text = "Start: $formatted"
            } else {
                endTime = time
                binding.tvEndTime.text = "End: $formatted"
            }
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun saveEventToCalendarViaIntent() {
        val title = binding.etEventTitle.text.toString().trim()
        val desc = binding.etEventDescription.text.toString().trim()

        if (!validateInputs(title, desc)) {
            return
        }

        val beginTime = startTime!!.timeInMillis
        val finishTime = endTime!!.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, finishTime)
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private val REQUEST_AUTHORIZATION = 1001

    private suspend fun saveEventToGoogleCalendar() {
        val title = binding.etEventTitle.text.toString().trim()
        val desc = binding.etEventDescription.text.toString().trim()

        if (!validateInputs(title, desc)) {
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

                // Create credential for Calendar API
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), listOf(CalendarScopes.CALENDAR)
                ).apply {
                    selectedAccount = account.account
                }

                // Build Calendar service
                val calendarService = com.google.api.services.calendar.Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("SmartHomeVoice").build()

                // Create start and end time
                val start = EventDateTime()
                    .setDateTime(DateTime(startTime!!.time))
                    .setTimeZone(TimeZone.getDefault().id)

                val end = EventDateTime()
                    .setDateTime(DateTime(endTime!!.time))
                    .setTimeZone(TimeZone.getDefault().id)

                // Create event
                val event = Event()
                    .setSummary(title)
                    .setDescription(desc)
                    .setStart(start)
                    .setEnd(end)

                try {
                    // Insert event to primary calendar
                    val createdEvent = calendarService.events().insert("primary", event).execute()

                    Log.d(TAG, "Event created: ${createdEvent.htmlLink}")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
                        clearInputs()
                    }
                } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                    // This exception occurs when the user needs to grant permissions
                    Log.d(TAG, "Need to request permissions: ${e.message}")
                    withContext(Dispatchers.Main) {
                        // Start the authorization intent
                        startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating event", e)
                val errorMessage = when (e) {
                    is GoogleJsonResponseException -> {
                        when (e.statusCode) {
                            403 -> "Calendar API is not enabled. Please enable it in Google Cloud Console."
                            401 -> "Authentication failed. Please sign in again."
                            else -> "Failed to create event: ${e.message}"
                        }
                    }
                    is com.google.android.gms.auth.UserRecoverableAuthException -> {
                        // Handle UserRecoverableAuthException
                        withContext(Dispatchers.Main) {
                            startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                        }
                        return@withContext
                    }
                    else -> "Failed to create event: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(title: String, desc: String): Boolean {
        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter title and description", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDate == null || startTime == null || endTime == null) {
            Toast.makeText(requireContext(), "Please select date, start time and end time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (startTime!!.after(endTime)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun clearInputs() {
        binding.etEventTitle.text.clear()
        binding.etEventDescription.text.clear()
        binding.tvEventDate.text = "Select Date"
        binding.tvStartTime.text = "Select Start Time"
        binding.tvEndTime.text = "Select End Time"
        selectedDate = null
        startTime = null
        endTime = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_AUTHORIZATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    // User granted permission, retry operation
                    Log.d(TAG, "User granted calendar permissions, retrying operation")
                    coroutineScope.launch {
                        saveEventToGoogleCalendar()
                    }
                } else {
                    // User denied permission
                    Log.d(TAG, "User denied calendar permissions")
                    Toast.makeText(
                        requireContext(),
                        "Calendar permissions are required to create events",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}