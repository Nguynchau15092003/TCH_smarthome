package com.tchassistant.smarthomevoice

import java.util.Date

data class HistoryItem(
    val id: String,
    val title: String,
    val description: String,
    val date: Date,
    val type: ItemType,
    val isCompleted: Boolean = false
) {
    enum class ItemType {
        EVENT,
        TASK
    }
} 