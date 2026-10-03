package com.example.brain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "brain_memories",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["key"]),
        Index(value = ["scope"]),
        Index(value = ["importance"])
    ]
)
data class MemoryItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "default_user",
    val category: String = MemoryCategory.PREFERENCES.name,
    val key: String,
    val content: String,
    val why: String = "Explicitly provided by user",
    val confidence: String = MemoryConfidence.HIGH.name,
    val source: String = MemorySource.EXPLICIT_USER.name,
    val importance: Int = 5, // 0 to 10
    val scope: String = MemoryScope.GLOBAL.name,
    val strength: String = PreferenceStrength.NORMAL.name,
    val exceptionContext: String? = null,
    val tags: String = "",
    val isPinned: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val validUntil: Long? = null
)

@Entity(
    tableName = "brain_relations",
    indices = [
        Index(value = ["fromMemoryId"]),
        Index(value = ["toMemoryId"])
    ]
)
data class MemoryRelation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromMemoryId: String,
    val toMemoryId: String,
    val relationType: String = "ASSOCIATED_WITH", // DEPENDS_ON, ASSOCIATED_WITH, CAUSES, CONTRADICTS
    val strength: Float = 1.0f
)

@Entity(
    tableName = "brain_audit_logs",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class BrainAuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoryId: String? = null,
    val action: String = AuditAction.CREATED.name,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "brain_user_settings"
)
data class BrainUserSettings(
    @PrimaryKey
    val userId: String = "default_user",
    val isMemoryPaused: Boolean = false,
    val allowInference: Boolean = true,
    val autoConsolidate: Boolean = true,
    val maxRetrievedContext: Int = 12,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
