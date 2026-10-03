package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.web.WebsiteBuilderManager

/**
 * Background Glowing Code Matrix Stream that lives on the Home Screen background.
 * Streams code line-by-line like a human and auto-scrolls upwards as code grows.
 */
@Composable
fun WebsiteBackgroundCodeMatrix(
    modifier: Modifier = Modifier
) {
    val codeLines by WebsiteBuilderManager.codeLines.collectAsState()
    val isWritingCode by WebsiteBuilderManager.isWritingCode.collectAsState()
    val isCompleted by WebsiteBuilderManager.isCompleted.collectAsState()
    val projectTitle by WebsiteBuilderManager.currentProjectTitle.collectAsState()

    if (codeLines.isEmpty()) return

    val listState = rememberLazyListState()

    // Auto-scroll upward as lines of code are written so newest line is always visible
    LaunchedEffect(codeLines.size) {
        if (codeLines.isNotEmpty()) {
            listState.animateScrollToItem(codeLines.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "matrixGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xDB08051A)) // Dark translucent backdrop
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = glowAlpha),
                        Color(0xFFFF2A85).copy(alpha = glowAlpha * 0.7f),
                        Color(0xFF00FF88).copy(alpha = glowAlpha * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0xFF00F0FF),
                ambientColor = Color(0xFFFF2A85)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Matrix Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF4560)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFEB019)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF00E396)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⚡ Maya Live Code: $projectTitle.html",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isWritingCode) Color(0xFF00FF88).copy(alpha = 0.2f)
                                else Color(0xFFFF2A85).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isWritingCode) "WRITING LIVE ▋" else "READY IN CHROME",
                            color = if (isWritingCode) Color(0xFF00FF88) else Color(0xFFFF2A85),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live glowing lines list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(codeLines) { index, line ->
                    GlowingCodeLine(lineNumber = index + 1, code = line)
                }

                if (isWritingCode) {
                    item {
                        Text(
                            text = "  ▋",
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
}

/**
 * Floating Neon Status Pill at bottom of screen with "Open in Chrome" & Fullscreen view.
 */
@Composable
fun WebsiteFloatingStatusBar(
    onExpandCodeView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val codeLines by WebsiteBuilderManager.codeLines.collectAsState()
    val isWritingCode by WebsiteBuilderManager.isWritingCode.collectAsState()
    val isCompleted by WebsiteBuilderManager.isCompleted.collectAsState()
    val projectTitle by WebsiteBuilderManager.currentProjectTitle.collectAsState()

    if (codeLines.isEmpty()) return

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF00D0922))
                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onExpandCodeView() }
                ) {
                    Text(
                        text = if (isWritingCode) "⚡" else "🌐",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "$projectTitle (${codeLines.size} lines)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = if (isWritingCode) "Maya coding like a human..." else "Ready! Tap to open in Chrome",
                            color = if (isWritingCode) Color(0xFF00FF88) else Color(0xFF00F0FF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00F0FF).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(10.dp))
                            .clickable { WebsiteBuilderManager.openInChrome(context) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in Chrome",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Chrome 🌐",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onExpandCodeView,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand Full Code",
                            tint = Color(0xFFA59DC2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen Glowing Code View Modal
 */
@Composable
fun WebsiteFullscreenCodeModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val codeLines by WebsiteBuilderManager.codeLines.collectAsState()
    val isWritingCode by WebsiteBuilderManager.isWritingCode.collectAsState()
    val projectTitle by WebsiteBuilderManager.currentProjectTitle.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(codeLines.size) {
        if (codeLines.isNotEmpty()) {
            listState.animateScrollToItem(codeLines.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5060410))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B081B))
                .border(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "⚡ Maya Live Coding: $projectTitle.html",
                        color = Color(0xFF00F0FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isWritingCode) "● Status: Writing code live (auto-scrolling up)" else "● Status: Ready & Live in Chrome",
                        color = if (isWritingCode) Color(0xFF00FF88) else Color(0xFFFF2A85),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00F0FF).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(10.dp))
                            .clickable { WebsiteBuilderManager.openInChrome(context) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Open in Chrome 🌐",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF05030E))
                    .padding(12.dp)
            ) {
                itemsIndexed(codeLines) { index, line ->
                    GlowingCodeLine(lineNumber = index + 1, code = line)
                }
            }
        }
    }
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
