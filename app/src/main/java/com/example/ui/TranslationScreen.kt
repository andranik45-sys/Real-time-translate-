package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import com.example.data.TranslationEntity
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel,
    onRequestPermission: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeModeTab by remember { mutableStateOf(0) } // 0 = Standard Direct, 1 = Conversation Mode
    val sourceText by viewModel.sourceText.collectAsStateWithLifecycle()
    val translatedText by viewModel.translatedText.collectAsStateWithLifecycle()
    val sourceLang by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
    val isSpeakingSource by viewModel.isSpeakingSource.collectAsStateWithLifecycle()
    val isSpeakingTarget by viewModel.isSpeakingTarget.collectAsStateWithLifecycle()
    val history by viewModel.translationHistory.collectAsStateWithLifecycle()
    val uiTranslations by viewModel.uiTranslations.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Local filter tabs for Room History
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Starred

    val filteredHistory = remember(history, selectedTab) {
        if (selectedTab == 1) {
            history.filter { it.isFavorite }
        } else {
            history
        }
    }

    // Dropdown controls
    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    CircleShape
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sync Translator".localize(uiTranslations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    var showUiLangMenu by remember { mutableStateOf(false) }
                    val appInterfaceLang by viewModel.appInterfaceLanguage.collectAsStateWithLifecycle()
                    
                    Box {
                        IconButton(onClick = { showUiLangMenu = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = appInterfaceLang.flagEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showUiLangMenu,
                            onDismissRequest = { showUiLangMenu = false },
                            modifier = Modifier.requiredSizeIn(maxHeight = 350.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("${"App Language".localize(uiTranslations)}: ${appInterfaceLang.nativeName}") },
                                onClick = {},
                                enabled = false
                            )
                            HorizontalDivider()
                            viewModel.languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${lang.flagEmoji}  ${lang.nativeName} (${lang.name})")
                                    },
                                    onClick = {
                                        viewModel.selectInterfaceLanguage(lang)
                                        showUiLangMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Language Selection Header Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source Lang Selector
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("source_language_dropdown")
                    ) {
                        LanguageBadge(
                            language = sourceLang,
                            onClick = { showSourceMenu = true }
                        )
                        DropdownMenu(
                            expanded = showSourceMenu,
                            onDismissRequest = { showSourceMenu = false },
                            modifier = Modifier.requiredSizeIn(maxHeight = 350.dp)
                        ) {
                            viewModel.languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${lang.flagEmoji}  ${lang.nativeName} (${lang.name})")
                                    },
                                    onClick = {
                                        viewModel.selectSourceLanguage(lang)
                                        showSourceMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = { viewModel.swapLanguages() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .testTag("swap_languages_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Languages",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Target Lang Selector
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_language_dropdown"),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        LanguageBadge(
                            language = targetLang,
                            onClick = { showTargetMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTargetMenu,
                            onDismissRequest = { showTargetMenu = false },
                            modifier = Modifier.requiredSizeIn(maxHeight = 350.dp)
                        ) {
                            viewModel.languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${lang.flagEmoji}  ${lang.nativeName} (${lang.name})")
                                    },
                                    onClick = {
                                        viewModel.selectTargetLanguage(lang)
                                        showTargetMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dual Mode Selector Mode Tabs
            PrimaryTabRow(
                selectedTabIndex = activeModeTab,
                modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 4.dp),
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = activeModeTab == 0,
                    onClick = { activeModeTab = 0 },
                    text = { Text("Standard Mode".localize(uiTranslations), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeModeTab == 1,
                    onClick = { 
                        activeModeTab = 1
                        viewModel.stopListening() // Reset any direct recordings
                    },
                    text = { Text("Conversation".localize(uiTranslations), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Premium AI Translate & Conversation Mods Selection Row
            var showModsPanel by remember { mutableStateOf(false) }
            val selectedStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
            val selectedContext by viewModel.selectedContext.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModsPanel = !showModsPanel },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Translation Mode Mods".localize(uiTranslations),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedStyle.emoji} ${selectedStyle.title.localize(uiTranslations)} • ${selectedContext.emoji} ${selectedContext.title.localize(uiTranslations)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showModsPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle mods options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showModsPanel) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Translation Style Mods
                        Text(
                            text = "AI TRANSLATION STYLE / TONE:".localize(uiTranslations),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            TranslationStyle.values().forEach { style ->
                                ModChip(
                                    text = style.title.localize(uiTranslations),
                                    emoji = style.emoji,
                                    selected = selectedStyle == style,
                                    onClick = { viewModel.selectStyle(style) },
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Dialogue Context / Topic Mods
                        Text(
                            text = "CONVERSATION TOPIC SCENARIO:".localize(uiTranslations),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            ContextMod.values().forEach { contextMod ->
                                ModChip(
                                    text = contextMod.title.localize(uiTranslations),
                                    emoji = contextMod.emoji,
                                    selected = selectedContext == contextMod,
                                    onClick = { viewModel.selectContext(contextMod) },
                                    selectedColor = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Split-Earbud Mode Card
            val isSplitEarbudMode by viewModel.isSplitEarbudMode.collectAsStateWithLifecycle()
            val hasBluetoothHeadsets = remember { mutableStateOf(viewModel.isBluetoothHeadsetsConnected()) }

            LaunchedEffect(isSplitEarbudMode) {
                while (true) {
                    hasBluetoothHeadsets.value = viewModel.isBluetoothHeadsetsConnected()
                    delay(2500)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSplitEarbudMode) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSplitEarbudMode) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Headset,
                                contentDescription = null,
                                tint = if (isSplitEarbudMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Split-Earbud Mode".localize(uiTranslations),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val statusText = if (hasBluetoothHeadsets.value) {
                                    "Bluetooth headphones detected! Stereo split pan is active.".localize(uiTranslations)
                                } else {
                                    "Please connect Bluetooth headphones to activate split audio routing.".localize(uiTranslations)
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    color = if (hasBluetoothHeadsets.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Switch(
                            checked = isSplitEarbudMode,
                            onCheckedChange = { viewModel.toggleSplitEarbudMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.scale(0.85f).testTag("split_earbud_toggle_switch")
                        )
                    }

                    if (isSplitEarbudMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Left Earbud: Source language".localize(uiTranslations) + " 🎧 ←",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = sourceLang.nativeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Right Earbud: Target language".localize(uiTranslations) + " → 🎧",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = targetLang.nativeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (activeModeTab == 0) {
                // Main Input-Output Workspace
                Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Source Expression Input Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sourceLang.nativeName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 2.dp)
                                )

                                if (sourceText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearText() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("clear_text_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Input",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Raw input field
                            TextField(
                                value = sourceText,
                                onValueChange = { viewModel.updateSourceText(it) },
                                placeholder = {
                                    Text(
                                        text = "${"Type here or press Mic to talk in".localize(uiTranslations)} ${sourceLang.nativeName}...",
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("input_speech_text_field"),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Done
                                )
                            )

                            // Left align speak buttons and record volume details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (sourceText.isNotBlank()) {
                                    // Tap to Pronounce/Speak input out loud
                                    IconButton(
                                        onClick = { viewModel.speakText(sourceText, sourceLang, isSource = true) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSpeakingSource) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                else Color.Transparent,
                                                CircleShape
                                            )
                                            .testTag("speak_source_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeakingSource) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                                            contentDescription = "Speak Input Preview",
                                            tint = if (isSpeakingSource) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(36.dp))
                                }

                                // Interactive Pulsing Waveform displaying when speaking/recording
                                if (isRecording) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        (1..10).forEach { index ->
                                            val animatedHeight by animateFloatAsState(
                                                targetValue = if (isRecording) (10 + (audioVolume * 24) * (index % 3 + 1) * 0.35f) else 4f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(animatedHeight.dp)
                                                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(100))
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "${sourceText.length} chars",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Translated Output Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("translation_output_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = CardStrokeSpec(isLoading)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                    Text(
                                        text = "${targetLang.nativeName.uppercase()} (${"Translate".localize(uiTranslations).uppercase()})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                if (translatedText.isNotBlank()) {
                                    Text(
                                        text = translatedText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = "Your synchronized translation result will instantly appear here...".localize(uiTranslations),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            if (translatedText.isNotBlank() && !isLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tap to Pronounce/Speak output out loud
                                    IconButton(
                                        onClick = { viewModel.speakText(translatedText, targetLang, isSource = false) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSpeakingTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else Color.Transparent,
                                                CircleShape
                                            )
                                            .testTag("speak_target_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeakingTarget) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                                            contentDescription = "Speak translation",
                                            tint = if (isSpeakingTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Tap to Copy translated text
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(translatedText))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy Translation text",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick floating mic action inside bottom of the screen space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Large record toggle button
                val pulseScale by rememberInfiniteTransition().animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRecording) 1.25f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                val btnColor by animateColorAsState(
                    targetValue = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopListening()
                        } else {
                            onRequestPermission(sourceLang)
                        }
                    },
                    modifier = Modifier
                        .scale(pulseScale)
                        .height(56.dp)
                        .testTag("voice_input_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = btnColor
                    ),
                    shape = RoundedCornerShape(100),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop listening" else "Start Voice Input"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecording) "Recording...".localize(uiTranslations) else "Tap to Speak".localize(uiTranslations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Visual Notification Banner for Client Error Message Handling
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 4,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // History & Database Save section
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // History header with Starred Favorites tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved History".localize(uiTranslations),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Tab Switcher List (All vs Starred)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { selectedTab = 0 }
                            .background(
                                if (selectedTab == 0) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All".localize(uiTranslations),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clickable { selectedTab = 1 }
                            .background(
                                if (selectedTab == 1) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Favorites".localize(uiTranslations),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (filteredHistory.isNotEmpty()) {
                    Text(
                        text = "Clear All".localize(uiTranslations),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { viewModel.clearAllHistory() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // History Items List
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth()
            ) {
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Default.StarBorder else Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(1.1f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedTab == 1) "No favorites stored yet".localize(uiTranslations) else "No local translation history".localize(uiTranslations),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_list"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredHistory,
                            key = { it.id }
                        ) { item ->
                            HistoryItemRow(
                                item = item,
                                onClick = { viewModel.selectHistoryItem(item) },
                                onToggleFav = { viewModel.toggleFavorite(item) },
                                onDelete = { viewModel.deleteHistoryItem(item) }
                            )
                        }
                    }
                }
            }
            } else {
                ConversationWorkspace(
                    viewModel = viewModel,
                    uiTranslations = uiTranslations,
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LanguageBadge(
    language: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = language.flagEmoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Column {
                Text(
                    text = language.nativeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = language.name,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HistoryItemRow(
    item: TranslationEntity,
    onClick: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Direction tag
                Text(
                    text = "${item.sourceLangName} → ${item.targetLangName}".uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Text(
                    text = item.originalText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.translatedText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                // Toggle Favorite status
                IconButton(
                    onClick = onToggleFav,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete history translation
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CardStrokeSpec(isLoading: Boolean): androidx.compose.foundation.BorderStroke? {
    if (!isLoading) return null
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val animatedOpacity by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )
    return androidx.compose.foundation.BorderStroke(
        2.dp, 
        MaterialTheme.colorScheme.primary.copy(alpha = animatedOpacity)
    )
}

@Composable
fun ConversationWorkspace(
    viewModel: TranslationViewModel,
    uiTranslations: Map<String, String>,
    onRequestPermission: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceLang by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val history by viewModel.translationHistory.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val listeningLanguage by viewModel.listeningLanguage.collectAsStateWithLifecycle()
    val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
    val partialTargetText by viewModel.partialTargetText.collectAsStateWithLifecycle()
    val sourceText by viewModel.sourceText.collectAsStateWithLifecycle()

    // Filter conversation history between the two selected languages
    val conversationItems = remember(history, sourceLang, targetLang) {
        history.filter {
            (it.sourceLangCode == sourceLang.code && it.targetLangCode == targetLang.code) ||
            (it.sourceLangCode == targetLang.code && it.targetLangCode == sourceLang.code)
        }.sortedBy { it.timestamp }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (conversationItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bilateral Conversation Mode".localize(uiTranslations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap either microphone below to speak and translate. Conversational replies are spoken back out loud and saved.".localize(uiTranslations),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                // Auto scroll to latest messages when they arrive
                LaunchedEffect(conversationItems.size) {
                    if (conversationItems.isNotEmpty()) {
                        lazyListState.animateScrollToItem(conversationItems.size - 1)
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = conversationItems,
                        key = { it.id }
                    ) { item ->
                        val isSourceSpeaker = item.sourceLangCode == sourceLang.code
                        ConversationBubble(
                            item = item,
                            isSourceSpeaker = isSourceSpeaker,
                            onPlay = { text, langCode ->
                                val targetLangObj = if (langCode == sourceLang.code) sourceLang else targetLang
                                viewModel.speakText(text, targetLangObj, isSource = langCode == sourceLang.code)
                            },
                            onToggleFav = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }

        // Live partial transcription feedback block
        if (isRecording) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${"Listening:".localize(uiTranslations)} " + (
                            if (listeningLanguage?.code == sourceLang.code) sourceText else partialTargetText
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Parallel Mic Action Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left mic (Source language, e.g. English)
                val isLeftActive = isRecording && listeningLanguage?.code == sourceLang.code
                val leftPulseScale by rememberInfiniteTransition().animateFloat(
                    initialValue = 1f,
                    targetValue = if (isLeftActive) 1.1f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseOutQuad),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Button(
                    onClick = {
                        if (isLeftActive) {
                            viewModel.stopListening()
                        } else {
                            onRequestPermission(sourceLang)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .scale(leftPulseScale)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLeftActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLeftActive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Listen English",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sourceLang.flagEmoji,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = sourceLang.nativeName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right mic (Target Language, e.g. Spanish)
                val isRightActive = isRecording && listeningLanguage?.code == targetLang.code
                val rightPulseScale by rememberInfiniteTransition().animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRightActive) 1.1f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseOutQuad),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Button(
                    onClick = {
                        if (isRightActive) {
                            viewModel.stopListening()
                        } else {
                            onRequestPermission(targetLang)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .scale(rightPulseScale)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRightActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRightActive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Listen Target Language",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = targetLang.flagEmoji,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = targetLang.nativeName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(
    item: TranslationEntity,
    isSourceSpeaker: Boolean,
    onPlay: (String, String) -> Unit,
    onToggleFav: () -> Unit
) {
    val containerColor = if (isSourceSpeaker) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
    }

    val alignment = if (isSourceSpeaker) Alignment.End else Alignment.Start
    val shape = if (isSourceSpeaker) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Speaker tag
        Text(
            text = item.sourceLangName.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = shape,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Original voice input text
                Text(
                    text = item.originalText,
                    fontSize = 14.sp,
                    color = if (isSourceSpeaker) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Normal
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = if (isSourceSpeaker) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                )

                // Translated translation text display
                Text(
                    text = item.translatedText,
                    fontSize = 15.sp,
                    color = if (isSourceSpeaker) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentOnSurfaceColor = if (isSourceSpeaker) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    
                    // Favorite button
                    IconButton(
                        onClick = onToggleFav,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.error else currentOnSurfaceColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Pronounce Speech button
                    IconButton(
                        onClick = { onPlay(item.translatedText, item.targetLangCode) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = currentOnSurfaceColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModChip(
    text: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "bgColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = animatedBgColor,
        contentColor = animatedContentColor,
        modifier = Modifier
            .padding(end = 6.dp)
            .height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun String.localize(uiTranslations: Map<String, String>): String {
    return uiTranslations[this] ?: this
}

