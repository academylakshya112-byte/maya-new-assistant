package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.live.LiveSessionManager
import com.example.live.ZoyaState
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class ZoyaForegroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    lateinit var liveSessionManager: LiveSessionManager
    private lateinit var toolEngine: ToolExecutionEngine

    private var isRecording = false

    // Configuration for Gemini Live Audio (16kHz, Mono, PCM 16-bit)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

    // Configuration for audio output (24kHz, Mono, PCM 16-bit is typical for server output)
    private val outputSampleRate = 24000
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val outBufferSize = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat) * 4

    companion object {
        var currentState: ZoyaState = ZoyaState.IDLE
            private set
        var onStateChange: ((ZoyaState) -> Unit)? = null

        private val _messages = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
        val messages: kotlinx.coroutines.flow.StateFlow<List<String>> = _messages.asStateFlow()

        // Provide a way to send message from UI to active service if it exists
        var activeService: ZoyaForegroundService? = null

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ZoyaForegroundService::class.java).apply {
                    action = "STOP"
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("ZoyaService", "Error stopping service via intent", e)
            }
            try {
                activeService?.stopSelf()
            } catch (e: Exception) {
                Log.e("ZoyaService", "Error calling stopSelf", e)
            }
            activeService = null
            currentState = ZoyaState.IDLE
            onStateChange?.invoke(ZoyaState.IDLE)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            activeService = this
            toolEngine = ToolExecutionEngine(this)
            
            val onAudioOut: (ByteArray) -> Unit = { audioData ->
                playAudio(audioData)
            }
            
            val onInterruptOut: () -> Unit = {
                try {
                    audioOutputQueue.clear()
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.pause()
                        audioTrack?.flush()
                        audioTrack?.play()
                    }
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Error flushing track on interrupt", e)
                }
            }
            
            liveSessionManager = LiveSessionManager(this, toolEngine, onAudioOut, onInterruptOut)

            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } catch (e: Exception) {
                    try { startForeground(1, createNotification()) } catch(e: Exception) { }
                }
            } else {
                try { startForeground(1, createNotification()) } catch(e: Exception) { }
            }

            scope.launch {
                liveSessionManager.zoyaState.collect { state ->
                    currentState = state
                    onStateChange?.invoke(state)
                    com.example.overlay.FloatingOrbManager.updateState(state)
                }
            }
            scope.launch {
                liveSessionManager.messages.collect { msgList ->
                    _messages.value = msgList
                }
            }

            initAudioTrack()
            startMicrophoneLoop()
            liveSessionManager.startSession()
            com.example.overlay.FloatingOrbManager.show(this)
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error in onCreate", e)
        }
    }

    private val audioOutputQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
    private var isAudioPlaybackActive = false

    private fun startAudioPlaybackLoop() {
        isAudioPlaybackActive = true
        scope.launch(Dispatchers.IO) {
            while (isActive && isAudioPlaybackActive) {
                try {
                    // Instantaneous wake-up with 0ms delay as soon as audio arrives
                    val data = audioOutputQueue.take()
                    if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.play()
                    }
                    audioTrack?.write(data, 0, data.size)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Playback loop error", e)
                }
            }
        }
    }

    private fun initAudioTrack() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 2 else 4096
            
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(outputSampleRate)
                        .setChannelMask(outChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
                
            audioTrack = builder.build()
            audioTrack?.play()
            startAudioPlaybackLoop()
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error initializing AudioTrack", e)
        }
    }

    private fun playAudio(data: ByteArray) {
        try {
            audioOutputQueue.offer(data)
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error queueing audio", e)
        }
    }

    private fun startMicrophoneLoop() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("ZoyaDiagnostic", "Missing RECORD_AUDIO permission")
            return
        }

        try {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 2 else 4096
            
            Log.i("ZoyaDiagnostic", "Starting low-latency microphone recording. bufSize=$finalBuf")

            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                createAttributionContext("zoya_audio")
            } else {
                this
            }
            audioRecord = AudioRecord.Builder()
                .setContext(ctx)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("ZoyaDiagnostic", "AudioRecord initialization failed!")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            scope.launch(Dispatchers.IO) {
                // Ultra-low latency 40ms streaming chunk (640 samples at 16kHz)
                val chunkSize = 640
                val audioBuffer = ShortArray(chunkSize)
                var readCount = 0
                while (isActive && isRecording) {
                    try {
                        val readResult = audioRecord?.read(audioBuffer, 0, chunkSize) ?: 0
                        if (readResult > 0) {
                            if (readCount % 50 == 0) {
                                Log.v("ZoyaDiagnostic", "Microphone read loop active. readResult=$readResult")
                            }
                            readCount++
                            processAudio(audioBuffer, readResult)
                        } else {
                            Log.e("ZoyaDiagnostic", "Microphone read failed or empty: $readResult")
                        }
                    } catch (e: Exception) {
                        Log.e("ZoyaDiagnostic", "Error reading audio", e)
                    }
                }
                Log.i("ZoyaDiagnostic", "Microphone loop stopped.")
            }
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error starting microphone", e)
        }
    }

    private var isUserSpeaking = false
    private var silenceChunkCount = 0
    private var speechChunkCount = 0
    private var lastFlushTime = 0L

    private fun processAudio(buffer: ShortArray, length: Int) {
        val state = liveSessionManager.zoyaState.value
        
        if (state == ZoyaState.IDLE) {
            val now = System.currentTimeMillis()
            if (now - lastFlushTime > 3000) {
                lastFlushTime = now
                liveSessionManager.startSession()
            }
            return
        }

        val prefs = getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val echoGuardEnabled = prefs.getBoolean("echo_guard", true)

        var sum = 0L
        for (i in 0 until length) {
            sum += abs(buffer[i].toLong())
        }
        val avg = if (length > 0) sum / length else 0

        // When Maya is speaking
        if (state == ZoyaState.SPEAKING) {
            if (echoGuardEnabled) {
                // If user speaks loudly to interrupt Maya
                if (avg > 3200) {
                    liveSessionManager.sendAudioData(buffer, length)
                }
            } else {
                liveSessionManager.sendAudioData(buffer, length)
            }
            isUserSpeaking = false
            silenceChunkCount = 0
            speechChunkCount = 0
            return
        }

        // When Maya is listening or thinking
        val isVoice = avg > 450
        if (isVoice) {
            speechChunkCount++
            silenceChunkCount = 0
            if (speechChunkCount >= 2) {
                isUserSpeaking = true
            }
            liveSessionManager.sendAudioData(buffer, length)
        } else {
            speechChunkCount = 0
            if (isUserSpeaking) {
                silenceChunkCount++
                // Stream trailing 320ms (8 chunks) so trailing speech + natural silence are smoothly sent
                if (silenceChunkCount <= 8) {
                    liveSessionManager.sendAudioData(buffer, length)
                } else {
                    isUserSpeaking = false
                    silenceChunkCount = 0
                }
            } else {
                // Idle silence: send keepalive chunk every 160ms (every 4th chunk)
                silenceChunkCount++
                if (silenceChunkCount % 4 == 0) {
                    liveSessionManager.sendAudioData(buffer, length)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendTextMessage(text: String) {
        liveSessionManager.sendTextMessage(text)
    }

    fun reconnectSession() {
        liveSessionManager.startSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeService = null
        isRecording = false
        isAudioPlaybackActive = false
        currentState = ZoyaState.IDLE
        onStateChange?.invoke(currentState)
        com.example.overlay.FloatingOrbManager.hide()
        audioOutputQueue.clear()
        try { audioRecord?.stop() } catch (e: Exception) {}
        try { audioRecord?.release() } catch (e: Exception) {}
        try { audioTrack?.stop() } catch (e: Exception) {}
        try { audioTrack?.release() } catch (e: Exception) {}
        try { liveSessionManager.stopSession() } catch (e: Exception) {}
        job.cancel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "ZOYA_CHANNEL",
                "Maya Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ZoyaForegroundService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            100,
            stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, "ZOYA_CHANNEL")
            .setContentTitle("Maya is active 💖")
            .setContentText("Listening in background. Tap Turn Off to stop.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off", stopPendingIntent)
            .build()
    }
}
