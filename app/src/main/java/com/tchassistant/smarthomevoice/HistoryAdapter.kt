package com.tchassistant.smarthomevoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tchassistant.smarthomevoice.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private var items: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Track current day for headers
    private var lastDateString = ""

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            // Set type icon and title
            val typeIcon = when (item.type) {
                HistoryItem.ItemType.EVENT -> "📅"
                HistoryItem.ItemType.TASK -> "✓"
            }

            // Format the date
            val isDateOnly = isDateOnlyFormat(item.date)
            val formattedDate = if (isDateOnly) {
                dateOnlyFormat.format(item.date)
            } else {
                timeOnlyFormat.format(item.date)
            }

            // Set the date header visibility
            val currentDateString = getDayHeader(item.date)
            if (adapterPosition > 0 && currentDateString == lastDateString) {
                binding.layoutDateHeader.visibility = ViewGroup.GONE
            } else {
                binding.layoutDateHeader.visibility = ViewGroup.VISIBLE
                binding.tvDateHeader.text = currentDateString
                lastDateString = currentDateString
            }

            // Set completion status
            val completionStatus = if (item.type == HistoryItem.ItemType.TASK) {
                if (item.isCompleted) {
                    binding.tvItemStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                    "✅ Completed"
                } else {
                    binding.tvItemStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_orange_dark))
                    "⏳ Pending"
                }
            } else {
                binding.tvItemStatus.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))
                ""
            }

            // Update UI elements
            binding.tvItemText.text = "$typeIcon ${item.title}"
            binding.tvItemDescription.text = item.description.takeIf { it.isNotBlank() } ?: "No description"
            binding.tvItemDate.text = formattedDate
            binding.tvItemStatus.text = completionStatus

            // Set card background color based on type
            val cardBackgroundColor = when {
                item.type == HistoryItem.ItemType.EVENT ->
                    binding.root.context.getColor(android.R.color.holo_blue_light)
                item.type == HistoryItem.ItemType.TASK && item.isCompleted ->
                    binding.root.context.getColor(android.R.color.holo_green_light)
                else ->
                    binding.root.context.getColor(android.R.color.holo_orange_light)
            }
            binding.cardBackground.setCardBackgroundColor(cardBackgroundColor)

            // Hide description if it's empty
            if (item.description.isBlank()) {
                binding.tvItemDescription.visibility = ViewGroup.GONE
            } else {
                binding.tvItemDescription.visibility = ViewGroup.VISIBLE
            }
        }
    }

    private fun isDateOnlyFormat(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.HOUR_OF_DAY) == 0 &&
                calendar.get(Calendar.MINUTE) == 0 &&
                calendar.get(Calendar.SECOND) == 0
    }

    private fun getDayHeader(date: Date): String {
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val itemDate = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(today, itemDate) -> "Today"
            isSameDay(tomorrow, itemDate) -> "Tomorrow"
            else -> {
                val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                dateFormat.format(date)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])

        // Reset day tracking when binding the first item
        if (position == 0) {
            lastDateString = ""
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
        // Reset day tracking when updating items
        lastDateString = ""
    }
}