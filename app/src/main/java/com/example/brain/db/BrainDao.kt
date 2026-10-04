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

    // ==========================================
    // CONTEXT / KNOWLEDGE MAP DAO
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextNode(node: com.example.brain.model.ContextNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextNodes(nodes: List<com.example.brain.model.ContextNode>)

    @Update
    suspend fun updateContextNode(node: com.example.brain.model.ContextNode)

    @Query("SELECT * FROM context_nodes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getContextNodeById(id: String, userId: String): com.example.brain.model.ContextNode?

    @Query("SELECT * FROM context_nodes WHERE userId = :userId AND nodeType = :nodeType AND title = :title LIMIT 1")
    suspend fun findContextNode(userId: String, nodeType: String, title: String): com.example.brain.model.ContextNode?

    @Query("SELECT * FROM context_nodes WHERE userId = :userId ORDER BY importance DESC, updatedAt DESC")
    fun getAllContextNodesFlow(userId: String): Flow<List<com.example.brain.model.ContextNode>>

    @Query("SELECT * FROM context_nodes WHERE userId = :userId ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAllContextNodes(userId: String): List<com.example.brain.model.ContextNode>

    @Query("SELECT * FROM context_nodes WHERE userId = :userId AND nodeType = :nodeType ORDER BY importance DESC, updatedAt DESC")
    suspend fun getContextNodesByType(userId: String, nodeType: String): List<com.example.brain.model.ContextNode>

    @Query("""
        SELECT * FROM context_nodes 
        WHERE userId = :userId 
          AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR propertiesJson LIKE '%' || :query || '%')
        ORDER BY importance DESC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchContextNodes(userId: String, query: String, limit: Int = 15): List<com.example.brain.model.ContextNode>

    @Query("DELETE FROM context_nodes WHERE id = :id AND userId = :userId")
    suspend fun deleteContextNode(id: String, userId: String)

    @Query("SELECT COUNT(*) FROM context_nodes WHERE userId = :userId")
    suspend fun getContextNodeCount(userId: String): Int

    // Context Edges
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextEdge(edge: com.example.brain.model.ContextEdge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContextEdges(edges: List<com.example.brain.model.ContextEdge>)

    @Query("SELECT * FROM context_edges WHERE userId = :userId AND fromNodeId = :fromNodeId")
    suspend fun getOutEdges(userId: String, fromNodeId: String): List<com.example.brain.model.ContextEdge>

    @Query("SELECT * FROM context_edges WHERE userId = :userId AND toNodeId = :toNodeId")
    suspend fun getInEdges(userId: String, toNodeId: String): List<com.example.brain.model.ContextEdge>

    @Query("SELECT * FROM context_edges WHERE userId = :userId AND (fromNodeId = :nodeId OR toNodeId = :nodeId)")
    suspend fun getConnectedEdges(userId: String, nodeId: String): List<com.example.brain.model.ContextEdge>

    @Query("SELECT * FROM context_edges WHERE userId = :userId AND fromNodeId = :fromId AND toNodeId = :toId AND relationshipType = :relType LIMIT 1")
    suspend fun findEdge(userId: String, fromId: String, toId: String, relType: String): com.example.brain.model.ContextEdge?

    @Query("SELECT * FROM context_edges WHERE userId = :userId")
    fun getAllContextEdgesFlow(userId: String): Flow<List<com.example.brain.model.ContextEdge>>

    @Query("SELECT * FROM context_edges WHERE userId = :userId")
    suspend fun getAllContextEdges(userId: String): List<com.example.brain.model.ContextEdge>

    @Query("DELETE FROM context_edges WHERE userId = :userId AND (fromNodeId = :nodeId OR toNodeId = :nodeId)")
    suspend fun deleteEdgesForNode(userId: String, nodeId: String)

    // ==========================================
    // SKILL FORGE DAO
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: com.example.brain.model.SkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<com.example.brain.model.SkillEntity>)

    @Update
    suspend fun updateSkill(skill: com.example.brain.model.SkillEntity)

    @Query("SELECT * FROM skills WHERE skillId = :skillId AND userId = :userId LIMIT 1")
    suspend fun getSkillById(skillId: String, userId: String): com.example.brain.model.SkillEntity?

    @Query("SELECT * FROM skills WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getSkillByName(userId: String, name: String): com.example.brain.model.SkillEntity?

    @Query("SELECT * FROM skills WHERE userId = :userId ORDER BY status DESC, successRate DESC, lastUsedAt DESC")
    fun getAllSkillsFlow(userId: String): Flow<List<com.example.brain.model.SkillEntity>>

    @Query("SELECT * FROM skills WHERE userId = :userId ORDER BY status DESC, successRate DESC, lastUsedAt DESC")
    suspend fun getAllSkills(userId: String): List<com.example.brain.model.SkillEntity>

    @Query("SELECT * FROM skills WHERE userId = :userId AND status = 'VERIFIED' ORDER BY successRate DESC, successCount DESC")
    suspend fun getVerifiedSkills(userId: String): List<com.example.brain.model.SkillEntity>

    @Query("""
        SELECT * FROM skills 
        WHERE userId = :userId 
          AND status != 'DISABLED'
          AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR requiredToolsJson LIKE '%' || :query || '%')
        ORDER BY successRate DESC, lastUsedAt DESC
        LIMIT :limit
    """)
    suspend fun searchSkills(userId: String, query: String, limit: Int = 10): List<com.example.brain.model.SkillEntity>

    @Query("DELETE FROM skills WHERE skillId = :skillId AND userId = :userId")
    suspend fun deleteSkill(skillId: String, userId: String)

    @Query("SELECT COUNT(*) FROM skills WHERE userId = :userId")
    suspend fun getSkillCount(userId: String): Int

    // Skill Execution History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillExecution(execution: com.example.brain.model.SkillExecutionRecord)

    @Query("SELECT * FROM skill_executions WHERE userId = :userId AND skillId = :skillId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getSkillExecutionHistory(userId: String, skillId: String, limit: Int = 20): List<com.example.brain.model.SkillExecutionRecord>

    @Query("SELECT * FROM skill_executions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSkillExecutionsFlow(userId: String, limit: Int = 50): Flow<List<com.example.brain.model.SkillExecutionRecord>>
}
