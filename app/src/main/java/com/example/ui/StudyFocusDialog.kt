package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study.ActivationResult
import com.example.study.DeactivationResult
import com.example.study.StudyFocusManager
import com.example.study.StudyModeType

@Composable
fun StudyFocusDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sessionState by StudyFocusManager.state.collectAsState()

    var selectedMode by remember { mutableStateOf(StudyModeType.NORMAL) }
    var selectedDurationMinutes by remember { mutableIntStateOf(60) }
    var showEmergencyConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (sessionState.isActive) {
                                    if (sessionState.mode == StudyModeType.RESTRICTED) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                                } else Color(0xFFEFF6FF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sessionState.mode == StudyModeType.RESTRICTED) Icons.Default.Lock else Icons.Default.Book,
                            contentDescription = "Study Focus",
                            tint = if (sessionState.mode == StudyModeType.RESTRICTED) Color(0xFFDC2626) else Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (sessionState.isActive) {
                                if (sessionState.mode == StudyModeType.RESTRICTED) "Restricted Focus Locked" else "Study Mode Active"
                            } else "Study Focus System",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Maya Focus Engine",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (sessionState.isActive && sessionState.mode != StudyModeType.NONE) {
                    // --- ACTIVE SESSION VIEW ---
                    val timerStr = sessionState.remainingFormatted
                    val isRestricted = sessionState.mode == StudyModeType.RESTRICTED

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (isRestricted) listOf(Color(0xFFFEF2F2), Color(0xFFFFF1F2))
                                    else listOf(Color(0xFFEFF6FF), Color(0xFFF0FDF4))
                                )
                            )
                            .border(
                                1.5.dp,
                                if (isRestricted) Color(0xFFFCA5A5) else Color(0xFF93C5FD),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isRestricted) "🛡️ STRICT TIME-LOCKED SESSION" else "📚 FLEXIBLE STUDY MODE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRestricted) Color(0xFFDC2626) else Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = timerStr,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Remaining Time (${sessionState.remainingSpokenHindi})",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            val endTimeFormatted = remember(sessionState.endTimeMillis) {
                                if (sessionState.endTimeMillis > 0) {
                                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sessionState.endTimeMillis))
                                } else "--"
                            }
                            Text(
                                text = "Ends at: $endTimeFormatted",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    // Policy summary & Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Status & Protection Details:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        if (isRestricted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Permission Status: ✅ Enforced via Accessibility", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restricted Apps: Instagram, Facebook, TikTok, Netflix & Reels", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Permission Status: ✅ Flexible Study Guard", fontSize = 11.sp, color = Color(0xFF15803D))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("YouTube educational lectures & notes allowed", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF reader, Chrome research & Maya AI allowed", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                        if (isRestricted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Social media, Games & Reels strictly locked", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Distraction warning if games or reels opened", fontSize = 11.sp, color = Color(0xFFD97706))
                            }
                        }
                    }

                    // Stop Session Button
                    if (isRestricted) {
                        OutlinedButton(
                            onClick = {
                                StudyFocusManager.requestEmergencyOverride()
                                showEmergencyConfirmDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Emergency Override Exit", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                StudyFocusManager.deactivateNormalMode()
                                onDismissRequest()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Stop Study Mode", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                } else {
                    // --- SETUP NEW STUDY SESSION VIEW ---
                    Text(
                        text = "Choose protection mode:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )

                    // Mode Selector Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Normal Mode Card
                        ModeOptionCard(
                            title = "Normal Study",
                            subtitle = "Flexible alerts, early exit allowed",
                            icon = Icons.Default.Book,
                            iconTint = Color(0xFF2563EB),
                            isSelected = selectedMode == StudyModeType.NORMAL,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = StudyModeType.NORMAL }
                        )

                        // Restricted Mode Card
                        ModeOptionCard(
                            title = "Restricted Mode",
                            subtitle = "Strict time-lock & app blocker",
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFFDC2626),
                            isSelected = selectedMode == StudyModeType.RESTRICTED,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = StudyModeType.RESTRICTED }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose duration:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )

                    // Quick duration chips
                    val durations = listOf(
                        Pair("30 min", 30),
                        Pair("1 hr", 60),
                        Pair("2 hrs", 120),
                        Pair("3 hrs", 180),
                        Pair("4 hrs", 240)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durations.forEach { (label, min) ->
                            val isChosen = selectedDurationMinutes == min
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                    .clickable { selectedDurationMinutes = min }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    // Explanatory note
                    Text(
                        text = if (selectedMode == StudyModeType.RESTRICTED) {
                            "⚠️ Restricted Mode locks distracting apps (Instagram, Snapchat, Games, Reels) for the full duration. Study content on YouTube & browser is allowed."
                        } else {
                            "💡 Normal Mode warns you if you drift into entertainment or social media, while keeping notes and study videos open."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Start Button
                    Button(
                        onClick = {
                            val policy = if (selectedMode == StudyModeType.RESTRICTED) {
                                com.example.study.YouTubePolicy.STUDY
                            } else {
                                com.example.study.YouTubePolicy.NONE
                            }
                            StudyFocusManager.activateMode(selectedMode, selectedDurationMinutes, policy)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == StudyModeType.RESTRICTED) Color(0xFFDC2626) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedMode == StudyModeType.RESTRICTED) Icons.Default.Lock else Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedMode == StudyModeType.RESTRICTED) "Lock In Restricted Mode 🛡️" else "Start Study Mode 📚",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )

    // Emergency Override Confirmation Dialog
    if (showEmergencyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Override", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Text(
                    "Restricted Study Mode is time-locked to protect your focus. Are you sure you want to stop this study session before the timer completes?",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        StudyFocusManager.confirmEmergencyOverride(confirmed = true)
                        showEmergencyConfirmDialog = false
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Override", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    StudyFocusManager.confirmEmergencyOverride(confirmed = false)
                    showEmergencyConfirmDialog = false
                }) {
                    Text("Stay Focused", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFFF8FAFC) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) iconTint else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Animated glowing banner for MayaHomeScreen when Study Mode is active.
 */
@Composable
fun StudyFocusActiveBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionState by StudyFocusManager.state.collectAsState()

    AnimatedVisibility(
        visible = sessionState.isActive && sessionState.mode != StudyModeType.NONE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val isRestricted = sessionState.mode == StudyModeType.RESTRICTED
        val timerStr = sessionState.remainingFormatted

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isRestricted) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                    )
                    .border(
                        1.dp,
                        if (isRestricted) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isRestricted) "🔒 Study Restricted · $timerStr" else "📚 Study Mode · $timerStr",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRestricted) Color(0xFFDC2626) else Color(0xFF1D4ED8)
                )
            }
        }
    }
}
