package com.example.mediumclone.ui.screens.article

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mediumclone.ui.components.BookmarkButton
import com.example.mediumclone.ui.components.Paywall
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    navController: NavController,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isBookmarkLoading by viewModel.isBookmarkLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is ArticleDetailUiState.Success -> {
                    val article = state.article
                    val isOwnArticle = currentUser?.id == article.authorId
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    TopAppBar(
                        title = { Text("") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Bookmark button
                            BookmarkButton(
                                isBookmarked = isBookmarked,
                                isLoading = isBookmarkLoading,
                                onBookmarkClick = { viewModel.toggleBookmark() }
                            )

                            // Options menu
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (isOwnArticle) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                navController.navigate("editor/${article.id}")
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }

                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            com.example.mediumclone.utils.ShareUtils.shareArticle(context, article)
                                        }
                                    )
                                }
                            }
                        }
                    )

                    if (showDeleteDialog) {
                        com.example.mediumclone.ui.components.DeleteArticleDialog(
                            onDismiss = { showDeleteDialog = false },
                            onConfirm = {
                                showDeleteDialog = false
                                viewModel.deleteArticle(article.id)
                                navController.popBackStack()
                            }
                        )
                    }
                }
                else -> {
                    TopAppBar(
                        title = { Text("") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ArticleDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ArticleDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ArticleDetailUiState.Success -> {
                    val article = state.article
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val dateString = sdf.format(article.timestamp.toDate())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Title
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 40.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Author info - clickable
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("profile/${article.authorId}")
                                        }
                                ) {
                                    AsyncImage(
                                        model = article.authorAvatarUrl ?: "https://ui-avatars.com/api/?name=${article.authorName}",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = article.authorName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$dateString · ${article.readTimeMinutes} min read",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Cover Image
                                if (article.coverImageUrl != null) {
                                    AsyncImage(
                                        model = article.coverImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // Content is rendered below with Premium logic

                                Spacer(modifier = Modifier.height(24.dp))

                                // Tags
                                if (article.tags.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        article.tags.forEach { tag ->
                                            androidx.compose.material3.SuggestionChip(
                                                onClick = { navController.navigate("search/$tag") },
                                                label = { Text(tag) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                Divider()

                                Spacer(modifier = Modifier.height(16.dp))

                                // Markdown Content Area
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        if (article.isPremium && !isPremiumUser) {
                                             // Truncated Content
                                             val truncatedContent = article.content.take(300) + "..."
                                             com.example.mediumclone.ui.screens.article.MarkdownRenderer(
                                                content = truncatedContent,
                                                onLinkClick = {}
                                            )
                                            Spacer(modifier = Modifier.height(300.dp)) // Reserve space for Paywall
                                        } else {
                                            // Full Content
                                            var showLinkDialog by remember { mutableStateOf(false) }
                                            var clickedLink by remember { mutableStateOf("") }
                                            
                                            com.example.mediumclone.ui.screens.article.MarkdownRenderer(
                                                content = article.content,
                                                onLinkClick = { url ->
                                                    clickedLink = url
                                                    showLinkDialog = true
                                                }
                                            )
                                            
                                            if (showLinkDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showLinkDialog = false },
                                                    title = { Text("External Link") },
                                                    text = { Text("You are about to leave the app to visit:\n$clickedLink\n\nDo you want to continue?") },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                showLinkDialog = false
                                                                try {
                                                                    var url = clickedLink
                                                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                                        url = "https://$url"
                                                                    }
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    // Fallback or ignore
                                                                }
                                                            }
                                                        ) {
                                                            Text("Continue")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showLinkDialog = false }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Paywall Overlay
                                    if (article.isPremium && !isPremiumUser) {
                                        com.example.mediumclone.ui.components.Paywall(
                                            onUpgradeClick = { navController.navigate("subscription") },
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        )
                                    }
                                }
                                // Like button
                                val isLiked by viewModel.isLiked.collectAsState()
                                val isLikeLoading by viewModel.isLikeLoading.collectAsState()

                                // Like button (Claps) moved down for luxury feel
                                Spacer(modifier = Modifier.height(40.dp))
                                
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    com.example.mediumclone.ui.components.ClapButton(
                                        clapCount = article.likesCount,
                                        isClappedByMe = isLiked,
                                        onClap = { 
                                            if (!isLikeLoading) viewModel.toggleLike() 
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Comments section
                                Text(
                                    text = "Comments",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                com.example.mediumclone.ui.components.CommentsSection(
                                    articleId = article.id,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
