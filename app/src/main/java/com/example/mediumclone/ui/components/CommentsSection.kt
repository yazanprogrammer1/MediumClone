package com.example.mediumclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mediumclone.data.model.Comment
import com.example.mediumclone.data.repository.ArticlesRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository
) : ViewModel() {
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadComments(articleId: String) {
        viewModelScope.launch {
            articlesRepository.getComments(articleId).collect {
                _comments.value = it
            }
        }
    }

    fun addComment(articleId: String, content: String) {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            _isLoading.value = true
            articlesRepository.addComment(articleId, content)
            _isLoading.value = false
        }
    }

    fun deleteComment(articleId: String, commentId: String) {
        viewModelScope.launch {
            articlesRepository.deleteComment(articleId, commentId)
        }
    }

    fun toggleLikeComment(articleId: String, commentId: String, userId: String, isLiked: Boolean) {
        viewModelScope.launch {
            if (isLiked) {
                articlesRepository.unlikeComment(articleId, commentId, userId)
            } else {
                articlesRepository.likeComment(articleId, commentId, userId)
            }
        }
    }

    fun updateComment(articleId: String, commentId: String, newContent: String) {
        viewModelScope.launch {
            if (newContent.isBlank()) return@launch
            articlesRepository.updateComment(articleId, commentId, newContent)
        }
    }
}

@Composable
fun CommentsSection(
    articleId: String,
    navController: NavController,
    viewModel: CommentsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var newCommentText by remember { mutableStateOf("") }
    /*

     */
    LaunchedEffect(articleId) {
        viewModel.loadComments(articleId)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // Add Comment Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Write a comment...") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    viewModel.addComment(articleId, newCommentText)
                    newCommentText = ""
                },
                enabled = newCommentText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (comments.isEmpty()) {
            Text(
                text = "No comments yet. Be the first to share your thoughts!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            comments.forEach { comment ->
                CommentItem(
                    comment = comment, 
                    articleId = articleId,
                    currentUserId = Firebase.auth.currentUser?.uid,
                    onDelete = { commentId -> viewModel.deleteComment(articleId, commentId) },
                    onToggleLike = { commentId, isLiked -> 
                        val userId = Firebase.auth.currentUser?.uid
                        if (userId != null) {
                            viewModel.toggleLikeComment(articleId, commentId, userId, isLiked)
                        }
                    },
                    onEdit = { commentId, newContent ->
                        viewModel.updateComment(articleId, commentId, newContent)
                    },
                    navController = navController
                )
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    articleId: String,
    currentUserId: String?,
    onDelete: (String) -> Unit,
    onToggleLike: (String, Boolean) -> Unit,
    onEdit: (String, String) -> Unit,
    navController: NavController
) {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateString = sdf.format(comment.timestamp.toDate())
    val isLiked = comment.likedBy.contains(currentUserId)
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf(comment.content) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = comment.userAvatarUrl ?: "https://ui-avatars.com/api/?name=${comment.userName}",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("profile/${comment.userId}") },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { navController.navigate("profile/${comment.userId}") }
                    )
                    Text(
                        text = " · $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (isEditing) {
                    Column {
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            TextButton(onClick = { isEditing = false; editContent = comment.content }) {
                                Text("Cancel")
                            }
                            Button(onClick = { 
                                onEdit(comment.id, editContent)
                                isEditing = false 
                            }) {
                                Text("Save")
                            }
                        }
                    }
                } else {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Likes Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                     IconToggleButton(
                        checked = isLiked,
                        onCheckedChange = { if (currentUserId != null) onToggleLike(comment.id, isLiked) }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (comment.likesCount > 0) {
                        Text(
                            text = "${comment.likesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Delete Menu (only for owner)
        if (currentUserId == comment.userId) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            isEditing = true
                            editContent = comment.content
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDelete(comment.id)
                        }
                    )
                }
            }
        }
    }
}
