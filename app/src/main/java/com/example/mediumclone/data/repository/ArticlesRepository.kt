package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.Article
import kotlinx.coroutines.flow.Flow

interface ArticlesRepository {
    fun getArticles(): Flow<List<Article>>
    fun getArticlesByUser(userId: String): Flow<List<Article>>
    suspend fun getArticle(articleId: String): Result<Article>
    fun searchArticlesByTag(tag: String): Flow<List<Article>>
    fun searchArticles(query: String): Flow<List<Article>>
    suspend fun createArticle(title: String, content: String, coverImageUrl: String?, tags: List<String>): Result<Unit>
    suspend fun uploadImage(byteArray: ByteArray): Result<String> // Returns URL
    suspend fun likeArticle(articleId: String, userId: String): Result<Unit>
    suspend fun unlikeArticle(articleId: String, userId: String): Result<Unit>
    fun isArticleLiked(articleId: String, userId: String): Flow<Boolean>
    suspend fun bookmarkArticle(articleId: String, userId: String): Result<Unit>
    suspend fun unbookmarkArticle(articleId: String, userId: String): Result<Unit>
    fun isArticleBookmarked(articleId: String, userId: String): Flow<Boolean>
    fun getBookmarkedArticles(userId: String): Flow<List<Article>>
    suspend fun deleteArticle(articleId: String, userId: String): Result<Unit>
    suspend fun updateArticle(articleId: String, title: String, content: String, coverImageUrl: String?, tags: List<String>): Result<Unit>
    fun getComments(articleId: String): Flow<List<com.example.mediumclone.data.model.Comment>>
    suspend fun addComment(articleId: String, content: String): Result<Unit>
    suspend fun deleteComment(articleId: String, commentId: String): Result<Unit>
    suspend fun likeComment(articleId: String, commentId: String, userId: String): Result<Unit>
    suspend fun unlikeComment(articleId: String, commentId: String, userId: String): Result<Unit>
    suspend fun updateComment(articleId: String, commentId: String, newContent: String): Result<Unit>
}
