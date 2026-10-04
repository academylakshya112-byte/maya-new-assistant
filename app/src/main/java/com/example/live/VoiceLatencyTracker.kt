package com.example.live

import android.util.Log

object VoiceLatencyTracker {
    var t1: Long = 0L // User Speech End
    var t2: Long = 0L // Safety Dispatch
    var t3: Long = 0L // First Gemini Audio Chunk Received
    var t4: Long = 0L // AudioTrack Playback Started

    var isTrackingTurn = false
    var turnCounter = 0

    fun onSpeechEnded() {
        if (!isTrackingTurn) {
            t1 = System.nanoTime()
            t2 = 0L
            t3 = 0L
            t4 = 0L
            isTrackingTurn = true
            turnCounter++
            Log.i("VoiceLatencyTracker", "--- Start Turn $turnCounter Monotonic Capture ---")
            Log.i("VoiceLatencyTracker", "T1 (User Speech End) = $t1 ns")
        }
    }

    fun onSafetyDispatched() {
        if (isTrackingTurn && t2 == 0L) {
            t2 = System.nanoTime()
            val diffMs = (t2 - t1) / 1_000_000.0
            Log.i("VoiceLatencyTracker", "T2 (Safety Dispatch) = $t2 ns (T2 - T1 = ${String.format("%.2f", diffMs)} ms)")
        }
    }

    fun onFirstAudioReceived() {
        if (isTrackingTurn && t3 == 0L) {
            t3 = System.nanoTime()
            val diffMs = (t3 - t1) / 1_000_000.0
            Log.i("VoiceLatencyTracker", "T3 (First Audio Arrived) = $t3 ns (T3 - T1 = ${String.format("%.2f", diffMs)} ms)")
        }
    }

    fun onPlaybackStarted() {
        if (isTrackingTurn && t4 == 0L) {
            t4 = System.nanoTime()
            val t2t1 = if (t1 > 0 && t2 > 0) (t2 - t1) / 1_000_000.0 else 0.0
            val t3t1 = if (t1 > 0 && t3 > 0) (t3 - t1) / 1_000_000.0 else 0.0
            val t4t3 = if (t3 > 0) (t4 - t3) / 1_000_000.0 else 0.0
            val t4t1 = if (t1 > 0) (t4 - t1) / 1_000_000.0 else 0.0

            Log.i("VoiceLatencyTracker", "T4 (Playback Started) = $t4 ns (T4 - T1 = ${String.format("%.2f", t4t1)} ms)")
            Log.i("VoiceLatencyTracker", "=== TURN $turnCounter LATENCY SUMMARY ===")
            Log.i("VoiceLatencyTracker", "T2 - T1 (Gating) = ${String.format("%.2f", t2t1)} ms")
            Log.i("VoiceLatencyTracker", "T3 - T1 (Network/Inference) = ${String.format("%.2f", t3t1)} ms")
            Log.i("VoiceLatencyTracker", "T4 - T3 (Playback Init Overhead) = ${String.format("%.2f", t4t3)} ms")
            Log.i("VoiceLatencyTracker", "Total End-to-End Latency = ${String.format("%.2f", t4t1)} ms")
            Log.i("VoiceLatencyTracker", "-----------------------------------------")

            // Lock completed turn
            isTrackingTurn = false
        }
    }

    fun reset() {
        t1 = 0L
        t2 = 0L
        t3 = 0L
        t4 = 0L
        isTrackingTurn = false
    }
}
