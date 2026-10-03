package com.example.brain.subsystems

data class CapabilityEntry(
    val actionName: String,
    val description: String,
    val requiredPermission: String?,
    val isAvailable: Boolean = true,
    val limitations: String = ""
)

object SelfKnowledgeEngine {

    val capabilities: List<CapabilityEntry> = listOf(
        CapabilityEntry(
            actionName = "Phone Call",
            description = "Search contacts and launch voice calls.",
            requiredPermission = "android.permission.CALL_PHONE",
            limitations = "Requires CALL_PHONE permission and SIM selection confirmation."
        ),
        CapabilityEntry(
            actionName = "SMS Messaging",
            description = "Search contacts and send direct SMS texts.",
            requiredPermission = "android.permission.SEND_SMS",
            limitations = "Requires SEND_SMS permission; asks for clarification if multiple numbers match."
        ),
        CapabilityEntry(
            actionName = "WhatsApp Automation",
            description = "Search contact, type message, and click send via Accessibility.",
            requiredPermission = "Accessibility Service",
            limitations = "Requires Maya Accessibility Service enabled; strictly verifies message delivery bubble."
        ),
        CapabilityEntry(
            actionName = "Media Playback Control",
            description = "Play, pause, resume, next, previous, stop, seek forward/back.",
            requiredPermission = "MediaSession / Notification Listener",
            limitations = "Works on active media players; falls back to hardware media keys."
        ),
        CapabilityEntry(
            actionName = "System Settings Toggles",
            description = "Toggle Wi-Fi, Bluetooth, Flashlight/Torch, Hotspot, and Mobile Data.",
            requiredPermission = "Settings & Accessibility",
            limitations = "Toggles background tile seamlessly without leaving Quick Settings hanging open."
        ),
        CapabilityEntry(
            actionName = "Screen Perception & Tap",
            description = "Capture screen text and interactive elements, and tap buttons directly.",
            requiredPermission = "Accessibility Service",
            limitations = "Requires user-enabled Screen Capture / Accessibility; never executes unknown destructive actions."
        ),
        CapabilityEntry(
            actionName = "Web Coding & Builder",
            description = "Generate full HTML/CSS/JS applications and preview them live in Chrome.",
            requiredPermission = null,
            limitations = "Writes code stream in background; opens browser for full interaction."
        ),
        CapabilityEntry(
            actionName = "Brain Memory & Recall",
            description = "Persistent human-like contextual memory, preferences, and verified learning.",
            requiredPermission = null,
            limitations = "Never stores sensitive credentials; user has full control to view, pause, edit, or forget."
        )
    )

    fun getCapabilitySummary(): String {
        return capabilities.joinToString("\n") {
            "• ${it.actionName}: ${it.description} (Constraint: ${it.limitations})"
        }
    }
}
