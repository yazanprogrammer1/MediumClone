package com.example.mediumclone.data.model

import com.google.firebase.Timestamp

data class Article(
    val id: String = "",
    val title: String = "",
    val content: String = "", // Can be markdown or HTML
    val coverImageUrl: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val tags: List<String> = emptyList(),
    val timestamp: Timestamp = Timestamp.now(),
    val readTimeMinutes: Int = 1,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val bookmarkedBy: List<String> = emptyList(),
    val isPremium: Boolean = false
)
