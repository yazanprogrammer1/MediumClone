package com.example.mediumclone.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    navController: NavController,
    viewModel: EditorViewModel = hiltViewModel(),
    articleId: String? = null
) {
    // Bind to ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val existingImageUrl by viewModel.existingImageUrl.collectAsState()
    
    
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tagInput by remember { mutableStateOf("") }
    
    // Local state for content manipulation
    // Initialize with ViewModel content to handle Draft Loading
    var contentTextFieldValue by remember { mutableStateOf(content) }
    
    // Sync local state when ViewModel content changes (e.g. created by AI or Loaded from Draft)
    LaunchedEffect(content) {
        if (contentTextFieldValue.text != content.text) {
             contentTextFieldValue = content
        }
    }
    
    // AI State
    var showAIDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }
    val isGeneratingAI by viewModel.isGeneratingAI.collectAsState()
    
    // Premium & Ad Logic
    val showPremiumDialog by viewModel.showPremiumDialog.collectAsState()
    var showAdPlayer by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val visualTransformation = remember(colorScheme) {
        MarkdownTransformation(colorScheme)
    }

    // Sync from ViewModel to local state
    LaunchedEffect(content) {
        if (contentTextFieldValue.text != content.text) {
            contentTextFieldValue = contentTextFieldValue.copy(text = content.text)
        }
    }
    
    // Load article if editing
    LaunchedEffect(articleId) {
        if (articleId != null) {
            viewModel.loadArticle(articleId)
        } else {
            viewModel.resetState()
        }
    }
    
    // Launcher for Cover Image
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Launcher for Inline Image
    val inlineImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                // Use current cursor position
                val cursor = contentTextFieldValue.selection.min
                viewModel.uploadInlineImage(bytes, cursor)
            }
        }
    }

    val isUploadingInline by viewModel.isUploadingInlineImage.collectAsState()
    val autosaveStatus by viewModel.autosaveStatus.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is EditorUiState.Success) {
            navController.popBackStack()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (articleId != null) "Edit Story" else "Draft", style = MaterialTheme.typography.titleMedium)
                        autosaveStatus?.let {
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val bytes = selectedImageUri?.let { 
                                context.contentResolver.openInputStream(it)?.readBytes()
                            }
                            viewModel.publishArticle(bytes)
                        },
                        enabled = title.isNotBlank() && content.text.isNotBlank() && uiState !is EditorUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        if (uiState is EditorUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Text("Publish")
                        }
                    }
                }
            )
        },

        bottomBar = {
            EditorToolbar(
                value = contentTextFieldValue,
                onValueChange = { 
                    contentTextFieldValue = it
                    viewModel.updateContent(it)
                },
                onAIClick = { showAIDialog = true },
                onImageClick = { inlineImageLauncher.launch("image/*") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Access Options Dialog
            if (showPremiumDialog) {
                com.example.mediumclone.ui.components.AccessOptionsDialog(
                    onDismiss = { viewModel.dismissPremiumDialog() },
                    onSubscribeClick = { 
                        viewModel.dismissPremiumDialog()
                        navController.navigate("subscription") 
                    },
                    onWatchAdClick = {
                        viewModel.dismissPremiumDialog()
                        showAdPlayer = true
                    }
                )
            }
            
            // Ad Player
            if (showAdPlayer) {
                com.example.mediumclone.ui.screens.ads.AdPlayerScreen(
                    onAdComplete = {
                        showAdPlayer = false
                        viewModel.unlockAITemporarily()
                        // Retry generation automatically
                        if (aiTopic.isNotBlank()) {
                            viewModel.generateAIContent(aiTopic)
                        }
                    }
                )
            }
            
            // AI Dialog
            if (showAIDialog) {
                AlertDialog(
                    onDismissRequest = { showAIDialog = false },
                    title = { Text("Generate with AI") },
                    text = {
                        Column {
                            Text("Enter a topic and let AI write a premium article for you.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = aiTopic,
                                onValueChange = { aiTopic = it },
                                label = { Text("Topic (e.g., Future of AI)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAIDialog = false
                                viewModel.generateAIContent(aiTopic)
                            },
                            enabled = aiTopic.isNotBlank()
                        ) {
                            Text("Generate")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAIDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // AI Loading Indicator (Overlay)
            if (isGeneratingAI) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                         )
                         Spacer(modifier = Modifier.width(16.dp))
                         Text("AI is writing your article...", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            
            // Inline Upload Loading Indicator
            if (isUploadingInline) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                         )
                         Spacer(modifier = Modifier.width(16.dp))
                         Text("Uploading image...", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            
            if (uiState is EditorUiState.Error) {
                Text(
                    text = (uiState as EditorUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Image Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (existingImageUrl != null) {
                     AsyncImage(
                        model = existingImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Add a cover image", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            TextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.outline)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Content Field
            TextField(
                value = contentTextFieldValue,
                onValueChange = { 
                    contentTextFieldValue = it
                    viewModel.updateContent(it)
                },
                placeholder = { Text("Tell your story...", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp), // Giving it some minimum height
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                visualTransformation = visualTransformation
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tags Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Tags (${tags.size}/5)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display Tags
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Add Tag Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { if (tags.size < 5) tagInput = it },
                        placeholder = { 
                            Text(if (tags.size < 5) "Add a tag..." else "Max 5 tags reached") 
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = tags.size < 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (tagInput.isNotBlank() && tags.size < 5) {
                                    viewModel.addTag(tagInput.trim())
                                    tagInput = ""
                                }
                            }
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank()) {
                                viewModel.addTag(tagInput.trim())
                                tagInput = ""
                            }
                        },
                        enabled = tagInput.isNotBlank() && tags.size < 5
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Extension to avoid import issues
private val Color = androidx.compose.ui.graphics.Color
