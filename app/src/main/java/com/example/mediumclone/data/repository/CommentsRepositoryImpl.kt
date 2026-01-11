package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CommentsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository
) : CommentsRepository {

    override fun getComments(articleId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("articles")
            .document(articleId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override fun getCommentsCount(articleId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("articles")
            .document(articleId)
            .collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(articleId: String, content: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            // Fetch user data from Firestore
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("name") ?: currentUser.displayName ?: "Anonymous"
            val userAvatar = userDoc.getString("avatarUrl")
            
            val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document()
            
            val comment = Comment(
                id = commentRef.id,
                articleId = articleId,
                userId = currentUser.uid,
                userName = userName,
                userAvatarUrl = userAvatar,
                content = content
            )
            
            commentRef.set(comment).await()
            
            // Update article's commentsCount
            firestore.collection("articles").document(articleId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(articleId: String, commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            // Verify user owns the comment
            val commentDoc = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document(commentId)
                .get()
                .await()
            
            val commentUserId = commentDoc.getString("userId")
            if (commentUserId != currentUser.uid) {
                throw Exception("You can only delete your own comments")
            }
            
            // Delete comment
            commentDoc.reference.delete().await()
            
            // Update article's commentsCount
            firestore.collection("articles").document(articleId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
