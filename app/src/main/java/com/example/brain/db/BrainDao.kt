package com.example.brain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.brain.model.BrainAuditLog
import com.example.brain.model.BrainUserSettings
import com.example.brain.model.MemoryItem
import com.example.brain.model.MemoryRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainDao {

    // --- Memories ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(items: List<MemoryItem>)

    @Update
    suspend fun updateMemory(item: MemoryItem)

    @Query("SELECT * FROM brain_memories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getMemoryById(id: String, userId: String): MemoryItem?

    @Query("SELECT * FROM brain_memories WHERE userId = :userId AND key = :key AND isActive = 1 LIMIT 1")
    suspend fun getMemoryByKey(userId: String, key: String): MemoryItem?

    @Query("SELECT * FROM brain_memories WHERE userId = :userId AND isActive = 1 ORDER BY importance DESC, updatedAt DESC")
    fun getAllActiveMemoriesFlow(userId: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM brain_memories WHERE userId = :userId AND isActive = 1 ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAllActiveMemories(userId: String): List<MemoryItem>

    @Query("SELECT * FROM brain_memories WHERE userId = :userId AND category = :category AND isActive = 1 ORDER BY importance DESC, updatedAt DESC")
    fun getMemoriesByCategoryFlow(userId: String, category: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM brain_memories WHERE userId = :userId AND category = :category AND isActive = 1 ORDER BY importance DESC, updatedAt DESC")
    suspend fun getMemoriesByCategory(userId: String, category: String): List<MemoryItem>

    @Query("""
        SELECT * FROM brain_memories 
        WHERE userId = :userId 
          AND isActive = 1 
          AND (key LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY importance DESC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchMemories(userId: String, query: String, limit: Int = 15): List<MemoryItem>

    @Query("DELETE FROM brain_memories WHERE id = :id AND userId = :userId")
    suspend fun deleteMemoryById(id: String, userId: String)

    @Query("DELETE FROM brain_memories WHERE userId = :userId AND category = :category")
    suspend fun deleteCategory(userId: String, category: String)

    @Query("DELETE FROM brain_memories WHERE userId = :userId")
    suspend fun clearAllMemories(userId: String)

    @Query("SELECT COUNT(*) FROM brain_memories WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM brain_memories WHERE userId = :userId")
    suspend fun getTotalCount(userId: String): Int

    // --- Relations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: MemoryRelation)

    @Query("SELECT toMemoryId FROM brain_relations WHERE fromMemoryId = :memoryId")
    suspend fun getRelatedMemoryIds(memoryId: String): List<String>

    @Query("DELETE FROM brain_relations WHERE fromMemoryId = :memoryId OR toMemoryId = :memoryId")
    suspend fun deleteRelationsForMemory(memoryId: String)

    // --- Audit Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: BrainAuditLog)

    @Query("SELECT * FROM brain_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 50): Flow<List<BrainAuditLog>>

    // --- User Settings ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: BrainUserSettings)

    @Query("SELECT * FROM brain_user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettings(userId: String): BrainUserSettings?
}
