package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiChatMessage
import com.example.data.AppColors.FacebookBlue
import com.example.data.GroundingCitation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    messages: List<AiChatMessage>,
    selectedModel: String,
    systemInstruction: String,
    isSearchGroundingEnabled: Boolean,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSetSystemInstruction: (String) -> Unit,
    onToggleSearchGrounding: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val models = listOf(
        Triple("gemini-2.5-flash", "Gemini 2.5 Flash", "Production multimodal model, fast reasoning & low latency"),
        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "General multi-turn tasks & search grounding"),
        Triple("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep reasoning & complex tasks"),
        Triple("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Ultra-fast quick responses")
    )

    val starterPrompts = listOf(
        "Tell me about the best trekking trails in Yarkhoon Valley",
        "Write a welcoming caption for a Chitrali cultural festival post",
        "What is the current weather forecast for Upper Chitral?",
        "Explain the history of Shandur Polo and traditional Khowar music"
    )

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Yarkhoon AI Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSearchGroundingEnabled) Color(0xFF4285F4).copy(alpha = 0.15f) else FacebookBlue.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSearchGroundingEnabled) {
                                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(10.dp))
                                        Text("Search Grounded", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                                    } else {
                                        Text(
                                            when (selectedModel) {
                                                "gemini-2.5-flash" -> "Flash 2.5"
                                                "gemini-3.5-flash" -> "Flash 3.5"
                                                "gemini-3.1-pro-preview" -> "Pro 3.1"
                                                "gemini-3.1-flash-lite" -> "Flash-Lite"
                                                else -> "Flash 2.5"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FacebookBlue
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (isGenerating) "Gemini is typing..." else "Powered by Google Gemini API",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Search Grounding Quick Toggle
                    IconButton(
                        onClick = { onToggleSearchGrounding(!isSearchGroundingEnabled) },
                        modifier = Modifier.testTag("toggle_search_grounding_btn")
                    ) {
                        Icon(
                            imageVector = if (isSearchGroundingEnabled) Icons.Filled.TravelExplore else Icons.Outlined.TravelExplore,
                            contentDescription = "Search Grounding",
                            tint = if (isSearchGroundingEnabled) Color(0xFF4285F4) else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Model Selector Dropdown Trigger
                    Box {
                        IconButton(onClick = { showModelMenu = true }, modifier = Modifier.testTag("chat_model_selector_btn")) {
                            Icon(Icons.Filled.Tune, contentDescription = "Select Model", tint = FacebookBlue)
                        }
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            models.forEach { (modelId, label, desc) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(label, fontWeight = if (selectedModel == modelId) FontWeight.Bold else FontWeight.Normal)
                                                if (selectedModel == modelId) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Filled.Check, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        onSelectModel(modelId)
                                        showModelMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Persona & System Role Settings") },
                                leadingIcon = { Icon(Icons.Filled.Psychology, contentDescription = null) },
                                onClick = {
                                    showModelMenu = false
                                    showSettingsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat History", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showModelMenu = false
                                    onClearChat()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Grounding notification banner if enabled
            AnimatedVisibility(visible = isSearchGroundingEnabled) {
                Surface(
                    color = Color(0xFF4285F4).copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(14.dp))
                        Text(
                            "Google Search Grounding active: queries retrieve real-time facts with source citations.",
                            fontSize = 11.sp,
                            color = Color(0xFF1967D2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.content))
                        },
                        onOpenCitation = { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        TypingIndicatorBubble(model = selectedModel)
                    }
                }
            }

            // Starter Suggestions Row if short conversation
            if (messages.size <= 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(starterPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                onSendMessage(prompt)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(14.dp))
                                Text(prompt, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Input Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Gemini or explore valley info...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                val text = inputText
                                inputText = ""
                                onSendMessage(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("gemini_chat_send_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = FacebookBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }

    // Persona / System Instruction Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            SystemInstructionSheet(
                currentInstruction = systemInstruction,
                onSave = {
                    onSetSystemInstruction(it)
                    showSettingsSheet = false
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: AiChatMessage,
    onCopy: () -> Unit,
    onOpenCitation: (String) -> Unit
) {
    val isUser = message.role == "user"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = FacebookBlue.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) FacebookBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
                modifier = Modifier.testTag("chat_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Grounding Citations List
                    if (message.searchCitations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sources from Google Search:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        message.searchCitations.forEach { citation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { onOpenCitation(citation.uri) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(12.dp))
                                Text(
                                    text = citation.title,
                                    fontSize = 11.sp,
                                    color = Color(0xFF1A73E8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Timestamp and Actions
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                if (!isUser) {
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        when (message.modelUsed) {
                            "gemini-3.1-pro-preview" -> "Pro"
                            "gemini-3.1-flash-lite" -> "Lite"
                            else -> "Flash"
                        },
                        fontSize = 10.sp,
                        color = FacebookBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onCopy, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(model: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = CircleShape,
            color = FacebookBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = FacebookBlue)
                Text("Gemini is thinking...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SystemInstructionSheet(
    currentInstruction: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentInstruction) }

    val presetPersonas = listOf(
        "Chitral Valley Cultural Guide" to "You are Yarkhoon AI, an expert on Chitral Valley culture, geography, business, language (Khowar/Urdu/English), and community guidance.",
        "Social Media Copywriter" to "You are an expert social media copywriter for Yarkhoon community posts. Help write engaging captions, hashtags, and event announcements with a lively tone.",
        "Concise Fact Assistant" to "You are a direct, concise AI assistant. Provide fact-based, bulleted summaries and fast answers to questions.",
        "Storyteller & Poet" to "You are a traditional mountain storyteller. Craft evocative descriptions of Hindu Kush landscapes, traditional folklore, and community heritage."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Text("AI Persona & System Instructions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Define how the Gemini chatbot behaves and formulates responses.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        Spacer(modifier = Modifier.height(14.dp))
        Text("Choose a preset persona:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        presetPersonas.forEach { (name, prompt) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { text = prompt },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(prompt, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Custom System Instruction") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(text) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
            ) {
                Text("Apply Persona", fontWeight = FontWeight.Bold)
            }
        }
    }
}
