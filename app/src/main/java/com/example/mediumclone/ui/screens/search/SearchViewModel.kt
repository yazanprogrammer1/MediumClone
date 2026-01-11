package com.example.mediumclone.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.model.Article
import com.example.mediumclone.data.repository.ArticlesRepository
import com.example.mediumclone.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Top, 1: Stories, 2: People
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    
    // Cache current user ID to filter self out
    private val _currentUserId = MutableStateFlow<String?>(null)

    init {
        observeCurrentUserAndFollowing()
        observeSearch()
    }

    private fun observeCurrentUserAndFollowing() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUserId.value = user?.id
                if (user != null) {
                    authRepository.getFollowingIds(user.id).collect { ids ->
                        _followingIds.value = ids.toSet()
                        // Refresh UI if follow state changes
                        refreshPeopleState()
                    }
                } else {
                    _followingIds.value = emptySet()
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Debounce typing
                .distinctUntilChanged()
                .combine(_followingIds) { query, following -> Pair(query, following) }
                .collect { (query, following) ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, articles = emptyList(), people = emptyList(), error = null) }
                        return@collect
                    }

                    _uiState.update { it.copy(isLoading = true, error = null) }
                    
                    // Note: We are using a simplified search here. 
                    // ideally we'd pass 'tags' check into repository too.
                    try {
                        articlesRepository.searchArticles(query)
                            .collect { filteredArticles ->
                                // "People" are unique authors from the results, EXCLUDING current user
                                val currentId = _currentUserId.value
                                val uniqueAuthors = filteredArticles
                                    .distinctBy { it.authorId }
                                    .filter { it.authorId != currentId } // Filter Self
                                    .map { article ->
                                        PersonResult(
                                            userId = article.authorId,
                                            name = article.authorName,
                                            avatarUrl = article.authorAvatarUrl,
                                            isFollowing = following.contains(article.authorId)
                                        )
                                    }
                                
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false, 
                                        articles = filteredArticles,
                                        people = uniqueAuthors
                                    ) 
                                }
                            }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Search failed") }
                    }
                }
        }
    }
    
    // Re-calculates isFollowing state for current people list without re-fetching
    private fun refreshPeopleState() {
         val currentPeople = _uiState.value.people
         val following = _followingIds.value
         val updatedPeople = currentPeople.map { person ->
             person.copy(isFollowing = following.contains(person.userId))
         }
         _uiState.update { it.copy(people = updatedPeople) }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val currentId = _currentUserId.value ?: return@launch
            val isFollowing = _followingIds.value.contains(userId)
            
            // Optimistic Update
            val updatedFollowing = if (isFollowing) {
                _followingIds.value - userId
            } else {
                _followingIds.value + userId
            }
            _followingIds.value = updatedFollowing
            refreshPeopleState()

            // API Call
            val result = if (isFollowing) {
                authRepository.unfollowUser(currentId, userId)
            } else {
                authRepository.followUser(currentId, userId)
            }
            
            if (result.isFailure) {
                // Revert if failed
                // (In a real app, show error message)
                _followingIds.value = if (isFollowing) updatedFollowing + userId else updatedFollowing - userId
                refreshPeopleState()
            }
        }
    }

    fun searchByTag(tag: String) {
        onQueryChange(tag)
        _selectedTab.value = 1 // Switch to Stories usually for tags
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val articles: List<Article> = emptyList(),
    val people: List<PersonResult> = emptyList(),
    val error: String? = null
)

data class PersonResult(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val isFollowing: Boolean
)
