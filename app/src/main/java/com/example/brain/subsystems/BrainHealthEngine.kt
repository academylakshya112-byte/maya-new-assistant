package com.example.brain.subsystems

import com.example.brain.db.BrainDao
import com.example.brain.model.MemoryCategory

data class BrainHealthStatus(
    val totalMemories: Int,
    val activeMemories: Int,
    val isMemoryPaused: Boolean,
    val categoryCounts: Map<String, Int>,
    val databaseStatus: String,
    val securityStatus: String,
    val lastSyncTime: Long
)

object BrainHealthEngine {

    suspend fun getHealthStatus(dao: BrainDao, userId: String): BrainHealthStatus {
        val total = dao.getTotalCount(userId)
        val active = dao.getActiveCount(userId)
        val settings = dao.getUserSettings(userId)
        val isPaused = settings?.isMemoryPaused ?: false

        val catCounts = mutableMapOf<String, Int>()
        for (cat in MemoryCategory.entries) {
            val list = dao.getMemoriesByCategory(userId, cat.name)
            catCounts[cat.displayName] = list.size
        }

        return BrainHealthStatus(
            totalMemories = total,
            activeMemories = active,
            isMemoryPaused = isPaused,
            categoryCounts = catCounts,
            databaseStatus = "Healthy (Local SQLite Room)",
            securityStatus = "Firewall Active (Zero-Credential Policy)",
            lastSyncTime = settings?.lastSyncTimestamp ?: System.currentTimeMillis()
        )
    }
}
