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
