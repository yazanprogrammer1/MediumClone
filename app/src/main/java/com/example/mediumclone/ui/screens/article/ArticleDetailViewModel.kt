package com.example.mediumclone.ui.screens.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.mediumclone.data.model.Article
import com.example.mediumclone.data.repository.ArticlesRepository
import com.example.mediumclone.data.repository.AuthRepository
import com.example.mediumclone.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()
    
    val isPremiumUser = subscriptionRepository.isPremium

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _isLikeLoading = MutableStateFlow(false)
    val isLikeLoading: StateFlow<Boolean> = _isLikeLoading.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isBookmarkLoading = MutableStateFlow(false)
    val isBookmarkLoading: StateFlow<Boolean> = _isBookmarkLoading.asStateFlow()

    // Add currentUser for permissions checks
    val currentUser = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private var currentArticleId: String? = null

    fun loadArticle(articleId: String) {
        currentArticleId = articleId
        viewModelScope.launch {
            _uiState.value = ArticleDetailUiState.Loading
            val result = articlesRepository.getArticle(articleId)
            if (result.isSuccess) {
                _uiState.value = ArticleDetailUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = ArticleDetailUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load article")
            }
        }
        
        // Listen to like status in separate coroutine
        viewModelScope.launch {
            authRepository.currentUser.first()?.let { user ->
                articlesRepository.isArticleLiked(articleId, user.id)
                    .collect { liked ->
                        _isLiked.value = liked
                    }
            }
        }
        
        // Listen to bookmark status in separate coroutine
        viewModelScope.launch {
            authRepository.currentUser.first()?.let { user ->
                articlesRepository.isArticleBookmarked(articleId, user.id)
                    .collect { bookmarked ->
                        _isBookmarked.value = bookmarked
                    }
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            val articleId = currentArticleId ?: return@launch
            val user = authRepository.currentUser.first() ?: return@launch
            
            // Optimistic UI update - change immediately
            val wasLiked = _isLiked.value
            _isLiked.value = !wasLiked
            
            // Optimistic Count Update
            val currentState = _uiState.value
            if (currentState is ArticleDetailUiState.Success) {
                val currentCount = currentState.article.likesCount
                val newCount = if (wasLiked) currentCount - 1 else currentCount + 1
                _uiState.value = ArticleDetailUiState.Success(
                    currentState.article.copy(likesCount = newCount)
                )
            }
            
            _isLikeLoading.value = true
            
            val result = if (wasLiked) {
                articlesRepository.unlikeArticle(articleId, user.id)
            } else {
                articlesRepository.likeArticle(articleId, user.id)
            }
            
            _isLikeLoading.value = false
            
            if (result.isFailure) {
                // Revert on failure
                _isLiked.value = wasLiked
                if (currentState is ArticleDetailUiState.Success) {
                    _uiState.value = currentState // Revert count
                }
            }
        }
    }

    fun deleteArticle(articleId: String) {
        viewModelScope.launch {
            currentUser.value?.id?.let { userId ->
                articlesRepository.deleteArticle(articleId, userId)
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val articleId = currentArticleId ?: return@launch
            val user = authRepository.currentUser.first() ?: return@launch
            
            // Optimistic UI update - change immediately
            val wasBookmarked = _isBookmarked.value
            _isBookmarked.value = !wasBookmarked
            
            _isBookmarkLoading.value = true
            
            val result = if (wasBookmarked) {
                articlesRepository.unbookmarkArticle(articleId, user.id)
            } else {
                articlesRepository.bookmarkArticle(articleId, user.id)
            }
            
            _isBookmarkLoading.value = false
            
            if (result.isFailure) {
                // Revert on failure
                _isBookmarked.value = wasBookmarked
            }
        }
    }
}

sealed class ArticleDetailUiState {
    object Loading : ArticleDetailUiState()
    data class Success(val article: Article) : ArticleDetailUiState()
    data class Error(val message: String) : ArticleDetailUiState()
}
