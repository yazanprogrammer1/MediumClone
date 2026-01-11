package com.example.mediumclone.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediumclone.data.model.Article
import com.example.mediumclone.data.model.User
import com.example.mediumclone.data.repository.ArticlesRepository
import com.example.mediumclone.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val articlesRepository: ArticlesRepository,
    private val draftsRepository: com.example.mediumclone.data.repository.DraftsRepository,
    private val subscriptionRepository: com.example.mediumclone.data.repository.SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    val isPremium = subscriptionRepository.isPremium
    
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading.asStateFlow()

    private var currentUserId: String? = null
    private var profileUserId: String? = null

    val isOwnProfile: Boolean
        get() = currentUserId == profileUserId

    val currentUser = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        // Observe current user
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                currentUserId = user?.id
            }
        }

        // Separate flow to observe follow status whenever profileUserId or currentUserId changes
        viewModelScope.launch {
            authRepository.currentUser
                .combine(_uiState) { user, state ->
                    val profileId = (state as? ProfileUiState.Success)?.user?.id ?: profileUserId
                    Pair(user?.id, profileId)
                }
                .collect { (currentId, profileId) ->
                    if (currentId != null && profileId != null && currentId != profileId) {
                        try {
                            authRepository.isFollowing(currentId, profileId)
                                .collect { following ->
                                    _isFollowing.value = following
                                }
                        } catch (e: Exception) {
                            // Handle error or cancellation
                        }
                    }
                }
        }
    }

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            // Use provided userId or current user's ID
            val targetUserId = userId ?: authRepository.currentUser.first()?.id
            profileUserId = targetUserId
            
            if (targetUserId == null) {
                _uiState.value = ProfileUiState.Error("User not found")
                return@launch
            }

            // Fetch user profile
            try {
                val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(targetUserId)
                    .get()
                    .await()

                val user = userDoc.toObject(com.example.mediumclone.data.model.User::class.java)

                if (user == null) {
                    _uiState.value = ProfileUiState.Error("User not found")
                    return@launch
                }

                // Fetch user's articles and drafts concurrently if own profile
                val articlesFlow = articlesRepository.getArticlesByUser(targetUserId)
                val draftsFlow = if (targetUserId == currentUserId) {
                    draftsRepository.getAllDrafts()
                } else {
                    flowOf(emptyList())
                }

                combine(articlesFlow, draftsFlow) { articles, drafts ->
                    Pair(articles, drafts)
                }.catch { e ->
                     _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load content")
                }.collect { (articles, drafts) ->
                    _uiState.value = ProfileUiState.Success(user, articles, drafts)
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val currentId = currentUserId ?: return@launch
            val targetId = profileUserId ?: return@launch
            
            if (currentId == targetId) return@launch // Can't follow yourself
            
            // Optimistic UI update
            val wasFollowing = _isFollowing.value
            _isFollowing.value = !wasFollowing
            
            _isFollowLoading.value = true
            
            val result = if (wasFollowing) {
                authRepository.unfollowUser(currentId, targetId)
            } else {
                authRepository.followUser(currentId, targetId)
            }
            
            _isFollowLoading.value = false
            
            if (result.isFailure) {
                // Revert on failure
                _isFollowing.value = wasFollowing
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun uploadAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            // Upload to Supabase
            val uploadResult = articlesRepository.uploadImage(imageBytes)
            if (uploadResult.isFailure) {
                _uiState.value = ProfileUiState.Error("Failed to upload image")
                return@launch
            }
            
            val avatarUrl = uploadResult.getOrNull() ?: return@launch
            
            // Get current user ID
            val userId = authRepository.currentUser.first()?.id
            if (userId == null) {
                _uiState.value = ProfileUiState.Error("User not found")
                return@launch
            }
            
            // Update in Firestore
            val updateResult = authRepository.updateUserAvatar(userId, avatarUrl)
            if (updateResult.isFailure) {
                _uiState.value = ProfileUiState.Error("Failed to update profile")
                return@launch
            }
            
            // Reload profile
            loadProfile(null)
        }
    }

    fun deleteArticle(articleId: String) {
        viewModelScope.launch {
            currentUser.value?.id?.let { userId ->
                articlesRepository.deleteArticle(articleId, userId)
                // Reload profile after deletion is handled by Flow collection update automatically if simpler, 
                // but here we manually reload to be safe or rely on flow if it was set up as stream.
                // Our repo returns Flow, so if we are collecting, it might update automatically depending on impl.
                // Firestore listeners update automatically. 
            }
        }
    }
    
    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            draftsRepository.deleteDraft(draftId)
            // Drafts are collected from Room Flow, so UI updates automatically
        }
    }

    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            // Get current user ID
            val userId = authRepository.currentUser.first()?.id
            if (userId == null) {
                _uiState.value = ProfileUiState.Error("User not found")
                return@launch
            }
            
            // Update in Firestore
            val updateResult = authRepository.updateUserProfile(userId, name, bio)
            if (updateResult.isFailure) {
                _uiState.value = ProfileUiState.Error("Failed to update profile")
                return@launch
            }
            
            // Reload profile
            loadProfile(null)
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: User, 
        val articles: List<Article>, 
        val drafts: List<com.example.mediumclone.data.local.Draft> = emptyList()
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
