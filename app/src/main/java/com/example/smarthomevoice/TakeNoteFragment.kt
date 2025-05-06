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
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.ActivityTakeNoteBinding
import java.util.*

class TakeNoteFragment : Fragment() {

    private var _binding: ActivityTakeNoteBinding? = null
    private val binding get() = _binding!!
    private var selectedDateTime: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTakeNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chọn ngày giờ
        binding.tvSelectedDateTime.setOnClickListener {
            showDateTimePicker()
        }

        // Lưu ghi chú vào lịch
        binding.btnSaveNote.setOnClickListener {
            saveNoteToCalendar()
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

    private fun saveNoteToCalendar() {
        val noteTitle = binding.etNoteTitle.text.toString().trim()
        val noteDescription = binding.etNoteDescription.text.toString().trim()

        if (noteTitle.isEmpty() || noteDescription.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both title and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDateTime == null) {
            Toast.makeText(requireContext(), "Please select a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val beginTime = selectedDateTime!!.timeInMillis
        val endTime = beginTime + 60 * 60 * 1000

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(CalendarContract.Events.TITLE, noteTitle)
            putExtra(CalendarContract.Events.DESCRIPTION, noteDescription)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No calendar app found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
