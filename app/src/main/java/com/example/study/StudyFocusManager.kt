package com.example.study

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.accessibility.ZoyaAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class StudyModeType {
    NONE,
    NORMAL,
    RESTRICTED
}

enum class YouTubePolicy {
    NONE,
    STUDY,
    RESTRICTED
}

data class StudyFocusState(
    val mode: StudyModeType = StudyModeType.NONE,
    val isActive: Boolean = false,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val durationMinutes: Int = 0,
    val pendingEmergencyOverride: Boolean = false,
    val youtubePolicy: YouTubePolicy = YouTubePolicy.NONE
) {
    val remainingMillis: Long
        get() {
            if (!isActive || mode == StudyModeType.NONE) return 0L
            val now = System.currentTimeMillis()
            return (endTimeMillis - now).coerceAtLeast(0L)
        }

    val remainingFormatted: String
        get() {
            val totalSeconds = remainingMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }

    val remainingSpokenHindi: String
        get() {
            val totalMinutes = (remainingMillis / 1000 + 59) / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "$hours ghanta $minutes minute"
                hours > 0 -> "$hours ghanta"
                minutes > 0 -> "$minutes minute"
                else -> "1 minute se kam"
            }
        }
}

sealed class ActivationResult {
    data class Success(val mode: StudyModeType, val durationMinutes: Int, val youtubePolicy: YouTubePolicy, val message: String) : ActivationResult()
    data class NeedsYouTubePolicy(val message: String) : ActivationResult()
    data class MissingPermission(val mode: StudyModeType, val message: String) : ActivationResult()
    data class AlreadyActive(val currentMode: StudyModeType, val remainingSpoken: String, val message: String) : ActivationResult()
    data class Error(val message: String) : ActivationResult()
}

sealed class DeactivationResult {
    data class Success(val message: String) : DeactivationResult()
    data class RestrictedLocked(val remainingSpoken: String, val message: String) : DeactivationResult()
    data class NeedsEmergencyConfirmation(val message: String) : DeactivationResult()
    data class Cancelled(val message: String) : DeactivationResult()
    data class NotActive(val message: String) : DeactivationResult()
}

object StudyFocusManager {

    private const val TAG = "StudyFocusManager"
    private const val PREFS_NAME = "StudyFocusPrefs"
    private const val KEY_MODE = "study_mode"
    private const val KEY_IS_ACTIVE = "study_is_active"
    private const val KEY_START_TIME = "study_start_time"
    private const val KEY_END_TIME = "study_end_time"
    private const val KEY_DURATION_MINUTES = "study_duration_minutes"
    private const val KEY_YOUTUBE_POLICY = "study_youtube_policy"

    private val _state = MutableStateFlow(StudyFocusState())
    val state: StateFlow<StudyFocusState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Callback when study mode expires
    var onModeExpired: ((StudyModeType) -> Unit)? = null

    // Distraction packages configured for Restricted Mode
    val RESTRICTED_PACKAGES = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.snapchat.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus"
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        loadPersistedState()
        startTimerTicker()
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadPersistedState() {
        val prefs = getPrefs() ?: return
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val modeStr = prefs.getString(KEY_MODE, StudyModeType.NONE.name) ?: StudyModeType.NONE.name
        val mode = try {
            StudyModeType.valueOf(modeStr)
        } catch (e: Exception) {
            StudyModeType.NONE
        }
        val startTime = prefs.getLong(KEY_START_TIME, 0L)
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val duration = prefs.getInt(KEY_DURATION_MINUTES, 0)

        val now = System.currentTimeMillis()
        val policyStr = prefs.getString(KEY_YOUTUBE_POLICY, YouTubePolicy.NONE.name) ?: YouTubePolicy.NONE.name
        val policy = try {
            YouTubePolicy.valueOf(policyStr)
        } catch (e: Exception) {
            if (mode == StudyModeType.RESTRICTED) YouTubePolicy.STUDY else YouTubePolicy.NONE
        }

        if (isActive && mode != StudyModeType.NONE && endTime > now) {
            _state.value = StudyFocusState(
                mode = mode,
                isActive = true,
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                durationMinutes = duration,
                pendingEmergencyOverride = false,
                youtubePolicy = policy
            )
            Log.d(TAG, "Restored active study state: mode=$mode, policy=$policy, remaining=${(endTime - now) / 1000}s")
        } else {
            // Expired or stale
            clearPersistedState()
            _state.value = StudyFocusState()
            Log.d(TAG, "No active study state or session already expired")
        }
    }

