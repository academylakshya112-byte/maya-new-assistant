package com.example.brain.model

enum class MemoryScope {
    GLOBAL,
    PROJECT,
    TASK,
    CONVERSATION,
    TEMPORARY
}

enum class MemoryConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class MemorySource {
    EXPLICIT_USER,
    CONVERSATION,
    PROJECT,
    LEARNED_PATTERN,
    VERIFIED_ACTION,
    IMPORTED,
    SYSTEM
}

enum class PreferenceStrength {
    WEAK,
    NORMAL,
    STRONG,
    EXPLICIT_REQUIRED
}

enum class GoalStatus {
    PLANNED,
    IN_PROGRESS,
    WAITING,
    COMPLETED,
    FAILED
}

enum class WaitingState {
    NONE,
    WAITING_FOR_USER,
    WAITING_FOR_NETWORK,
    WAITING_FOR_TOOL,
    WAITING_FOR_UPLOAD,
    WAITING_FOR_APPROVAL,
    WAITING_FOR_EXTERNAL_RESULT
}

enum class EmotionalSimulationState {
    NEUTRAL,
    FOCUSED,
    CURIOUS,
    CALM,
    CONCERNED,
    EXCITED,
    SUPPORTIVE,
    CONFUSED
}

enum class CertaintyLevel {
    CERTAIN,
    LIKELY,
    UNCERTAIN,
    UNKNOWN
}

enum class MemoryCategory(val displayName: String) {
    PERSONAL("Personal Profile"),
    LIKES("Likes"),
    DISLIKES("Dislikes"),
    PREFERENCES("Preferences"),
    PROJECTS("Projects"),
    GOALS("Goals & Tasks"),
    EPISODIC("Important Events"),
    PROCEDURAL("Procedures & Workflows"),
    SKILLS("Learned Skills"),
    DECISIONS("Decisions"),
    HABITS("Habits & Routines"),
    EXPERIENCES("Verified Experiences")
}

enum class AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    RETRIEVED,
    IMPORTED,
    EXPORTED,
    CONSOLIDATED,
    PAUSED,
    RESUMED
}

// ==========================================
// CONTEXT / KNOWLEDGE MAP ENUMS
// ==========================================

enum class ContextNodeType(val icon: String) {
    USER("👤"),
    MEMORY("🧠"),
    PREFERENCE("⚙️"),
    PROJECT("📁"),
    FEATURE("🧩"),
    TASK("📋"),
    GOAL("🎯"),
    EVENT("📅"),
    PROBLEM("⚠️"),
    CAUSE("🔍"),
    SOLUTION("💡"),
    DECISION("⚖️"),
    SKILL("🛠️"),
    WORKFLOW("🔄"),
    TOOL("🔧"),
    RESULT("✅"),
    LESSON("📚")
}

enum class ContextRelationshipType(val label: String) {
    RELATED_TO("Related To"),
    BELONGS_TO("Belongs To"),
    CONTAINS("Contains"),
    PART_OF("Part Of"),
    DEPENDS_ON("Depends On"),
    CAUSED_BY("Caused By"),
    SOLVED_BY("Solved By"),
    LEARNED_FROM("Learned From"),
    USES("Uses"),
    REQUIRES("Requires"),
    IMPROVES("Improves"),
    REPLACES("Replaces"),
    CONFLICTS_WITH("Conflicts With"),
    PRECEDES("Precedes"),
    FOLLOWS("Follows"),
    PRODUCES("Produces"),
    ASSOCIATED_WITH("Associated With")
}

enum class ContextConfidence {
    HIGH,
    MEDIUM,
    LOW
}

// ==========================================
// SKILL FORGE ENUMS
// ==========================================

enum class SkillStatus(val displayName: String) {
    DRAFT("Draft"),
    LEARNED("Learned"),
    VERIFIED("Verified"),
    OUTDATED("Outdated"),
    DISABLED("Disabled")
}

enum class SkillConfidence {
    LOW,
    MEDIUM,
    HIGH
}
