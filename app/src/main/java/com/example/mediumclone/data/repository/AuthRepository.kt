package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit>
    suspend fun updateUserProfile(userId: String, name: String, bio: String): Result<Unit>
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit>
    fun getFollowingIds(userId: String): Flow<List<String>>
    fun isFollowing(currentUserId: String, targetUserId: String): Flow<Boolean>
}
