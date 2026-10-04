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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.brain.model.ContextEdge
import com.example.brain.model.ContextNode
import com.example.brain.model.ContextNodeType
import com.example.brain.model.MemoryCategory
import com.example.brain.model.MemoryConfidence
import com.example.brain.model.MemoryItem
import com.example.brain.model.SkillEntity
import com.example.brain.subsystems.BrainHealthStatus
import kotlinx.coroutines.launch
import org.json.JSONArray
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
    
    // Top-Level Main Tabs: 0: Memories, 1: Context Map, 2: Skill Forge, 3: Health
    var mainTabIndex by remember { mutableIntStateOf(0) }
    val mainTabs = listOf("Memories 🧠", "Context Map 🗺️", "Skill Forge 🛠️", "Health 🛡️")

    val allMemoriesFlow = remember { BrainEngine.getAllActiveMemoriesFlow() }
    val allMemories by allMemoriesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val allNodesFlow = remember { BrainEngine.getAllContextNodesFlow() }
    val allNodes by allNodesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val allEdgesFlow = remember { BrainEngine.getAllContextEdgesFlow() }
    val allEdges by allEdgesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val allSkillsFlow = remember { BrainEngine.getAllSkillsFlow() }
    val allSkills by allSkillsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var brainHealth by remember { mutableStateOf<BrainHealthStatus?>(null) }

    val memoryCategories = remember {
        listOf("All") + MemoryCategory.entries.map { it.displayName }
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

    val filteredNodes = remember(allNodes, searchQuery) {
        if (searchQuery.isBlank()) allNodes else {
            allNodes.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.nodeType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredSkills = remember(allSkills, searchQuery) {
        if (searchQuery.isBlank()) allSkills else {
            allSkills.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
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

            // --- MAIN NAVIGATION TABS ---
            TabRow(
                selectedTabIndex = mainTabIndex,
                containerColor = Color(0xFF130D22),
                contentColor = Color(0xFFFF2A85),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[mainTabIndex]),
                        color = Color(0xFFFF2A85)
                    )
                }
            ) {
                mainTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = mainTabIndex == index,
                        onClick = {
                            mainTabIndex = index
                            if (index == 3) refreshHealth()
                        },
                        text = {
                            Text(
                                text = title,
                                color = if (mainTabIndex == index) Color.White else Color(0xFFA59DC2),
                                fontSize = 12.sp,
                                fontWeight = if (mainTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        when (mainTabIndex) {
                            1 -> "Search Context Map (nodes, features, problems)..."
                            2 -> "Search Skill Forge (workflows, verified skills)..."
                            else -> "Search memories, facts, preferences..."
                        },
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            // --- TAB CONTENT ---
            when (mainTabIndex) {
                0 -> {
                    // 1. MEMORIES TAB
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
                        memoryCategories.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        color = if (selectedCategoryIndex == index) Color.White else Color(0xFFA59DC2),
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredMemories.isEmpty()) {
                        EmptyBrainState("No memories match '$searchQuery'")
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
                1 -> {
                    // 2. CONTEXT / KNOWLEDGE MAP TAB
                    ContextMapView(
                        nodes = filteredNodes,
                        edges = allEdges,
                        onDeleteNode = { nodeId ->
                            scope.launch {
                                BrainEngine.deleteContextNode(nodeId)
                                Toast.makeText(context, "Node deleted from Context Map", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddNodeClick = { showAddNodeDialog = true }
                    )
                }
                2 -> {
                    // 3. SKILL FORGE TAB
                    SkillForgeView(
                        skills = filteredSkills,
                        onToggleStatus = { skillId ->
                            scope.launch {
                                val isNowActive = BrainEngine.toggleSkillStatus(skillId)
                                Toast.makeText(context, if (isNowActive) "Skill Enabled ✅" else "Skill Disabled ⏸️", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSkill = { skillId ->
                            scope.launch {
                                BrainEngine.deleteSkill(skillId)
                                Toast.makeText(context, "Skill removed from Forge", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                3 -> {
                    // 4. HEALTH DASHBOARD TAB
                    BrainHealthView(
                        health = brainHealth,
                        onClearAll = { showClearConfirmDialog = true },
                        onRefresh = { refreshHealth() }
                    )
                }
            }
        }

        // Floating Action Button
        if (mainTabIndex == 0) {
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
        } else if (mainTabIndex == 1) {
            FloatingActionButton(
                onClick = { showAddNodeDialog = true },
                containerColor = Color(0xFF8B5CF6),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Hub, contentDescription = "Add Context Node")
            }
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

    // --- ADD CONTEXT NODE DIALOG ---
    if (showAddNodeDialog) {
        var nodeTitle by remember { mutableStateOf("") }
        var nodeDesc by remember { mutableStateOf("") }
        var nodeTypeSelected by remember { mutableStateOf(ContextNodeType.FEATURE) }

        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            containerColor = Color(0xFF150F25),
            title = { Text("Add Context Map Node", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nodeTitle,
                        onValueChange = { nodeTitle = it },
                        label = { Text("Node Title (e.g. WhatsApp Automation)", color = Color(0xFFA59DC2)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF281C44)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nodeDesc,
                        onValueChange = { nodeDesc = it },
                        label = { Text("Description & Connection Context", color = Color(0xFFA59DC2)) },
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
                        if (nodeTitle.isNotBlank()) {
                            scope.launch {
                                BrainEngine.addContextNode(
                                    type = nodeTypeSelected,
                                    title = nodeTitle,
                                    description = nodeDesc,
                                    importance = 8
                                )
                                Toast.makeText(context, "Node added to Context Map! 🗺️", Toast.LENGTH_SHORT).show()
                                showAddNodeDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Add Node", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNodeDialog = false }) {
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
                Text("This will permanently delete stored memories from Maya's Brain database.", color = Color(0xFFA59DC2))
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
private fun EmptyBrainState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🧠", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = Color(0xFFA59DC2),
                fontSize = 14.sp
            )
        }
    }
}

// ==========================================
// CONTEXT / KNOWLEDGE MAP VIEW
// ==========================================

@Composable
fun ContextMapView(
    nodes: List<ContextNode>,
    edges: List<ContextEdge>,
    onDeleteNode: (String) -> Unit,
    onAddNodeClick: () -> Unit
) {
    if (nodes.isEmpty()) {
        EmptyBrainState("Context & Knowledge Map is empty.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF18102B))
                    .border(1.dp, Color(0xFF382361), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🗺️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Knowledge & Context Graph",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total ${nodes.size} nodes, ${edges.size} semantic relationships connecting problems, solutions & skills.",
                            color = Color(0xFFA59DC2),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        items(nodes, key = { it.id }) { node ->
            val icon = try { ContextNodeType.valueOf(node.nodeType).icon } catch (e: Exception) { "📌" }
            val connectedEdges = edges.filter { it.fromNodeId == node.id || it.toNodeId == node.id }

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
                        Text(text = icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = node.nodeType,
                                color = Color(0xFFA78BFA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Conf: ${node.confidence}",
                                color = Color(0xFF10B981),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { onDeleteNode(node.id) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Node",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = node.title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (node.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = node.description,
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Connected Edges
                    if (connectedEdges.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected Relations (${connectedEdges.size}):",
                            color = Color(0xFFA59DC2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        connectedEdges.take(4).forEach { edge ->
                            val isOutgoing = edge.fromNodeId == node.id
                            val otherId = if (isOutgoing) edge.toNodeId else edge.fromNodeId
                            val otherNode = nodes.find { it.id == otherId }
                            val targetName = otherNode?.title ?: "Related Node"
                            val arrow = if (isOutgoing) "──[${edge.relationshipType}]──>" else "<──[${edge.relationshipType}]──"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$arrow $targetName",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SKILL FORGE VIEW
// ==========================================

@Composable
fun SkillForgeView(
    skills: List<SkillEntity>,
    onToggleStatus: (String) -> Unit,
    onDeleteSkill: (String) -> Unit
) {
    if (skills.isEmpty()) {
        EmptyBrainState("Skill Forge has no learned workflows.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF13152B))
                    .border(1.dp, Color(0xFF282F61), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Skill Forge: Verified Workflows",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Maya reuses successfully verified multi-step procedures with strict verification rules.",
                            color = Color(0xFFA59DC2),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        items(skills, key = { it.skillId }) { skill ->
            var isExpanded by remember { mutableStateOf(false) }
            val isVerified = skill.status == "VERIFIED"
            val isDisabled = skill.status == "DISABLED"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDisabled) Color(0xFF0F0B18) else Color(0xFF150F25))
                    .border(
                        1.dp,
                        if (isVerified) Color(0xFF00FF88).copy(alpha = 0.5f) else Color(0xFF281C44),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🛠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isVerified) Color(0xFF00FF88).copy(alpha = 0.15f)
                                    else Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${skill.status} (v${skill.version})",
                                color = if (isVerified) Color(0xFF00FF88) else Color(0xFFA78BFA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = skill.category,
                                color = Color(0xFFE2E8F0),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { onToggleStatus(skill.skillId) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Toggle Status",
                                tint = if (isDisabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteSkill(skill.skillId) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Skill",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = skill.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = skill.description,
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Success Rate Progress Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { skill.successRate },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00FF88),
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${(skill.successRate * 100).toInt()}% (${skill.successCount} runs)",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Expand Steps Accordion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Workflow Steps" else "View Verified Steps",
                            color = Color(0xFFFF4081),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val steps = try {
                            val arr = JSONArray(skill.stepsJson)
                            val list = mutableListOf<String>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val action = obj.optString("action", "Step ${i + 1}")
                                val verif = obj.optString("verification", "")
                                list.add("${i + 1}. $action ${if (verif.isNotBlank()) "→ [Verif: $verif]" else ""}")
                            }
                            list
                        } catch (e: Exception) {
                            listOf("Steps: ${skill.stepsJson}")
                        }

                        steps.forEach { stepText ->
                            Text(
                                text = stepText,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Required Tools: ${skill.requiredToolsJson}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// MEMORY CARD
// ==========================================

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

            Text(
                text = memory.key,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

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

// ==========================================
// HEALTH VIEW
// ==========================================

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
