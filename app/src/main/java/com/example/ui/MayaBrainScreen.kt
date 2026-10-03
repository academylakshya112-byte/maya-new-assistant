package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brain.BrainEngine
import com.example.brain.model.MemoryCategory
import com.example.brain.model.MemoryConfidence
import com.example.brain.model.MemoryItem
import com.example.brain.subsystems.BrainHealthStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MayaBrainScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val isPaused by BrainEngine.isPaused.collectAsState()
    val allMemoriesFlow = remember { BrainEngine.getAllActiveMemoriesFlow() }
    val allMemories by allMemoriesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var brainHealth by remember { mutableStateOf<BrainHealthStatus?>(null) }

    val categories = remember {
        listOf("All") + MemoryCategory.entries.map { it.displayName } + listOf("Brain Health")
    }

    // Refresh health
    fun refreshHealth() {
        scope.launch {
            brainHealth = BrainEngine.getBrainHealth()
        }
    }

    val filteredMemories = remember(allMemories, searchQuery, selectedCategoryIndex) {
        allMemories.filter { item ->
            val matchesCategory = when (selectedCategoryIndex) {
                0 -> true
                categories.size - 1 -> false // Health tab
                else -> {
                    val catName = MemoryCategory.entries[selectedCategoryIndex - 1].name
                    item.category.equals(catName, ignoreCase = true)
                }
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.key.contains(searchQuery, ignoreCase = true) ||
                item.content.contains(searchQuery, ignoreCase = true) ||
                item.tags.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0716))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Maya Brain",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🧠",
                        fontSize = 18.sp
                    )
                }

                // Pause/Resume Button
                IconButton(
                    onClick = {
                        scope.launch {
                            val newPaused = BrainEngine.togglePauseMemory()
                            val msg = if (newPaused) "Brain memory paused." else "Brain memory active."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }

                // Export Button
                IconButton(
                    onClick = {
                        scope.launch {
                            val json = BrainEngine.exportAsJson()
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "Brain memory exported to clipboard (JSON)! 📋", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export JSON",
                        tint = Color(0xFFA78BFA)
                    )
                }

                // Import Button
                IconButton(
                    onClick = {
                        importJsonText = ""
                        showImportDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Import JSON",
                        tint = Color(0xFFA78BFA)
                    )
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memories, facts, preferences...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8B5CF6),
                    unfocusedBorderColor = Color(0xFF281C44),
                    focusedContainerColor = Color(0xFF150F25),
                    unfocusedContainerColor = Color(0xFF150F25),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Memory Status Banner (if paused)
            if (isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF332005))
                        .border(1.dp, Color(0xFFD97706), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⏸️ Brain Memory is currently paused. No new memories will be recorded.",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.5.sp
                    )
                }
            }

            // --- CATEGORY TABS ---
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFFF2A85),
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                        color = Color(0xFFFF2A85)
                    )
                }
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = {
                            selectedCategoryIndex = index
                            if (index == categories.size - 1) {
                                refreshHealth()
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedCategoryIndex == index) Color.White else Color(0xFFA59DC2),
                                fontSize = 13.sp,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- MAIN CONTENT ---
            if (selectedCategoryIndex == categories.size - 1) {
                // HEALTH DASHBOARD TAB
                BrainHealthView(
                    health = brainHealth,
                    onClearAll = { showClearConfirmDialog = true },
                    onRefresh = { refreshHealth() }
                )
            } else {
                // MEMORIES LIST
                if (filteredMemories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🧠",
                                fontSize = 42.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No memories match '$searchQuery'" else "No memories recorded in this section yet.",
                                color = Color(0xFFA59DC2),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredMemories, key = { it.id }) { item ->
                            MemoryCard(
                                memory = item,
                                onDelete = {
                                    scope.launch {
                                        BrainEngine.forget(item.id)
                                        Toast.makeText(context, "Deleted '${item.key}'", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button to manually add a memory
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFFF2A85),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Memory")
        }
    }

    // --- ADD MEMORY DIALOG ---
    if (showAddDialog) {
        var keyInput by remember { mutableStateOf("") }
        var contentInput by remember { mutableStateOf("") }
        var categorySelected by remember { mutableStateOf(MemoryCategory.PREFERENCES) }
        var importanceInput by remember { mutableIntStateOf(7) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF150F25),
            title = { Text("Remember New Fact / Preference", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Memory Key (e.g. favorite_color)", color = Color(0xFFA59DC2)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF281C44)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("Content / Rule", color = Color(0xFFA59DC2)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF281C44)
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contentInput.isNotBlank()) {
                            scope.launch {
                                val res = BrainEngine.remember(
                                    key = keyInput.ifBlank { "custom_preference_${System.currentTimeMillis()}" },
                                    content = contentInput,
                                    category = categorySelected,
                                    importance = importanceInput
                                )
                                Toast.makeText(context, res.second, Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85))
                ) {
                    Text("Save to Brain", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFFA59DC2))
                }
            }
        )
    }

    // --- IMPORT DIALOG ---
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Color(0xFF150F25),
            title = { Text("Import Brain Memories (JSON)", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste JSON backup of memories below:", color = Color(0xFFA59DC2), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("{\"memories\": [...]}", color = Color(0xFF6B7280)) },
                        modifier = Modifier.height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF281C44)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = BrainEngine.importFromJson(importJsonText)
                            Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                            if (res.first) showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Import", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color(0xFFA59DC2))
                }
            }
        )
    }

    // --- CLEAR ALL CONFIRMATION DIALOG ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color(0xFF150F25),
            title = { Text("Clear All Memories?", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all stored preferences and memories from Maya's Brain database.", color = Color(0xFFA59DC2))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            BrainEngine.clearAllMemories()
                            Toast.makeText(context, "All memories cleared.", Toast.LENGTH_SHORT).show()
                            showClearConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFFA59DC2))
                }
            }
        )
    }
}

