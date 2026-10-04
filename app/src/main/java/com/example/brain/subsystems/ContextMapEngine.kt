package com.example.brain.subsystems

import android.util.Log
import com.example.brain.db.BrainDao
import com.example.brain.model.ContextConfidence
import com.example.brain.model.ContextEdge
import com.example.brain.model.ContextNode
import com.example.brain.model.ContextNodeType
import com.example.brain.model.ContextRelationshipType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object ContextMapEngine {

    private const val TAG = "ContextMapEngine"
    private const val CURRENT_USER_ID = "default_user"

    /**
     * Seeds initial connected graph for Maya's internal knowledge map
     */
    suspend fun seedInitialGraph(dao: BrainDao, userId: String = CURRENT_USER_ID) = withContext(Dispatchers.IO) {
        val count = dao.getContextNodeCount(userId)
        if (count > 0) return@withContext

        Log.i(TAG, "Seeding initial Context & Knowledge Map for userId: $userId")

        // 1. Root User & Assistant nodes
        val userNode = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.USER,
            title = "SHADOW X RAHUL",
            description = "Creator and Boss of Maya. Master authority and primary user.",
            importance = 10
        )

        val assistantNode = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.PROJECT,
            title = "Maya AI Companion",
            description = "Intelligent, affectionate, ultra-fast personal assistant on Android.",
            importance = 10
        )

        linkNodes(dao, userId, userNode.id, assistantNode.id, ContextRelationshipType.BELONGS_TO, ContextConfidence.HIGH)

        // 2. Feature: WhatsApp Automation with verified 13-step flow
        val whatsappFeature = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.FEATURE,
            title = "WhatsApp Automation",
            description = "Automated WhatsApp messaging with 13-step verified UI interaction.",
            importance = 9
        )
        linkNodes(dao, userId, assistantNode.id, whatsappFeature.id, ContextRelationshipType.PART_OF, ContextConfidence.HIGH)

        val whatsappProblem = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.PROBLEM,
            title = "WhatsApp Message Failure & Ambiguity",
            description = "Multiple contacts found with same name, or premature success claim before message bubble appears.",
            importance = 9
        )
        linkNodes(dao, userId, whatsappFeature.id, whatsappProblem.id, ContextRelationshipType.CAUSED_BY, ContextConfidence.HIGH)

        val whatsappSolution = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.SOLUTION,
            title = "13-Step Verification Protocol",
            description = "Open WhatsApp -> Search -> Verify exact contact (clarify last 4 digits if multiple) -> Enter text -> Tap Send -> Verify bubble.",
            importance = 10
        )
        linkNodes(dao, userId, whatsappProblem.id, whatsappSolution.id, ContextRelationshipType.SOLVED_BY, ContextConfidence.HIGH)

        val whatsappTool = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.TOOL,
            title = "sendWhatsAppMessage",
            description = "Direct tool execution engine for WhatsApp automation.",
            importance = 8
        )
        linkNodes(dao, userId, whatsappSolution.id, whatsappTool.id, ContextRelationshipType.USES, ContextConfidence.HIGH)

        val whatsappSkill = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.SKILL,
            title = "Verified WhatsApp Message Workflow",
            description = "Learned and verified repeatable skill for sending WhatsApp messages safely.",
            importance = 9
        )
        linkNodes(dao, userId, whatsappSolution.id, whatsappSkill.id, ContextRelationshipType.PRODUCES, ContextConfidence.HIGH)
        linkNodes(dao, userId, whatsappSkill.id, whatsappTool.id, ContextRelationshipType.REQUIRES, ContextConfidence.HIGH)

        // 3. Feature: Live Website Generator
        val webFeature = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.FEATURE,
            title = "Live Website Builder",
            description = "Instant HTML5/CSS3/JS website coding with real-time code streaming and automatic Chrome launch.",
            importance = 8
        )
        linkNodes(dao, userId, assistantNode.id, webFeature.id, ContextRelationshipType.PART_OF, ContextConfidence.HIGH)

        val webTool = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.TOOL,
            title = "buildWebsite",
            description = "Builds website locally, runs nano HTTP server, streams code matrix, and opens Chrome.",
            importance = 8
        )
        linkNodes(dao, userId, webFeature.id, webTool.id, ContextRelationshipType.USES, ContextConfidence.HIGH)

        // 4. Feature: Personality & Normal Mode
        val normalModeFeature = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.FEATURE,
            title = "Normal & Personality Modes",
            description = "Switch between Normal Mode (zero romantic words, professional) and Personality Modes (Girlfriend, Nakhre, Super Friendly).",
            importance = 9
        )
        linkNodes(dao, userId, assistantNode.id, normalModeFeature.id, ContextRelationshipType.PART_OF, ContextConfidence.HIGH)

        val modeDecision = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.DECISION,
            title = "Master Mode Switch Protocol",
            description = "When switch is OFF, strictly suppress babu/sona/jaanu; when ON, activate chosen persona.",
            importance = 9
        )
        linkNodes(dao, userId, normalModeFeature.id, modeDecision.id, ContextRelationshipType.DEPENDS_ON, ContextConfidence.HIGH)

        // 5. Feature: Voice Engine
        val voiceFeature = getOrCreateNode(
            dao = dao,
            userId = userId,
            type = ContextNodeType.FEATURE,
            title = "Voice Personalities",
            description = "Multi-voice selection including Aoede, Venom, Kore, Puck, Charon, Fenrir, Jarvis, Friday.",
            importance = 8
        )
        linkNodes(dao, userId, assistantNode.id, voiceFeature.id, ContextRelationshipType.PART_OF, ContextConfidence.HIGH)

        Log.i(TAG, "Context & Knowledge Map successfully initialized with core knowledge graph.")
    }

    /**
     * Get existing node or create new without duplicate creation
     */
    suspend fun getOrCreateNode(
        dao: BrainDao,
        userId: String,
        type: ContextNodeType,
        title: String,
        description: String = "",
        propertiesJson: String = "{}",
        importance: Int = 5,
        confidence: ContextConfidence = ContextConfidence.HIGH
    ): ContextNode = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val existing = dao.findContextNode(userId, type.name, cleanTitle)
        if (existing != null) {
            // Update existing if new description or higher importance
            val updated = existing.copy(
                description = if (description.isNotBlank()) description else existing.description,
                importance = maxOf(existing.importance, importance),
                updatedAt = System.currentTimeMillis()
            )
            dao.updateContextNode(updated)
            return@withContext updated
        }

        val newNode = ContextNode(
            id = UUID.randomUUID().toString(),
            userId = userId,
            nodeType = type.name,
            title = cleanTitle,
            description = description,
            propertiesJson = propertiesJson,
            confidence = confidence.name,
            importance = importance,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertContextNode(newNode)
        newNode
    }

    /**
     * Link two nodes with a semantic relationship
     */
    suspend fun linkNodes(
        dao: BrainDao,
        userId: String,
        fromNodeId: String,
        toNodeId: String,
        relationshipType: ContextRelationshipType,
        confidence: ContextConfidence = ContextConfidence.HIGH,
        weight: Float = 1.0f,
        metadataJson: String = "{}"
    ) = withContext(Dispatchers.IO) {
        if (fromNodeId == toNodeId) return@withContext

        val existing = dao.findEdge(userId, fromNodeId, toNodeId, relationshipType.name)
        if (existing != null) return@withContext

        val edge = ContextEdge(
            id = UUID.randomUUID().toString(),
            userId = userId,
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            relationshipType = relationshipType.name,
            confidence = confidence.name,
            weight = weight,
            metadataJson = metadataJson,
            createdAt = System.currentTimeMillis()
        )
        dao.insertContextEdge(edge)
    }

    /**
     * Context-Aware Recall: Given a user query or topic, find relevant root nodes,
     * traverse 1-2 degrees of connected relationships, rank relevance, and return
     * a concise structured context string.
     */
    suspend fun recallContextGraph(
        dao: BrainDao,
        query: String,
        userId: String = CURRENT_USER_ID,
        maxHops: Int = 2,
        maxNodes: Int = 8
    ): String = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return@withContext ""

        // Extract key terms
        val terms = cleanQuery.split(" ", ",", ".", "?", "!", "-", "_")
            .filter { it.length >= 3 && it !in listOf("the", "and", "for", "with", "karo", "kare", "hoga", "wali", "wala", "phir", "aaya", "gaya") }

        // Find matching nodes
        val matchedNodes = mutableSetOf<ContextNode>()
        for (term in terms.take(5)) {
            val list = dao.searchContextNodes(userId, term, limit = 5)
            matchedNodes.addAll(list)
        }

        if (matchedNodes.isEmpty()) {
            val directMatches = dao.searchContextNodes(userId, cleanQuery.take(20), limit = 5)
            matchedNodes.addAll(directMatches)
        }

        if (matchedNodes.isEmpty()) return@withContext ""

        // Traverse connected graph (BFS up to maxHops)
        val visitedNodeIds = mutableSetOf<String>()
        val collectedNodes = mutableMapOf<String, ContextNode>()
        val collectedEdges = mutableListOf<Triple<String, String, String>>() // (fromTitle, relType, toTitle)

        for (node in matchedNodes.take(3)) {
            collectedNodes[node.id] = node
            visitedNodeIds.add(node.id)

            // Hop 1
            val connectedEdges = dao.getConnectedEdges(userId, node.id)
            for (edge in connectedEdges.take(6)) {
                val otherId = if (edge.fromNodeId == node.id) edge.toNodeId else edge.fromNodeId
                if (otherId !in visitedNodeIds && collectedNodes.size < maxNodes) {
                    val otherNode = dao.getContextNodeById(otherId, userId)
                    if (otherNode != null) {
                        visitedNodeIds.add(otherId)
                        collectedNodes[otherId] = otherNode
                        val fromTitle = if (edge.fromNodeId == node.id) node.title else otherNode.title
                        val toTitle = if (edge.fromNodeId == node.id) otherNode.title else node.title
                        collectedEdges.add(Triple(fromTitle, edge.relationshipType, toTitle))
                    }
                }
            }
        }

        // Build concise, readable graph summary
        val sb = StringBuilder()
        sb.append("CONNECTED KNOWLEDGE & CONTEXT MAP:\n")
        
        // Show entity chain
        if (collectedEdges.isNotEmpty()) {
            sb.append("• Relationships & Solutions Chain:\n")
            collectedEdges.take(5).forEach { (from, rel, to) ->
                sb.append("  - $from ──[$rel]──> $to\n")
            }
        }

        // Show node details (problems, solutions, skills, lessons)
        val highlighted = collectedNodes.values
            .sortedByDescending { it.importance }
            .take(4)

        for (n in highlighted) {
            val icon = try { ContextNodeType.valueOf(n.nodeType).icon } catch (e: Exception) { "📌" }
            sb.append("• $icon [${n.nodeType}] ${n.title}: ${n.description.take(120)}\n")
        }

        sb.toString().trim()
    }
}
