package com.example.smarthomevoice

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.FragmentCreateEventBinding
import java.util.*

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Calendar? = null
    private var startTime: Calendar? = null
    private var endTime: Calendar? = null

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

        binding.btnSaveEvent.setOnClickListener {
            saveEventToCalendar()
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

    private fun saveEventToCalendar() {
        val title = binding.etEventTitle.text.toString().trim()
        val desc = binding.etEventDescription.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter title and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate == null || startTime == null || endTime == null) {
            Toast.makeText(requireContext(), "Please select date, start time and end time", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime!!.after(endTime)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
