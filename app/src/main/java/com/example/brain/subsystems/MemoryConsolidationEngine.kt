package com.example.brain.subsystems

import com.example.brain.db.BrainDao
import com.example.brain.model.AuditAction
import com.example.brain.model.BrainAuditLog
import com.example.brain.model.MemoryItem

object MemoryConsolidationEngine {

    suspend fun consolidateOrInsert(
        dao: BrainDao,
        userId: String,
        newItem: MemoryItem
    ): MemoryItem {
        val existing = dao.getMemoryByKey(userId, newItem.key)
        return if (existing != null) {
            // Update existing memory with latest information
            val updated = existing.copy(
                content = newItem.content,
                why = newItem.why,
                confidence = newItem.confidence,
                source = newItem.source,
                importance = maxOf(existing.importance, newItem.importance),
                strength = newItem.strength,
                exceptionContext = newItem.exceptionContext ?: existing.exceptionContext,
                tags = if (newItem.tags.isNotBlank()) "${existing.tags}, ${newItem.tags}" else existing.tags,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateMemory(updated)
            dao.insertAuditLog(
                BrainAuditLog(
                    memoryId = updated.id,
                    action = AuditAction.UPDATED.name,
                    details = "Consolidated/Updated memory key '${updated.key}'"
                )
            )
            updated
        } else {
            dao.insertMemory(newItem)
            dao.insertAuditLog(
                BrainAuditLog(
                    memoryId = newItem.id,
                    action = AuditAction.CREATED.name,
                    details = "Stored new memory: '${newItem.key}' [${newItem.category}]"
                )
            )
            newItem
        }
    }
}
