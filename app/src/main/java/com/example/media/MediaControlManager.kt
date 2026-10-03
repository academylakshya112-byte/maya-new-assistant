package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class MediaInfo(
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val playbackStateStr: String,
    val durationMs: Long,
    val positionMs: Long,
    val packageName: String,
    val appName: String,
    val canSeek: Boolean
) {
    val formattedPosition: String
        get() = formatDuration(positionMs)

    val formattedDuration: String
        get() = formatDuration(durationMs)

    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    private fun formatDuration(millis: Long): String {
        if (millis <= 0) return "--:--"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

data class MediaActionResult(
    val success: Boolean,
    val message: String
)

object MediaControlManager {

    private const val TAG = "MediaControlManager"

    private val _currentMediaInfo = MutableStateFlow<MediaInfo?>(null)
    val currentMediaInfo: StateFlow<MediaInfo?> = _currentMediaInfo.asStateFlow()

    private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var isListenerRegistered = false

    fun init(context: Context) {
        updateActiveMediaInfo(context)
        registerSessionListener(context)
    }

    private fun getMediaSessionManager(context: Context): MediaSessionManager? {
        return context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    private fun registerSessionListener(context: Context) {
        if (isListenerRegistered) return
        try {
            val sessionManager = getMediaSessionManager(context) ?: return
            val compName = ComponentName(context, MayaNotificationListenerService::class.java)
            sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                processControllers(context, controllers)
            }
            sessionManager.addOnActiveSessionsChangedListener(sessionListener!!, compName)
            isListenerRegistered = true
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification listener permission not yet granted for session tracking: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering session listener: ${e.message}")
        }
    }

    fun updateActiveMediaInfo(context: Context) {
        try {
            val controller = getActiveMediaController(context)
            if (controller != null) {
                val metadata = controller.metadata
                val playbackState = controller.playbackState
                val stateInt = playbackState?.state ?: PlaybackState.STATE_NONE
                val isPlaying = (stateInt == PlaybackState.STATE_PLAYING || stateInt == PlaybackState.STATE_FAST_FORWARDING || stateInt == PlaybackState.STATE_REWINDING)

                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: "Unknown Title"
                val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                    ?: ""
                val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1L

                var currentPosition = playbackState?.position ?: 0L
                if (isPlaying && playbackState != null && playbackState.lastPositionUpdateTime > 0) {
                    val timeDelta = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
                    currentPosition += (timeDelta * playbackState.playbackSpeed).toLong()
                }

                val actions = playbackState?.actions ?: 0L
                val canSeek = (actions and PlaybackState.ACTION_SEEK_TO) != 0L

                val pkgName = controller.packageName ?: ""
                val appName = getAppName(context, pkgName)

                val stateStr = when (stateInt) {
                    PlaybackState.STATE_PLAYING -> "Playing"
                    PlaybackState.STATE_PAUSED -> "Paused"
                    PlaybackState.STATE_BUFFERING -> "Buffering"
                    PlaybackState.STATE_STOPPED -> "Stopped"
                    PlaybackState.STATE_CONNECTING -> "Connecting"
                    else -> if (isPlaying) "Playing" else "Paused"
                }

                _currentMediaInfo.value = MediaInfo(
                    title = title,
                    artist = artist,
                    album = album,
                    isPlaying = isPlaying,
                    playbackStateStr = stateStr,
                    durationMs = duration,
                    positionMs = currentPosition.coerceAtLeast(0L),
                    packageName = pkgName,
                    appName = appName,
                    canSeek = canSeek
                )
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating active media info: ${e.message}")
        }

        // Fallback check audio manager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isMusicActive = am?.isMusicActive == true
        if (isMusicActive) {
            _currentMediaInfo.value = MediaInfo(
                title = "Audio Playing",
                artist = "",
                album = "",
                isPlaying = true,
                playbackStateStr = "Playing",
                durationMs = -1L,
                positionMs = 0L,
                packageName = "",
                appName = "Media Player",
                canSeek = false
            )
        } else {
            _currentMediaInfo.value = null
        }
    }

    private fun processControllers(context: Context, controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            _currentMediaInfo.value = null
            return
        }
        val activeController = controllers.firstOrNull { c ->
            val s = c.playbackState?.state
            s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        if (activeController != null) {
            updateActiveMediaInfo(context)
        } else {
            _currentMediaInfo.value = null
        }
    }

    private fun getActiveMediaController(context: Context): MediaController? {
        val sessionManager = getMediaSessionManager(context) ?: return null
        try {
            val compName = ComponentName(context, MayaNotificationListenerService::class.java)
            val controllers = sessionManager.getActiveSessions(compName)
            if (!controllers.isNullOrEmpty()) {
                // Priority 1: Currently Playing or Buffering session
                val playing = controllers.firstOrNull {
                    val s = it.playbackState?.state
                    s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
                }
                if (playing != null) return playing

                // Priority 2: First active session
                return controllers.firstOrNull()
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "MediaSession active sessions security exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active sessions: ${e.message}")
        }
        return null
    }

    private fun getAppName(context: Context, packageName: String): String {
        if (packageName.isBlank()) return "Media Player"
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }

    // ==========================================
    // 1. BASIC PLAYBACK ACTIONS
    // ==========================================

    fun play(context: Context): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.play()
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "चल गया।")
            } catch (e: Exception) {
                Log.e(TAG, "Controller play failed: ${e.message}")
            }
        }
        // Fallback: Hardware Media Key Event
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        updateActiveMediaInfo(context)
        return if (dispatched) MediaActionResult(true, "चल गया।")
        else MediaActionResult(false, "मैं इसे अभी control नहीं कर पाई।")
    }

    fun pause(context: Context): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.pause()
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "Pause कर दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Controller pause failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
        updateActiveMediaInfo(context)
        return if (dispatched) MediaActionResult(true, "Pause कर दिया।")
        else MediaActionResult(false, "मैं इसे अभी control नहीं कर पाई।")
    }

    fun resume(context: Context): MediaActionResult {
        return play(context)
    }

    fun stop(context: Context): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.stop()
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "Stop कर दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Controller stop failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_STOP)
        updateActiveMediaInfo(context)
        return if (dispatched) MediaActionResult(true, "Stop कर दिया।")
        else MediaActionResult(false, "मैं इसे अभी control नहीं कर पाई।")
    }

    fun nextTrack(context: Context): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.skipToNext()
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "अगला track चला दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Controller skipToNext failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        updateActiveMediaInfo(context)
        return if (dispatched) MediaActionResult(true, "अगला track चला दिया।")
        else MediaActionResult(false, "मैं इसे अभी control नहीं कर पाई।")
    }

    fun previousTrack(context: Context): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.skipToPrevious()
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "पिछला track चला दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Controller skipToPrevious failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        updateActiveMediaInfo(context)
        return if (dispatched) MediaActionResult(true, "पिछला track चला दिया।")
        else MediaActionResult(false, "मैं इसे अभी control नहीं कर पाई।")
    }

    // ==========================================
    // 2. SEEK CONTROLS
    // ==========================================

    fun seekForward(context: Context, seconds: Int): MediaActionResult {
        val actualSeconds = if (seconds <= 0) 15 else seconds
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                val state = controller.playbackState
                val currentPos = state?.position ?: 0L
                val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1L
                val targetPos = if (duration > 0) (currentPos + actualSeconds * 1000L).coerceAtMost(duration)
                else currentPos + actualSeconds * 1000L

                controller.transportControls.seekTo(targetPos)
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "$actualSeconds सेकंड आगे कर दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Seek forward failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        return if (dispatched) MediaActionResult(true, "$actualSeconds सेकंड आगे कर दिया।")
        else MediaActionResult(false, "Maya अभी इस media player में seek नहीं कर सकती।")
    }

    fun seekBackward(context: Context, seconds: Int): MediaActionResult {
        val actualSeconds = if (seconds <= 0) 15 else seconds
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                val state = controller.playbackState
                val currentPos = state?.position ?: 0L
                val targetPos = (currentPos - actualSeconds * 1000L).coerceAtLeast(0L)

                controller.transportControls.seekTo(targetPos)
                updateActiveMediaInfo(context)
                return MediaActionResult(true, "$actualSeconds सेकंड पीछे कर दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Seek backward failed: ${e.message}")
            }
        }
        val dispatched = sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_REWIND)
        return if (dispatched) MediaActionResult(true, "$actualSeconds सेकंड पीछे कर दिया।")
        else MediaActionResult(false, "Maya अभी इस media player में seek नहीं कर सकती।")
    }

    fun seekTo(context: Context, positionMs: Long): MediaActionResult {
        val controller = getActiveMediaController(context)
        if (controller != null) {
            try {
                controller.transportControls.seekTo(positionMs)
                updateActiveMediaInfo(context)
                val totalSeconds = positionMs / 1000
                val min = totalSeconds / 60
                val sec = totalSeconds % 60
                val timeText = if (min > 0) "$min मिनट $sec सेकंड" else "$sec सेकंड"
                return MediaActionResult(true, "$timeText पर seek कर दिया।")
            } catch (e: Exception) {
                Log.e(TAG, "Seek to position failed: ${e.message}")
            }
        }
        return MediaActionResult(false, "Maya अभी इस media player में seek नहीं कर सकती।")
    }

    // ==========================================
    // 3. CURRENT MEDIA INFORMATION
    // ==========================================

    fun getCurrentMediaReport(context: Context): String {
        updateActiveMediaInfo(context)
        val info = _currentMediaInfo.value
        if (info == null || (!info.isPlaying && info.title == "Audio Playing")) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            return if (am?.isMusicActive == true) {
                "Phone par background audio active hai, par track title available nahi hai."
            } else {
                "अभी कोई media नहीं चल रहा है।"
            }
        }

        return buildString {
            append("🎵 Track: ${info.title}\n")
            if (info.artist.isNotBlank()) append("👤 Artist: ${info.artist}\n")
            if (info.album.isNotBlank()) append("💿 Album: ${info.album}\n")
            append("▶️ Status: ${info.playbackStateStr}\n")
            if (info.durationMs > 0) {
                append("⏱️ Position: ${info.formattedPosition} / ${info.formattedDuration}\n")
            }
            if (info.appName.isNotBlank()) append("📱 App: ${info.appName}")
        }
    }

    // ==========================================
    // 4. VOLUME CONTROL (SAFE & RELIABLE)
    // ==========================================

    fun adjustVolume(context: Context, direction: String): String {
        try {
            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.createAttributionContext("zoya_audio")
            } else context
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val stream = AudioManager.STREAM_MUSIC

            when (direction.lowercase()) {
                "up", "increase", "badao", "badhao", "raise" -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    return "Volume बढ़ा दिया।"
                }
                "down", "decrease", "kam", "lower" -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    return "Volume कम कर दिया।"
                }
                "mute", "silent", "shant" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    } else {
                        audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                    }
                    return "Mute कर दिया।"
                }
                "unmute", "sound_on" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    } else {
                        val maxVol = audioManager.getStreamMaxVolume(stream)
                        audioManager.setStreamVolume(stream, maxVol / 3, AudioManager.FLAG_SHOW_UI)
                    }
                    return "Unmute कर दिया।"
                }
                "max" -> {
                    val maxVol = audioManager.getStreamMaxVolume(stream)
                    audioManager.setStreamVolume(stream, maxVol, AudioManager.FLAG_SHOW_UI)
                    return "Volume full kar diya."
                }
                "half", "50" -> {
                    val maxVol = audioManager.getStreamMaxVolume(stream)
                    audioManager.setStreamVolume(stream, maxVol / 2, AudioManager.FLAG_SHOW_UI)
                    return "Volume 50% kar diya."
                }
                else -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    return "Volume बढ़ा दिया।"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting volume: ${e.message}")
            return "Volume control karne me error aayi."
        }
    }

    fun setVolumePercent(context: Context, percent: Int): String {
        try {
            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.createAttributionContext("zoya_audio")
            } else context
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val stream = AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(stream)

            // Safe clamping between 0 and 100
            val safePercent = percent.coerceIn(0, 100)
            val targetLevel = (maxVol * safePercent) / 100
            audioManager.setStreamVolume(stream, targetLevel, AudioManager.FLAG_SHOW_UI)
            return "Volume $safePercent% kar diya."
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume percent: ${e.message}")
            return "Volume set nahi ho paya."
        }
    }

    // ==========================================
    // HARDWARE MEDIA KEY EVENT HELPER
    // ==========================================

    private fun sendMediaKeyEvent(context: Context, keyCode: Int): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching media key event ($keyCode): ${e.message}")
            false
        }
    }
}
