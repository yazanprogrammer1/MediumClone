package com.example.mediumclone.ui.screens.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.model.Comment
import com.example.mediumclone.data.repository.AuthRepository
import com.example.mediumclone.data.repository.CommentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentsRepository: CommentsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentsCount = MutableStateFlow(0)
    val commentsCount: StateFlow<Int> = _commentsCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private var currentArticleId: String? = null
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: String?
        get() = _currentUserId.value

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUserId.value = user?.id
            }
        }
    }

    fun loadComments(articleId: String) {
        currentArticleId = articleId
        viewModelScope.launch {
            commentsRepository.getComments(articleId)
                .catch { e ->
                    // Handle error
                }
                .collect { commentsList ->
                    _comments.value = commentsList
                }
        }

        viewModelScope.launch {
            commentsRepository.getCommentsCount(articleId)
                .catch { e ->
                    // Handle error
                }
                .collect { count ->
                    _commentsCount.value = count
                }
        }
    }

    fun updateCommentText(text: String) {
        _commentText.value = text
    }

    fun postComment() {
        val articleId = currentArticleId ?: return
        val content = _commentText.value.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = commentsRepository.addComment(articleId, content)
            _isLoading.value = false

            if (result.isSuccess) {
                _commentText.value = ""
            } else {
                // Handle error - could show a snackbar
            }
        }
    }

    fun deleteComment(commentId: String) {
        val articleId = currentArticleId ?: return

        viewModelScope.launch {
            val result = commentsRepository.deleteComment(articleId, commentId)
            if (result.isFailure) {
                // Handle error - could show a snackbar
            }
        }
    }
}
