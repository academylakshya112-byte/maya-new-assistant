package com.example.brain

import android.content.Context
import android.util.Log
import com.example.brain.db.BrainDao
import com.example.brain.db.BrainDatabase
import com.example.brain.model.AuditAction
import com.example.brain.model.BrainAuditLog
import com.example.brain.model.BrainUserSettings
import com.example.brain.model.MemoryCategory
import com.example.brain.model.MemoryConfidence
import com.example.brain.model.MemoryItem
import com.example.brain.model.MemoryScope
import com.example.brain.model.MemorySource
import com.example.brain.model.PreferenceStrength
import com.example.brain.subsystems.BrainHealthEngine
import com.example.brain.subsystems.BrainHealthStatus
import com.example.brain.subsystems.EmotionalStateEngine
import com.example.brain.subsystems.MemoryConsolidationEngine
import com.example.brain.subsystems.MemoryRetrievalEngine
import com.example.brain.subsystems.MemorySecurityEngine
import com.example.brain.subsystems.SelfKnowledgeEngine
import com.example.brain.subsystems.WorkingMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BrainEngine {

    private const val TAG = "BrainEngine"
    private const val CURRENT_USER_ID = "default_user"

    private var database: BrainDatabase? = null
    val dao: BrainDao?
        get() = database?.brainDao()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (database == null) {
            database = BrainDatabase.getInstance(context)
            coroutineScope.launch {
                val d = dao ?: return@launch
                val settings = d.getUserSettings(CURRENT_USER_ID)
                if (settings == null) {
                    d.saveUserSettings(BrainUserSettings(userId = CURRENT_USER_ID))
                } else {
                    _isPaused.value = settings.isMemoryPaused
                }
                seedInitialKnowledge(d)
                com.example.brain.subsystems.ContextMapEngine.seedInitialGraph(d, CURRENT_USER_ID)
                com.example.brain.subsystems.SkillForgeEngine.seedInitialSkills(d, CURRENT_USER_ID)
            }
        }
    }

    private suspend fun seedInitialKnowledge(d: BrainDao) {
        val count = d.getTotalCount(CURRENT_USER_ID)
        if (count == 0) {
            val defaults = listOf(
                MemoryItem(
                    userId = CURRENT_USER_ID,
                    category = MemoryCategory.PERSONAL.name,
                    key = "assistant_identity",
                    content = "Name is strictly Maya. Warm, affectionate, caring AI companion created by SHADOW X RAHUL.",
                    why = "Core identity and creator directive",
                    confidence = MemoryConfidence.HIGH.name,
                    source = MemorySource.SYSTEM.name,
                    importance = 10,
                    strength = PreferenceStrength.EXPLICIT_REQUIRED.name
                ),
                MemoryItem(
                    userId = CURRENT_USER_ID,
                    category = MemoryCategory.PREFERENCES.name,
                    key = "response_style",
                    content = "Fast, natural, crisp responses in Hindi/Hinglish with sweet affection for boss SHADOW X RAHUL.",
                    why = "Default user interaction preference",
                    confidence = MemoryConfidence.HIGH.name,
                    source = MemorySource.SYSTEM.name,
                    importance = 9,
                    strength = PreferenceStrength.STRONG.name
                ),
                MemoryItem(
                    userId = CURRENT_USER_ID,
                    category = MemoryCategory.PROCEDURAL.name,
                    key = "whatsapp_automation_rule",
                    content = "WhatsApp flow must strictly follow 13-step verification; never claim sent without seeing outgoing bubble.",
                    why = "Strict verification reliability rule",
                    confidence = MemoryConfidence.HIGH.name,
                    source = MemorySource.SYSTEM.name,
                    importance = 10,
                    strength = PreferenceStrength.EXPLICIT_REQUIRED.name
                ),
                MemoryItem(
                    userId = CURRENT_USER_ID,
                    category = MemoryCategory.PREFERENCES.name,
                    key = "ui_theme_rule",
                    content = "Do not change existing Maya UI, theme, navigation, or color palette.",
                    why = "Explicit user system constraint",
                    confidence = MemoryConfidence.HIGH.name,
                    source = MemorySource.EXPLICIT_USER.name,
                    importance = 10,
                    strength = PreferenceStrength.EXPLICIT_REQUIRED.name
                )
            )
            d.insertMemories(defaults)
        }
    }

    // ==========================================
    // 1. REMEMBER / STORE
    // ==========================================

    suspend fun remember(
        key: String,
        content: String,
        category: MemoryCategory = MemoryCategory.PREFERENCES,
        why: String = "Explicit user command",
        importance: Int = 6,
        confidence: MemoryConfidence = MemoryConfidence.HIGH,
        source: MemorySource = MemorySource.EXPLICIT_USER,
        strength: PreferenceStrength = PreferenceStrength.NORMAL,
        scope: MemoryScope = MemoryScope.GLOBAL,
        exceptionContext: String? = null,
        tags: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext Pair(false, "Brain database unavailable.")

        if (_isPaused.value) {
            return@withContext Pair(false, "Brain memory is currently paused. Please resume memory to save new information.")
        }

        // 1. Security Firewall Check
        val secResult = MemorySecurityEngine.evaluate(key, content)
        if (!secResult.isSafe) {
            return@withContext Pair(false, "Security Firewall rejected: ${secResult.violationReason}")
        }

        val item = MemoryItem(
            id = UUID.randomUUID().toString(),
            userId = CURRENT_USER_ID,
            category = category.name,
            key = key.trim(),
            content = secResult.sanitizedContent,
            why = why,
            confidence = confidence.name,
            source = source.name,
            importance = importance.coerceIn(0, 10),
            scope = scope.name,
            strength = strength.name,
            exceptionContext = exceptionContext,
            tags = tags,
            updatedAt = System.currentTimeMillis()
        )

        val saved = MemoryConsolidationEngine.consolidateOrInsert(d, CURRENT_USER_ID, item)
        Pair(true, "Yaad rakh liya: '${saved.key}' ✨")
    }

    // ==========================================
    // 2. RECALL & SEARCH
    // ==========================================

    suspend fun recall(query: String, limit: Int = 10): List<MemoryItem> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext emptyList()
        MemoryRetrievalEngine.retrieveRelevant(d, CURRENT_USER_ID, query, limit)
    }

    fun getAllActiveMemoriesFlow(): Flow<List<MemoryItem>>? {
        return dao?.getAllActiveMemoriesFlow(CURRENT_USER_ID)
    }

    fun getMemoriesByCategoryFlow(category: String): Flow<List<MemoryItem>>? {
        return dao?.getMemoriesByCategoryFlow(CURRENT_USER_ID, category)
    }

    // ==========================================
    // 3. BUILD CONTEXT FOR GEMINI PROMPT
    // ==========================================

    suspend fun buildPromptContext(userPrompt: String, maxItems: Int = 8): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext ""
        val relevant = MemoryRetrievalEngine.retrieveRelevant(d, CURRENT_USER_ID, userPrompt, maxItems)

        WorkingMemory.updateRequest(userPrompt)
        EmotionalStateEngine.updateStateFromContext(userPrompt, null)

        val sb = StringBuilder()
        sb.append("\n=== MAYA BRAIN CONTINUITY & ACTIVE MEMORIES ===\n")
        sb.append("Current Emotional Tone: ${EmotionalStateEngine.currentState.value.name}\n")

        val workingCtx = WorkingMemory.context.value
        if (workingCtx.activeGoal.isNotBlank()) {
            sb.append("Active Goal: ${workingCtx.activeGoal}\n")
        }

        if (relevant.isNotEmpty()) {
            sb.append("Relevant Recalled Memories:\n")
            relevant.forEach { mem ->
                sb.append("• [${mem.category}] ${mem.key}: ${mem.content}")
                if (!mem.exceptionContext.isNullOrBlank()) {
                    sb.append(" (Exception: ${mem.exceptionContext})")
                }
                sb.append(" [Strength: ${mem.strength}]\n")
            }
        }

        // --- CONTEXT & KNOWLEDGE MAP RECALL ---
        val contextGraph = com.example.brain.subsystems.ContextMapEngine.recallContextGraph(d, userPrompt, CURRENT_USER_ID)
        if (contextGraph.isNotBlank()) {
            sb.append("\n$contextGraph\n")
        }

        // --- SKILL FORGE VERIFIED WORKFLOWS RECALL ---
        val matchingSkills = com.example.brain.subsystems.SkillForgeEngine.findMatchingSkills(d, userPrompt, CURRENT_USER_ID, limit = 2)
        if (matchingSkills.isNotEmpty()) {
            sb.append("\n=== SKILL FORGE: VERIFIED WORKFLOWS ===\n")
            matchingSkills.forEach { skill ->
                sb.append("• SKILL: ${skill.name} (v${skill.version}, Status: ${skill.status}, Success Rate: ${(skill.successRate * 100).toInt()}%)\n")
                sb.append("  Description: ${skill.description}\n")
                sb.append("  Required Tools: ${skill.requiredToolsJson}\n")
                sb.append("  Verification Rules: ${skill.verificationRulesJson}\n")
            }
        }

        sb.append("=== END BRAIN MEMORIES & SKILLS ===\n")

        sb.toString()
    }

    // ==========================================
    // 4. FORGET / CLEAR / DELETE
    // ==========================================

    suspend fun forget(queryOrId: String): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext "Brain database not available."
        val byId = d.getMemoryById(queryOrId, CURRENT_USER_ID)
        if (byId != null) {
            d.deleteMemoryById(byId.id, CURRENT_USER_ID)
            d.insertAuditLog(BrainAuditLog(memoryId = byId.id, action = AuditAction.DELETED.name, details = "Deleted memory '${byId.key}'"))
            return@withContext "Bhula diya: '${byId.key}'."
        }

        val search = d.searchMemories(CURRENT_USER_ID, queryOrId, limit = 1)
        if (search.isNotEmpty()) {
            val target = search.first()
            d.deleteMemoryById(target.id, CURRENT_USER_ID)
            d.insertAuditLog(BrainAuditLog(memoryId = target.id, action = AuditAction.DELETED.name, details = "Deleted memory '${target.key}'"))
            return@withContext "Bhula diya: '${target.key}'."
        }

        "Aisi koi memory nahi mili jise bhulna ho."
    }

    suspend fun forgetCategory(category: MemoryCategory): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext "Brain database not available."
        d.deleteCategory(CURRENT_USER_ID, category.name)
        d.insertAuditLog(BrainAuditLog(action = AuditAction.DELETED.name, details = "Cleared entire category ${category.name}"))
        "Category '${category.displayName}' ki saari memories bhula di gayi hain."
    }

    suspend fun clearAllMemories(): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext "Brain database not available."
        d.clearAllMemories(CURRENT_USER_ID)
        d.insertAuditLog(BrainAuditLog(action = AuditAction.DELETED.name, details = "Cleared all user memories."))
        seedInitialKnowledge(d)
        "Maya ki saari memories clear kar di gayi hain."
    }

    // ==========================================
    // 5. PAUSE & RESUME
    // ==========================================

    suspend fun togglePauseMemory(): Boolean = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext false
        val newPaused = !_isPaused.value
        _isPaused.value = newPaused
        d.saveUserSettings(BrainUserSettings(userId = CURRENT_USER_ID, isMemoryPaused = newPaused))
        d.insertAuditLog(BrainAuditLog(action = if (newPaused) AuditAction.PAUSED.name else AuditAction.RESUMED.name, details = "Memory paused = $newPaused"))
        newPaused
    }

    // ==========================================
    // 6. EXPLAIN MEMORY ("Why do you remember this?")
    // ==========================================

    suspend fun explainMemory(queryOrId: String): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext "Database unavailable."
        val byId = d.getMemoryById(queryOrId, CURRENT_USER_ID)
            ?: d.searchMemories(CURRENT_USER_ID, queryOrId, limit = 1).firstOrNull()

        if (byId == null) {
            return@withContext "Mujhe is baare me koi specific memory nahi mili."
        }

        buildString {
            append("🧠 Memory: ${byId.key}\n")
            append("• Content: ${byId.content}\n")
            append("• Why: ${byId.why}\n")
            append("• Source: ${byId.source}\n")
            append("• Confidence: ${byId.confidence}\n")
            append("• Importance: ${byId.importance}/10\n")
            append("• Scope: ${byId.scope}")
        }
    }

    // ==========================================
    // 7. OBSERVE & LEARN FROM TASK VERIFICATION
    // ==========================================

    suspend fun observeAndLearn(taskName: String, actionSuccess: Boolean, notes: String) = withContext(Dispatchers.IO) {
        WorkingMemory.recordToolResult(taskName, notes, actionSuccess)
        EmotionalStateEngine.updateStateFromContext(taskName, actionSuccess)

        if (actionSuccess && notes.isNotBlank()) {
            // Learn verified experience
            remember(
                key = "verified_task_$taskName",
                content = "Task '$taskName' completed successfully: $notes",
                category = MemoryCategory.EXPERIENCES,
                why = "Verified task execution",
                importance = 5,
                confidence = MemoryConfidence.HIGH,
                source = MemorySource.VERIFIED_ACTION
            )
        }
    }

    // ==========================================
    // 8. EXPORT & IMPORT JSON
    // ==========================================

    suspend fun exportAsJson(): String = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext "{}"
        val memories = d.getAllActiveMemories(CURRENT_USER_ID)
        val root = JSONObject()
        val array = JSONArray()

        memories.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("category", m.category)
                put("key", m.key)
                put("content", m.content)
                put("why", m.why)
                put("confidence", m.confidence)
                put("source", m.source)
                put("importance", m.importance)
                put("scope", m.scope)
                put("strength", m.strength)
                put("createdAt", m.createdAt)
            }
            array.put(obj)
        }
        root.put("version", 1)
        root.put("userId", CURRENT_USER_ID)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("memories", array)
        root.toString(2)
    }

    suspend fun importFromJson(jsonStr: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext Pair(false, "Database unavailable.")
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("memories") ?: return@withContext Pair(false, "Invalid format: 'memories' array missing.")

            var importedCount = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val key = obj.getString("key")
                val content = obj.getString("content")
                val category = obj.optString("category", MemoryCategory.PREFERENCES.name)
                val importance = obj.optInt("importance", 5)

                val secCheck = MemorySecurityEngine.evaluate(key, content)
                if (secCheck.isSafe) {
                    val item = MemoryItem(
                        userId = CURRENT_USER_ID,
                        category = category,
                        key = key,
                        content = secCheck.sanitizedContent,
                        why = "Imported memory backup",
                        importance = importance,
                        source = MemorySource.IMPORTED.name
                    )
                    MemoryConsolidationEngine.consolidateOrInsert(d, CURRENT_USER_ID, item)
                    importedCount++
                }
            }
            d.insertAuditLog(BrainAuditLog(action = AuditAction.IMPORTED.name, details = "Imported $importedCount memories."))
            Pair(true, "Successfully imported $importedCount memories! ✨")
        } catch (e: Exception) {
            Log.e(TAG, "Import error: ${e.message}")
            Pair(false, "Failed to import JSON: ${e.message}")
        }
    }

    // ==========================================
    // 9. HEALTH DASHBOARD STATUS
    // ==========================================

    suspend fun getBrainHealth(): BrainHealthStatus = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext BrainHealthStatus(0, 0, false, emptyMap(), "Error", "Offline", 0L)
        BrainHealthEngine.getHealthStatus(d, CURRENT_USER_ID)
    }

    // ==========================================
    // 10. CONTEXT MAP & SKILL FORGE FLOWS & OPS
    // ==========================================

    fun getAllContextNodesFlow(): Flow<List<com.example.brain.model.ContextNode>>? {
        return dao?.getAllContextNodesFlow(CURRENT_USER_ID)
    }

    fun getAllContextEdgesFlow(): Flow<List<com.example.brain.model.ContextEdge>>? {
        return dao?.getAllContextEdgesFlow(CURRENT_USER_ID)
    }

    fun getAllSkillsFlow(): Flow<List<com.example.brain.model.SkillEntity>>? {
        return dao?.getAllSkillsFlow(CURRENT_USER_ID)
    }

    fun getRecentSkillExecutionsFlow(): Flow<List<com.example.brain.model.SkillExecutionRecord>>? {
        return dao?.getRecentSkillExecutionsFlow(CURRENT_USER_ID)
    }

    suspend fun addContextNode(
        type: com.example.brain.model.ContextNodeType,
        title: String,
        description: String = "",
        importance: Int = 5
    ): com.example.brain.model.ContextNode? = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext null
        com.example.brain.subsystems.ContextMapEngine.getOrCreateNode(
            dao = d,
            userId = CURRENT_USER_ID,
            type = type,
            title = title,
            description = description,
            importance = importance
        )
    }

    suspend fun linkContextNodes(
        fromId: String,
        toId: String,
        relType: com.example.brain.model.ContextRelationshipType
    ) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        com.example.brain.subsystems.ContextMapEngine.linkNodes(
            dao = d,
            userId = CURRENT_USER_ID,
            fromNodeId = fromId,
            toNodeId = toId,
            relationshipType = relType
        )
    }

    suspend fun deleteContextNode(id: String) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.deleteContextNode(id, CURRENT_USER_ID)
        d.deleteEdgesForNode(CURRENT_USER_ID, id)
    }

    suspend fun deleteSkill(skillId: String) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.deleteSkill(skillId, CURRENT_USER_ID)
    }

    suspend fun toggleSkillStatus(skillId: String): Boolean = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext false
        val skill = d.getSkillById(skillId, CURRENT_USER_ID) ?: return@withContext false
        val newStatus = if (skill.status == com.example.brain.model.SkillStatus.DISABLED.name) {
            com.example.brain.model.SkillStatus.VERIFIED.name
        } else {
            com.example.brain.model.SkillStatus.DISABLED.name
        }
        d.updateSkill(skill.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        newStatus == com.example.brain.model.SkillStatus.VERIFIED.name
    }
}