    private fun persistState(state: StudyFocusState) {
        val prefs = getPrefs() ?: return
        prefs.edit().apply {
            putBoolean(KEY_IS_ACTIVE, state.isActive)
            putString(KEY_MODE, state.mode.name)
            putLong(KEY_START_TIME, state.startTimeMillis)
            putLong(KEY_END_TIME, state.endTimeMillis)
            putInt(KEY_DURATION_MINUTES, state.durationMinutes)
            putString(KEY_YOUTUBE_POLICY, state.youtubePolicy.name)
            apply()
        }
    }

    private fun clearPersistedState() {
        val prefs = getPrefs() ?: return
        prefs.edit().clear().apply()
    }

    private fun startTimerTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _state.value
                if (current.isActive && current.mode != StudyModeType.NONE) {
                    val now = System.currentTimeMillis()
                    if (now >= current.endTimeMillis) {
                        expireMode(current.mode)
                    } else {
                        // Trigger state emission to update live timer on UI
                        _state.value = current.copy()
                    }
                }
            }
        }
    }

    fun isPermissionGranted(mode: StudyModeType): Boolean {
        // Enforced via Maya's Accessibility Service
        return ZoyaAccessibilityService.isServiceRunning()
    }

    fun activateMode(
        mode: StudyModeType,
        durationMinutes: Int,
        youtubePolicy: YouTubePolicy = YouTubePolicy.NONE
    ): ActivationResult {
        if (durationMinutes <= 0) {
            return ActivationResult.Error("Kitne time ke liye boss?")
        }

        // For Study Restricted Mode, selecting a YouTube policy is mandatory!
        if (mode == StudyModeType.RESTRICTED && youtubePolicy == YouTubePolicy.NONE) {
            return ActivationResult.NeedsYouTubePolicy("Boss, YouTube ko Study Mode me rakhna hai ya YouTube ko bhi Study Restricted Mode me rakhun?")
        }

        // Check required permission
        if (!isPermissionGranted(mode)) {
            val msg = if (mode == StudyModeType.NORMAL) {
                "Boss, Study Mode ke liye permission required hai. Permission Settings se allow kar do."
            } else {
                "Boss, Study Restricted Mode ke liye required permission allowed nahi hai. Permission Settings me jaakar allow karo."
            }
            return ActivationResult.MissingPermission(mode, msg)
        }

        val current = _state.value
        // Multiple activation protection
        if (current.isActive && current.mode != StudyModeType.NONE) {
            val msg = "Boss, Study Mode already active hai. Kya existing timer change karna hai?"
            return ActivationResult.AlreadyActive(current.mode, current.remainingSpokenHindi, msg)
        }

        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60_000L)
        val assignedPolicy = if (mode == StudyModeType.RESTRICTED) youtubePolicy else YouTubePolicy.NONE

        val newState = StudyFocusState(
            mode = mode,
            isActive = true,
            startTimeMillis = now,
            endTimeMillis = endTime,
            durationMinutes = durationMinutes,
            pendingEmergencyOverride = false,
            youtubePolicy = assignedPolicy
        )

        _state.value = newState
        persistState(newState)

        // If YouTube is already open when YouTube Restricted Mode activates, immediately exit to Home!
        if (mode == StudyModeType.RESTRICTED && assignedPolicy == YouTubePolicy.RESTRICTED) {
            val activeService = ZoyaAccessibilityService.instance
            val currentPkg = activeService?.rootInActiveWindow?.packageName?.toString() ?: ""
            if (currentPkg.contains("youtube")) {
                activeService?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            }
        }

        val durationSpoken = formatDurationHindi(durationMinutes)
        val response = if (mode == StudyModeType.NORMAL) {
            "Done boss, Study Mode $durationSpoken ke liye active hai."
        } else if (assignedPolicy == YouTubePolicy.RESTRICTED) {
            "Done boss, Study Restricted Mode $durationSpoken ke liye active hai, aur YouTube bhi completely restricted hai."
        } else {
            "Done boss, Study Restricted Mode $durationSpoken ke liye active hai. YouTube Study Mode me rahega."
        }

        Handler(Looper.getMainLooper()).post {
            appContext?.let {
                val badge = if (mode == StudyModeType.NORMAL) "📚 Study Mode" else "🔒 Study Restricted Mode"
                val extra = if (mode == StudyModeType.RESTRICTED) {
                    if (assignedPolicy == YouTubePolicy.RESTRICTED) " • YouTube Blocked" else " • YouTube Study Filter"
                } else ""
                Toast.makeText(it, "$badge Active ($durationSpoken)$extra", Toast.LENGTH_SHORT).show()
            }
        }

        return ActivationResult.Success(mode, durationMinutes, assignedPolicy, response)
    }

    fun deactivateNormalMode(): DeactivationResult {
        val current = _state.value
        if (!current.isActive || current.mode == StudyModeType.NONE) {
            return DeactivationResult.NotActive("Abhi koi Study Mode active nahi hai boss.")
        }

        if (current.mode == StudyModeType.RESTRICTED) {
            val msg = "Boss, Study Restricted Mode abhi active hai. Iska set kiya hua time poora hone tak main ise normal command se band nahi kar sakti. ${current.remainingSpokenHindi} baki hai boss."
            return DeactivationResult.RestrictedLocked(current.remainingSpokenHindi, msg)
        }

        // Normal mode can always be stopped by user
        clearPersistedState()
        _state.value = StudyFocusState()

        Handler(Looper.getMainLooper()).post {
            appContext?.let {
                Toast.makeText(it, "📚 Study Mode Deactivated", Toast.LENGTH_SHORT).show()
            }
        }

        return DeactivationResult.Success("Study Mode band kar diya boss.")
    }

    fun requestEmergencyOverride(): DeactivationResult {
        val current = _state.value
        if (!current.isActive || current.mode != StudyModeType.RESTRICTED) {
            return DeactivationResult.NotActive("Restricted Mode active nahi hai boss.")
        }

        _state.value = current.copy(pendingEmergencyOverride = true)
        val msg = "Boss, Emergency Override se Study Restricted Mode samay se pehle band ho jayega. Confirm karein?"
        return DeactivationResult.NeedsEmergencyConfirmation(msg)
    }

    fun confirmEmergencyOverride(confirmed: Boolean): DeactivationResult {
        val current = _state.value
        if (!current.isActive || current.mode != StudyModeType.RESTRICTED) {
            return DeactivationResult.NotActive("Restricted Mode active nahi hai boss.")
        }

        if (!current.pendingEmergencyOverride) {
            return DeactivationResult.NeedsEmergencyConfirmation(
                "Boss, pehle Emergency Override request karni hogi."
            )
        }

        if (!confirmed) {
            _state.value = current.copy(pendingEmergencyOverride = false)
            return DeactivationResult.Cancelled("Emergency Override cancel kar diya gaya boss.")
        }

        // Explicitly confirmed
        clearPersistedState()
        _state.value = StudyFocusState()

        Handler(Looper.getMainLooper()).post {
            appContext?.let {
                Toast.makeText(it, "🔒 Study Restricted Mode stopped via Emergency Override", Toast.LENGTH_LONG).show()
            }
        }

        return DeactivationResult.Success("Study Restricted Mode emergency override se band kar diya gaya boss.")
    }

    private fun expireMode(expiredMode: StudyModeType) {
        clearPersistedState()
        _state.value = StudyFocusState()
        Log.d(TAG, "Study mode $expiredMode naturally expired")

        Handler(Looper.getMainLooper()).post {
            appContext?.let {
                val label = if (expiredMode == StudyModeType.RESTRICTED) "Study Restricted Mode" else "Study Mode"
                Toast.makeText(it, "⏰ Boss, $label ka time poora ho gaya hai.", Toast.LENGTH_LONG).show()
            }
        }

        onModeExpired?.invoke(expiredMode)
    }

    fun getStatusSpoken(): String {
        val current = _state.value
        if (!current.isActive || current.mode == StudyModeType.NONE) {
            return "Abhi koi study mode active nahi hai boss."
        }

        val modeLabel = if (current.mode == StudyModeType.RESTRICTED) "Study Restricted Mode" else "Study Mode"
        return "Boss, $modeLabel active hai aur ${current.remainingSpokenHindi} baki hai."
    }

    fun formatDurationHindi(minutes: Int): String {
        val hours = minutes / 60
        val remainingMins = minutes % 60
        return when {
            hours > 0 && remainingMins > 0 -> "$hours ghanta $remainingMins minute"
            hours > 0 -> if (hours == 1) "1 ghante" else "$hours ghante"
            else -> "$minutes minute"
        }
    }

    private var lastEnforcementTime = 0L

    fun onAccessibilityEvent(service: ZoyaAccessibilityService, event: android.view.accessibility.AccessibilityEvent) {
        val current = _state.value
        if (!current.isActive || current.mode == StudyModeType.NONE) return

        val packageName = event.packageName?.toString() ?: ""
        if (packageName.isBlank() || packageName == service.packageName) return

        val now = System.currentTimeMillis()
        if (now - lastEnforcementTime < 2500L) return

        if (current.mode == StudyModeType.RESTRICTED) {
            handleRestrictedModeEvent(service, event, packageName, now)
        } else if (current.mode == StudyModeType.NORMAL) {
            handleNormalModeEvent(service, event, packageName, now)
        }
    }

    private fun handleRestrictedModeEvent(
        service: ZoyaAccessibilityService,
        event: android.view.accessibility.AccessibilityEvent,
        packageName: String,
        now: Long
    ) {
        val current = _state.value

        // 1. Direct Restrict Distraction Apps
        if (RESTRICTED_PACKAGES.contains(packageName)) {
            lastEnforcementTime = now
            val handled = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            Handler(Looper.getMainLooper()).postDelayed({
                val currentPkg = service.rootInActiveWindow?.packageName?.toString() ?: ""
                val isVerifiedClosed = currentPkg != packageName
                if (isVerifiedClosed || handled) {
                    Toast.makeText(service, "Boss, Study Restricted Mode active hai. Ye app allowed nahi hai.", Toast.LENGTH_SHORT).show()
                }
            }, 300)
            return
        }

        // 2. YouTube Smart Filtering / Complete Restriction
        if (packageName.contains("youtube")) {
            // OPTION B: YouTube Restricted Mode -> Complete app block!
            if (current.youtubePolicy == YouTubePolicy.RESTRICTED) {
                lastEnforcementTime = now
                val handled = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentPkg = service.rootInActiveWindow?.packageName?.toString() ?: ""
                    val isVerifiedClosed = !currentPkg.contains("youtube")
                    if (isVerifiedClosed || handled) {
                        Toast.makeText(service, "Boss, Study Restricted Mode active hai. YouTube is mode me allowed nahi hai.", Toast.LENGTH_SHORT).show()
                    }
                }, 300)
                return
            }

            // OPTION A: YouTube Study Mode -> Educational ALLOW, Shorts & Pure Distractions RESTRICT
            val root = service.rootInActiveWindow ?: return
            val (isShorts, isDistractingVideo) = analyzeYouTubeContent(root)
            if (isShorts || isDistractingVideo) {
                lastEnforcementTime = now
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                Toast.makeText(service, "Boss, ye padhai se related nahi hai. Study Restricted Mode active hai.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 3. Browser Filtering (Study ALLOW, Social / Entertainment Web RESTRICT)
        if (packageName.contains("chrome") || packageName.contains("browser")) {
            val root = service.rootInActiveWindow ?: return
            if (isBrowserDistraction(root)) {
                lastEnforcementTime = now
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                Toast.makeText(service, "Boss, ye padhai se related nahi hai. Study Restricted Mode active hai.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNormalModeEvent(
        service: ZoyaAccessibilityService,
        event: android.view.accessibility.AccessibilityEvent,
        packageName: String,
        now: Long
    ) {
        if (RESTRICTED_PACKAGES.contains(packageName)) {
            lastEnforcementTime = now
            Toast.makeText(service, "Boss, ye padhai se related nahi lag raha. Study Mode active hai.", Toast.LENGTH_SHORT).show()
            return
        }

        if (packageName.contains("youtube")) {
            val root = service.rootInActiveWindow ?: return
            val (isShorts, isDistractingVideo) = analyzeYouTubeContent(root)
            if (isShorts || isDistractingVideo) {
                lastEnforcementTime = now
                Toast.makeText(service, "Boss, ye padhai se related nahi lag raha. Study Mode active hai.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeYouTubeContent(root: android.view.accessibility.AccessibilityNodeInfo): Pair<Boolean, Boolean> {
        val texts = mutableListOf<String>()
        extractNodeTexts(root, texts, maxDepth = 4)

        val fullText = texts.joinToString(" ").lowercase(Locale.ROOT)

        // Check for YouTube Shorts
        val isShorts = fullText.contains("shorts") && (fullText.contains("remix") || fullText.contains("subscribe") || fullText.contains("dislike"))

        // Educational terms (Study Content: ALLOW)
        val educationalKeywords = listOf(
            "physics", "chemistry", "biology", "math", "maths", "algebra", "calculus",
            "lecture", "tutorial", "class", "course", "study", "exam", "preparation",
            "jee", "neet", "upsc", "gate", "coding", "programming", "science", "history",
            "english", "notes", "syllabus", "lesson", "chapter", "academy", "documentary",
            "cbse", "ncert", "revision"
        )
        val hasEducational = educationalKeywords.any { fullText.contains(it) }

        // Distracting keywords (Unrelated Entertainment: RESTRICT in Restricted Mode)
        val distractingKeywords = listOf(
            "comedy", "standup comedy", "prank", "roast", "funny video", "meme",
            "gaming", "gameplay", "bgmi", "pubg", "free fire", "gta",
            "trailer", "movie scene", "roasting"
        )
        val hasDistracting = distractingKeywords.any { fullText.contains(it) }

        val isDistractingVideo = hasDistracting && !hasEducational
        return Pair(isShorts, isDistractingVideo)
    }

    private fun isBrowserDistraction(root: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        val texts = mutableListOf<String>()
        extractNodeTexts(root, texts, maxDepth = 3)
        val fullText = texts.joinToString(" ").lowercase(Locale.ROOT)

        val distractionDomains = listOf(
            "instagram.com", "facebook.com", "tiktok.com", "reddit.com", "twitter.com", "x.com"
        )
        return distractionDomains.any { fullText.contains(it) }
    }

    private fun extractNodeTexts(
        node: android.view.accessibility.AccessibilityNodeInfo?,
        result: MutableList<String>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (node == null || currentDepth > maxDepth || result.size > 25) return

        val text = node.text?.toString()
        if (!text.isNullOrBlank()) result.add(text)
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) result.add(desc)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractNodeTexts(child, result, maxDepth, currentDepth + 1)
            }
        }
    }
}
