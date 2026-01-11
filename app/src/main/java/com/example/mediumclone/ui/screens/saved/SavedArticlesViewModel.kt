package com.example.mediumclone.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.model.Article
import com.example.mediumclone.data.repository.ArticlesRepository
import com.example.mediumclone.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedArticlesViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    val savedArticles: StateFlow<List<Article>> = _savedArticles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSavedArticles()
    }

    private fun loadSavedArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.currentUser.first()?.let { user ->
                articlesRepository.getBookmarkedArticles(user.id)
                    .catch { e ->
                        _isLoading.value = false
                    }
                    .collect { articles ->
                        _savedArticles.value = articles
                        _isLoading.value = false
                    }
            }
        }
    }

    fun removeBookmark(articleId: String) {
        viewModelScope.launch {
            authRepository.currentUser.first()?.let { user ->
                articlesRepository.unbookmarkArticle(articleId, user.id)
            }
        }
    }
}
