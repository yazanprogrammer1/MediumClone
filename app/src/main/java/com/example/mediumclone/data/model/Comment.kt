package com.example.mediumclone.data.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val articleId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
