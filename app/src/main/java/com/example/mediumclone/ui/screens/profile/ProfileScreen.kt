package com.example.mediumclone.ui.screens.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mediumclone.ui.components.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("saved_articles") }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Bookmark,
                            contentDescription = "Saved Articles"
                        )
                    }
                    IconButton(onClick = { 
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    val articles = state.articles
                    val drafts = state.drafts
                    
                    val isFollowing by viewModel.isFollowing.collectAsState()
                    val isFollowLoading by viewModel.isFollowLoading.collectAsState()
                    val isPremium by viewModel.isPremium.collectAsState()

                    var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    val tabs = if (viewModel.isOwnProfile) listOf("Stories", "Drafts") else listOf("Stories")

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            ProfileHeader(
                                user = user,
                                onUploadAvatar = { imageBytes ->
                                    viewModel.uploadAvatar(imageBytes)
                                },
                                onEditProfile = { showEditDialog = true },
                                isOwnProfile = viewModel.isOwnProfile,
                                isFollowing = isFollowing,
                                isFollowLoading = isFollowLoading,
                                onToggleFollow = { viewModel.toggleFollow() },
                                isPremium = user.isPremium,
                                onUpgrade = { navController.navigate("subscription") }
                            )
                        }
                        
                        if (viewModel.isOwnProfile) {
                            item {
                                TabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                                        )
                                    }
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { Text(title) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Articles",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (selectedTabIndex == 0) {
                            items(articles) { article ->
                                ArticleCard(
                                    article = article,
                                    onClick = { navController.navigate("article/${article.id}") },
                                    onAuthorClick = { authorId ->
                                        navController.navigate("profile/$authorId")
                                    }
                                )
                            }
                            if (articles.isEmpty()) {
                                item {
                                    Text(
                                        text = "No stories yet.",
                                        modifier = Modifier.padding(24.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Drafts Tab
                             items(drafts) { draft ->
                                DraftCard(
                                    draft = draft,
                                    onClick = { 
                                        // Navigate to editor with draftId
                                        navController.navigate("editor?draftId=${draft.id}")
                                    },
                                    onDelete = {
                                        viewModel.deleteDraft(draft.id)
                                    }
                                )
                            }
                            if (drafts.isEmpty()) {
                                item {
                                     Text(
                                        text = "No drafts yet. Start writing!",
                                        modifier = Modifier.padding(24.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        val user = (uiState as? ProfileUiState.Success)?.user
        user?.let {
            com.example.mediumclone.ui.components.EditProfileDialog(
                currentName = it.name,
                currentBio = it.bio,
                onDismiss = { showEditDialog = false },
                onSave = { name, bio ->
                    viewModel.updateProfile(name, bio)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun DraftCard(
    draft: com.example.mediumclone.data.local.Draft,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = draft.title.ifBlank { "Untitled Draft" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = draft.content.replace("\n", " ").take(100),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last edited: ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(draft.lastModified))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onDelete) {
                    Icon(
                         imageVector = Icons.Default.Delete,
                         contentDescription = "Delete Draft",
                         tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileHeader(
    user: com.example.mediumclone.data.model.User,
    onUploadAvatar: (ByteArray) -> Unit,
    onEditProfile: () -> Unit,
    isOwnProfile: Boolean,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    onToggleFollow: () -> Unit = {},
    isPremium: Boolean = false,
    onUpgrade: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showAvatarViewer by remember { mutableStateOf(false) }
    

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Logic
        val context = androidx.compose.ui.platform.LocalContext.current
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                bytes?.let { b -> onUploadAvatar(b) }
            }
        }

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.combinedClickable(
                onClick = { 
                    if (isOwnProfile) launcher.launch("image/*") else showAvatarViewer = true 
                },
                onLongClick = {
                    showAvatarViewer = true
                }
            )
        ) {
            // Premium Border
            val borderModifier = if (isPremium) {
                Modifier
                    .size(110.dp)
                    .border(
                        width = 4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700), // Gold
                                Color(0xFFFFE57F), // Light Gold
                                Color(0xFFDAA520)  // Goldenrod
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp) // Space between border and image
            } else {
                Modifier.size(100.dp)
            }

            Box(modifier = borderModifier, contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = user.avatarUrl!!.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}&background=random" },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Interaction/Verification Icons
            if (isOwnProfile) {
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Avatar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (isPremium) {
                // Verification Badge (Instagram Style)
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(Color.White, CircleShape) // White border for icon
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, // Or a custom verified icon
                        contentDescription = "Verified Pro",
                        tint = Color(0xFF1DA1F2), // Twitter/Insta Blue
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Name and Badge Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (isPremium) {
                Spacer(modifier = Modifier.width(8.dp))
                com.example.mediumclone.ui.components.PremiumBadge()
            }
        }
        
        if (user.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
             text = "@${user.email.substringBefore("@")}",
             style = MaterialTheme.typography.labelLarge,
             color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${user.followersCount} Followers", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "${user.followingCount} Following", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isOwnProfile) {
             Button(
                 onClick = onEditProfile,
                 colors = ButtonDefaults.buttonColors(
                     containerColor = MaterialTheme.colorScheme.surfaceVariant,
                     contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                 )
             ) {
                 Text("Edit Profile")
             }
             
             if (!isPremium) {
                 Spacer(modifier = Modifier.height(12.dp))
                 Button(
                     onClick = onUpgrade,
                     modifier = Modifier.fillMaxWidth(),
                     colors = ButtonDefaults.buttonColors(
                         containerColor = MaterialTheme.colorScheme.primaryContainer,
                         contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                     )
                 ) {
                     Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Become a Member")
                 }
             }
        } else {
             // Follow Button logic with Premium Feel
             val buttonColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
             val contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
             
             Button(
                onClick = onToggleFollow,
                enabled = !isFollowLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = contentColor
                ),
                elevation = if (!isFollowing) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (isFollowLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = contentColor)
                } else {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Avatar Viewer Dialog
    if (showAvatarViewer) {
        com.example.mediumclone.ui.components.AvatarViewerDialog(
            avatarUrl = user.avatarUrl ?: "https://ui-avatars.com/api/?name=${user.name}&size=800",
            onDismiss = { showAvatarViewer = false }
        )
    }
}
