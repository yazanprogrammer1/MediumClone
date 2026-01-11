package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentsRepository {
    fun getComments(articleId: String): Flow<List<Comment>>
    fun getCommentsCount(articleId: String): Flow<Int>
    suspend fun addComment(articleId: String, content: String): Result<Unit>
    suspend fun deleteComment(articleId: String, commentId: String): Result<Unit>
}