@Composable
fun MemoryCard(
    memory: MemoryItem,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(memory.updatedAt) { dateFormat.format(Date(memory.updatedAt)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF150F25))
            .border(1.dp, Color(0xFF281C44), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = memory.category,
                        color = Color(0xFFA78BFA),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Importance Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF2A85).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Imp: ${memory.importance}/10",
                        color = Color(0xFFFF2A85),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Memory",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Key
            Text(
                text = memory.key,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Content
            Text(
                text = memory.content,
                color = Color(0xFFD1D5DB),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            if (!memory.exceptionContext.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Exception: ${memory.exceptionContext}",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Source: ${memory.source}",
                    color = Color(0xFF6B7280),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "• $formattedDate",
                    color = Color(0xFF6B7280),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun BrainHealthView(
    health: BrainHealthStatus?,
    onClearAll: () -> Unit,
    onRefresh: () -> Unit
) {
    if (health == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onRefresh) {
                Text("Load Brain Health Status")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Brain Health & Status Dashboard",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Metrics Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF150F25))
                    .border(1.dp, Color(0xFF281C44), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Memories:", color = Color(0xFFA59DC2), fontSize = 13.sp)
                        Text("${health.totalMemories}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Memories:", color = Color(0xFFA59DC2), fontSize = 13.sp)
                        Text("${health.activeMemories}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Memory Recording State:", color = Color(0xFFA59DC2), fontSize = 13.sp)
                        Text(if (health.isMemoryPaused) "Paused" else "Active", color = if (health.isMemoryPaused) Color(0xFFF59E0B) else Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Database Engine:", color = Color(0xFFA59DC2), fontSize = 13.sp)
                        Text(health.databaseStatus, color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Security Firewall:", color = Color(0xFFA59DC2), fontSize = 13.sp)
                        Text(health.securityStatus, color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Distribution by Category",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(health.categoryCounts.entries.toList()) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(entry.key, color = Color(0xFFA59DC2), fontSize = 12.sp)
                Text("${entry.value}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClearAll,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Brain Memories", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
