package com.example.brain.subsystems

import com.example.brain.db.BrainDao
import com.example.brain.model.MemoryConfidence
import com.example.brain.model.MemoryItem
import com.example.brain.model.PreferenceStrength
import java.util.Locale

object MemoryRetrievalEngine {

    suspend fun retrieveRelevant(
        dao: BrainDao,
        userId: String,
        query: String,
        limit: Int = 10
    ): List<MemoryItem> {
        val allActive = dao.getAllActiveMemories(userId)
        if (allActive.isEmpty()) return emptyList()

        val tokens = query.lowercase(Locale.getDefault())
            .split(" ", ",", ".", "?", "!", "\n")
            .filter { it.length > 2 }

        val scored = allActive.map { item ->
            val score = computeRelevanceScore(item, tokens, query)
            item to score
        }

        return scored
            .filter { it.second > 0.0f }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun computeRelevanceScore(item: MemoryItem, tokens: List<String>, rawQuery: String): Float {
        var score = 0.0f

        val keyLower = item.key.lowercase(Locale.getDefault())
        val contentLower = item.content.lowercase(Locale.getDefault())
        val tagsLower = item.tags.lowercase(Locale.getDefault())
        val rawLower = rawQuery.lowercase(Locale.getDefault())

        // 1. Direct exact or substring match
        if (rawLower.contains(keyLower) || contentLower.contains(rawLower)) {
            score += 15.0f
        }

        // 2. Token overlap
        for (token in tokens) {
            if (keyLower.contains(token)) score += 5.0f
            if (contentLower.contains(token)) score += 3.0f
            if (tagsLower.contains(token)) score += 2.0f
        }

        // 3. Importance weighting (0 to 10)
        score += (item.importance * 1.5f)

        // 4. Confidence boost
        when (item.confidence) {
            MemoryConfidence.HIGH.name -> score += 4.0f
            MemoryConfidence.MEDIUM.name -> score += 2.0f
            MemoryConfidence.LOW.name -> score += 0.5f
        }

        // 5. Preference strength boost
        when (item.strength) {
            PreferenceStrength.EXPLICIT_REQUIRED.name -> score += 10.0f
            PreferenceStrength.STRONG.name -> score += 6.0f
            PreferenceStrength.NORMAL.name -> score += 2.0f
            PreferenceStrength.WEAK.name -> score += 0.0f
        }

        // 6. Recency boost (within last 48 hours gets bonus)
        val ageHours = (System.currentTimeMillis() - item.updatedAt) / (1000 * 60 * 60)
        if (ageHours < 48) {
            score += 3.0f
        }

        return score
    }
}
