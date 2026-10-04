package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.web.WebsiteBuilderManager

/**
 * Fullscreen Human-Like Code Terminal that displays exclusively on Maya's Home Screen
 * when the user asks Maya to create or modify a website.
 * Streams code line-by-line like a real human software engineer typing in real-time.
 * NO pop-up dialogs, NO automatic Chrome launch popups.
 */
@Composable
fun WebsiteFullScreenCodeTerminal(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val codeLines by WebsiteBuilderManager.codeLines.collectAsState()
    val isWritingCode by WebsiteBuilderManager.isWritingCode.collectAsState()
    val isCompleted by WebsiteBuilderManager.isCompleted.collectAsState()
    val projectTitle by WebsiteBuilderManager.currentProjectTitle.collectAsState()
    val statusText by WebsiteBuilderManager.statusText.collectAsState()

    var copiedToClipboard by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll upward so newly written lines are always visible like human typing
    LaunchedEffect(codeLines.size) {
        if (codeLines.isNotEmpty()) {
            listState.animateScrollToItem(codeLines.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "terminalPulse")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060411))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF090618))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00F0FF).copy(alpha = borderGlowAlpha),
                            Color(0xFFFF2A85).copy(alpha = borderGlowAlpha * 0.7f),
                            Color(0xFF00FF88).copy(alpha = borderGlowAlpha * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color(0xFF00F0FF),
                    ambientColor = Color(0xFFFF2A85)
                )
                .padding(14.dp)
        ) {
            // --- TOP HEADER BAR: Terminal Controls & Live Status ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Window controls (Red, Amber, Green) + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF4560)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFEB019)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF00E396)))
                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Code Terminal",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "⚡ $projectTitle.html",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Action buttons: Copy, optional Run, and Close (NO popups)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy code button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B1536))
                            .border(1.dp, Color(0xFF382F66), RoundedCornerShape(8.dp))
                            .clickable {
                                val allCode = codeLines.joinToString("\n")
                                clipboardManager.setText(AnnotatedString(allCode))
                                copiedToClipboard = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = if (copiedToClipboard) Color(0xFF00FF88) else Color(0xFFA59DC2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedToClipboard) "Copied" else "Copy",
                                color = if (copiedToClipboard) Color(0xFF00FF88) else Color(0xFFA59DC2),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Optional manual Run button (user-initiated only, no automatic popup)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2B00F0FF))
                            .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(8.dp))
                            .clickable {
                                WebsiteBuilderManager.openInChrome(context)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in Browser",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Close button to return back to Maya's home screen
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Code Screen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- STATUS SUBHEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isWritingCode) Color(0xFF00FF88) else Color(0xFF00F0FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWritingCode) "Maya is typing code like a human programmer..." else "Complete • Ready to use",
                        color = if (isWritingCode) Color(0xFF00FF88) else Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${codeLines.size} lines",
                    color = Color(0xFF7E76A3),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // --- MAIN CODE DISPLAY: HUMAN-LIKE STREAMING TERMINAL ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF04020B))
                    .border(1.dp, Color(0xFF1E173D), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(codeLines) { index, line ->
                        GlowingCodeLine(lineNumber = index + 1, code = line)
                    }

                    if (isWritingCode) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${codeLines.size + 1} ".padStart(5, ' '),
                                    color = Color(0xFF4A4468),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "▋",
                                    color = Color(0xFF00F0FF).copy(alpha = cursorAlpha),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM STATUS BAR ---
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UTF-8  |  HTML5 + CSS3 + JavaScript",
                    color = Color(0xFF5D567B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Maya AI Live Coding Engine",
                    color = Color(0xFF00F0FF).copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Background Glowing Code Matrix Stream (preserved for backward compatibility).
 */
@Composable
fun WebsiteBackgroundCodeMatrix(
    modifier: Modifier = Modifier
) {
    WebsiteFullScreenCodeTerminal(
        onClose = { WebsiteBuilderManager.closeFullScreenCode() },
        modifier = modifier
    )
}

/**
 * Floating Neon Status Pill (preserved for backward compatibility).
 */
@Composable
fun WebsiteFloatingStatusBar(
    onExpandCodeView: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Kept for backward compatibility
}

/**
 * Fullscreen Glowing Code View Modal (preserved for backward compatibility).
 */
@Composable
fun WebsiteFullscreenCodeModal(
    onDismiss: () -> Unit
) {
    WebsiteFullScreenCodeTerminal(onClose = onDismiss)
}

@Composable
fun GlowingCodeLine(lineNumber: Int, code: String) {
    val trimmed = code.trim()
    val glowColor = when {
        trimmed.startsWith("<") && (trimmed.endsWith(">") || trimmed.contains("</")) -> Color(0xFF00F0FF) // Neon Cyan
        trimmed.contains("function ") || trimmed.contains("const ") || trimmed.contains("let ") || trimmed.contains("var ") -> Color(0xFFFF2A85) // Neon Pink
        trimmed.contains("style") || trimmed.contains("color:") || trimmed.contains("background") -> Color(0xFFFEB019) // Neon Amber
        trimmed.contains("{") || trimmed.contains("}") -> Color(0xFF00FF88) // Neon Green
        trimmed.startsWith("//") || trimmed.startsWith("/*") -> Color(0xFF6B7280) // Grey comment
        else -> Color(0xFFE2E8F0) // Off white
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$lineNumber ".padStart(5, ' '),
            color = Color(0xFF4A4468),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = code,
            color = glowColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
