package com.example.mediumclone.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isPremium: Boolean = false
)
