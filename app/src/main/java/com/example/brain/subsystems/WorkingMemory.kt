package com.example.brain.subsystems

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkingContext(
    val currentRequest: String = "",
    val currentTopic: String = "",
    val activeGoal: String = "",
    val activeEntities: List<String> = emptyList(),
    val recentToolResults: List<String> = emptyList(),
    val lastAction: String = "",
    val lastActionSuccess: Boolean? = null,
    val conversationState: String = "ACTIVE",
    val updatedAt: Long = System.currentTimeMillis()
)

object WorkingMemory {

    private val _context = MutableStateFlow(WorkingContext())
    val context: StateFlow<WorkingContext> = _context.asStateFlow()

    fun updateRequest(request: String, topic: String = "") {
        _context.value = _context.value.copy(
            currentRequest = request,
            currentTopic = topic.ifBlank { detectTopic(request) },
            updatedAt = System.currentTimeMillis()
        )
    }

    fun setGoal(goal: String) {
        _context.value = _context.value.copy(
            activeGoal = goal,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun recordToolResult(toolName: String, result: String, success: Boolean) {
        val updatedResults = (_context.value.recentToolResults + "$toolName -> $result").takeLast(5)
        _context.value = _context.value.copy(
            recentToolResults = updatedResults,
            lastAction = toolName,
            lastActionSuccess = success,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun clear() {
        _context.value = WorkingContext()
    }

    private fun detectTopic(request: String): String {
        val lower = request.lowercase()
        return when {
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("call") || lower.contains("phone") -> "Phone Call"
            lower.contains("sms") || lower.contains("message") -> "SMS"
            lower.contains("gaana") || lower.contains("song") || lower.contains("music") -> "Music & Media"
            lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("volume") -> "System Settings"
            lower.contains("weather") || lower.contains("mausam") -> "Weather"
            lower.contains("website") || lower.contains("code") -> "Web Coding"
            lower.contains("screen") || lower.contains("tap") -> "Screen Automation"
            else -> "General Conversation"
        }
    }
}
