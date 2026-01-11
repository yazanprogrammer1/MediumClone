package com.example.mediumclone.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.model.Article
import com.example.mediumclone.data.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val authRepository: com.example.mediumclone.data.repository.AuthRepository,
    private val subscriptionRepository: com.example.mediumclone.data.repository.SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val isPremium = subscriptionRepository.isPremium

    val currentUser = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        loadArticles()
    }

    private fun loadArticles() {
        viewModelScope.launch {
            articlesRepository.getArticles()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Failed to load articles")
                }
                .collect { articles ->
                    _uiState.value = if (articles.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(articles)
                    }
                }
        }
    }

    fun deleteArticle(articleId: String) {
        viewModelScope.launch {
            currentUser.value?.id?.let { userId ->
                articlesRepository.deleteArticle(articleId, userId)
                // Reload articles after deletion
                loadArticles()
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    data class Success(val articles: List<Article>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
