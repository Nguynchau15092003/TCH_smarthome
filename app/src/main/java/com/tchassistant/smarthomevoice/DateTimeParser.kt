package com.tchassistant.smarthomevoice

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for parsing date and time from natural language text
 */
class DateTimeParser {
    private val TAG = "DateTimeParser"

    /**
     * Attempts to parse a date string into a Calendar object
     */
    fun parseDate(dateText: String): Calendar? {
        val calendar = Calendar.getInstance()

        try {
            // Handle common date formats
            when {
                // Today
                dateText.contains("today", ignoreCase = true) -> {
                    return calendar
                }

                // Tomorrow
                dateText.contains("tomorrow", ignoreCase = true) -> {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    return calendar
                }

                // Day of week (e.g., "Monday", "Tuesday")
                dateText.lowercase().matches(Regex(".*(monday|tuesday|wednesday|thursday|friday|saturday|sunday).*")) -> {
                    val dayOfWeekStr = extractDayOfWeek(dateText.lowercase())
                    val targetDayOfWeek = getDayOfWeekValue(dayOfWeekStr)

                    if (targetDayOfWeek != -1) {
                        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        var daysToAdd = targetDayOfWeek - currentDayOfWeek

                        // If the day has already passed this week, go to next week
                        if (daysToAdd <= 0) {
                            daysToAdd += 7
                        }

                        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                        return calendar
                    }
                }

                // Try to parse formats like "May 19" or "19 May" or "19/05"
                else -> {
                    // Try various date formats
                    val formats = arrayOf(
                        "dd/MM", "MM/dd",
                        "dd-MM", "MM-dd",
                        "d MMMM", "MMMM d",
                        "d MMM", "MMM d",
                        "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"
                    )

                    for (format in formats) {
                        try {
                            val sdf = SimpleDateFormat(format, Locale.getDefault())
                            sdf.isLenient = false

                            val date = sdf.parse(dateText.trim())
                            if (date != null) {
                                val parsedCalendar = Calendar.getInstance()
                                parsedCalendar.time = date

                                // If the format doesn't include year, use current year
                                if (!format.contains("yyyy")) {
                                    parsedCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR))

                                    // If the date has already passed this year, assume next year
                                    if (parsedCalendar.before(calendar)) {
                                        parsedCalendar.add(Calendar.YEAR, 1)
                                    }
                                }

                                return parsedCalendar
                            }
                        } catch (e: Exception) {
                            // Format didn't match, try the next one
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: $dateText", e)
        }

        return null
    }

    /**
     * Attempts to parse a time string into hours and minutes
     * Returns a Pair<Int, Int> representing (hour, minute) in 24-hour format
     */
    fun parseTime(timeText: String): Pair<Int, Int>? {
        try {
            // First try to parse the time directly
            val timeFormats = arrayOf(
                "HH:mm", "h:mm a", "h a", "ha", "H'h'mm", "H'h'",
                "h:mmam", "h:mmam", "hamm", "hamm", // Handle formats without spaces
                "h:mm a.m.", "h:mm p.m.", "h a.m.", "h p.m." // Handle formats with periods
            )

            for (format in timeFormats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    sdf.isLenient = false

                    val date = sdf.parse(timeText.trim())
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        return Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                    }
                } catch (e: Exception) {
                    // Format didn't match, try the next one
                }
            }

            // If direct parsing fails, try to extract numbers and AM/PM
            val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?", RegexOption.IGNORE_CASE)
            val match = timeRegex.find(timeText)
            
            if (match != null) {
                val (hour, minute, ampm) = match.destructured
                var hourInt = hour.toInt()
                val minuteInt = minute.ifEmpty { "0" }.toInt()
                
                // Handle AM/PM
                if (ampm.isNotEmpty()) {
                    val isPM = ampm.startsWith("p", ignoreCase = true)
                    if (isPM && hourInt < 12) hourInt += 12
                    if (!isPM && hourInt == 12) hourInt = 0
                }
                
                return Pair(hourInt, minuteInt)
            }

            // Handle special cases
            when {
                timeText.contains("noon", ignoreCase = true) -> return Pair(12, 0)
                timeText.contains("midnight", ignoreCase = true) -> return Pair(0, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time: $timeText", e)
        }

        return null
    }

    private fun extractDayOfWeek(text: String): String {
        val daysOfWeek = arrayOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        for (day in daysOfWeek) {
            if (text.contains(day)) {
                return day
            }
        }
        return ""
    }

    private fun getDayOfWeekValue(dayOfWeek: String): Int {
        return when (dayOfWeek) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> -1
        }
    }
}