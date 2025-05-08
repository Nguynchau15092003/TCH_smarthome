package com.example.smarthomevoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.FragmentCreateTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateTaskFragment : Fragment() {

    private var _binding: FragmentCreateTaskBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()

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

        // Bắt sự kiện chọn ngày
        binding.etTaskDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    updateDateField()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        binding.btnSaveTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val notes = binding.etTaskNotes.text.toString().trim()
            val date = binding.etTaskDate.text.toString().trim()
            val completed = binding.cbTaskCompleted.isChecked

            if (title.isEmpty() || notes.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Đã lưu công việc: $title\nNgày: $date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDateField() {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etTaskDate.setText(format.format(calendar.time))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
