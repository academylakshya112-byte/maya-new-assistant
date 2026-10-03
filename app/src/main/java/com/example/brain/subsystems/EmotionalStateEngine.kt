package com.example.brain.subsystems

import com.example.brain.model.EmotionalSimulationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Conversational Tone & State Simulation Engine.
 * Note: This is an adaptive response-style mechanism to maintain human-like warmth
 * and contextual continuity, NOT biological emotion or consciousness.
 */
object EmotionalStateEngine {

    private val _currentState = MutableStateFlow(EmotionalSimulationState.SUPPORTIVE)
    val currentState: StateFlow<EmotionalSimulationState> = _currentState.asStateFlow()

    fun updateStateFromContext(userMessage: String, taskSuccess: Boolean?) {
        val lower = userMessage.lowercase()
        val newState = when {
            taskSuccess == false -> EmotionalSimulationState.CONCERNED
            taskSuccess == true -> EmotionalSimulationState.EXCITED
            lower.contains("gussa") || lower.contains("sad") || lower.contains("upset") || lower.contains("dard") -> EmotionalSimulationState.SUPPORTIVE
            lower.contains("code") || lower.contains("math") || lower.contains("calculate") || lower.contains("solve") -> EmotionalSimulationState.FOCUSED
            lower.contains("kya") || lower.contains("kaise") || lower.contains("kyu") || lower.contains("why") -> EmotionalSimulationState.CURIOUS
            lower.contains("chup") || lower.contains("shant") || lower.contains("sleep") || lower.contains("rest") -> EmotionalSimulationState.CALM
            else -> EmotionalSimulationState.SUPPORTIVE
        }
        _currentState.value = newState
    }

    fun setState(state: EmotionalSimulationState) {
        _currentState.value = state
    }
}
