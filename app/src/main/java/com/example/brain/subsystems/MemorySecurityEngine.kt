package com.example.brain.subsystems

import android.util.Log

data class SecurityCheckResult(
    val isSafe: Boolean,
    val sanitizedContent: String,
    val violationReason: String? = null
)

object MemorySecurityEngine {

    private const val TAG = "MemorySecurityEngine"

    // Regex patterns for sensitive data
    private val PASSWORD_PATTERN = Regex("""(?i)\b(password|passwd|passcode|secret_key|api_key|token)\s*[:=]\s*\S+""")
    private val OTP_PATTERN = Regex("""(?i)\b(otp|one[- ]time[- ]password|verification code|pin)\s*(is|:|=)?\s*\d{4,8}\b""")
    private val API_KEY_PATTERN = Regex("""(AIza[0-9A-Za-z-_]{35}|ghp_[0-9A-Za-z]{36}|Bearer\s+[A-Za-z0-9\-._~+/]+=*)""")
    private val CARD_PATTERN = Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b""")
    private val CVV_PATTERN = Regex("""(?i)\b(cvv|cvc|security code)\s*[:=]?\s*\d{3,4}\b""")

    /**
     * Memory Firewall: Validates candidate memories before persistence.
     * Prevents storing sensitive credentials, OTPs, API keys, or financial secrets.
     */
    fun evaluate(key: String, content: String): SecurityCheckResult {
        val combined = "$key $content"

        if (PASSWORD_PATTERN.containsMatchIn(combined)) {
            Log.w(TAG, "Memory Firewall rejected entry: Contains password or auth secret.")
            return SecurityCheckResult(false, "", "Content contains password or credential secret.")
        }

        if (OTP_PATTERN.containsMatchIn(combined)) {
            Log.w(TAG, "Memory Firewall rejected entry: Contains temporary OTP or PIN.")
            return SecurityCheckResult(false, "", "Content contains temporary one-time password (OTP).")
        }

        if (API_KEY_PATTERN.containsMatchIn(combined)) {
            Log.w(TAG, "Memory Firewall rejected entry: Contains API key or token.")
            return SecurityCheckResult(false, "", "Content contains API key or bearer token.")
        }

        if (CARD_PATTERN.containsMatchIn(combined) || CVV_PATTERN.containsMatchIn(combined)) {
            Log.w(TAG, "Memory Firewall rejected entry: Contains credit card or banking number.")
            return SecurityCheckResult(false, "", "Content contains sensitive payment card or CVV.")
        }

        // Clean & sanitize non-sensitive text
        val sanitized = content.trim()
        return SecurityCheckResult(true, sanitized, null)
    }
}
