package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.Article
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

class ArticlesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val supabase: SupabaseClient
) : ArticlesRepository {

    override fun getArticles(): Flow<List<Article>> = callbackFlow {
        val listener = firestore.collection("articles")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val articles = snapshot?.toObjects(Article::class.java) ?: emptyList()
                trySend(articles)
            }
        awaitClose { listener.remove() }
    }

    override fun getArticlesByUser(userId: String): Flow<List<Article>> = callbackFlow {
        val listener = firestore.collection("articles")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val articles = snapshot?.toObjects(Article::class.java) ?: emptyList()
                trySend(articles)
            }
        awaitClose { listener.remove() }
    }

    override fun searchArticlesByTag(tag: String): Flow<List<Article>> = callbackFlow {
        val listener = firestore.collection("articles")
            .whereArrayContains("tags", tag)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Get articles and sort by timestamp in code
                val articles = snapshot?.toObjects(Article::class.java)
                    ?.sortedByDescending { it.timestamp.toDate() } 
                    ?: emptyList()
                trySend(articles)
            }
        awaitClose { listener.remove() }
    }

    override fun searchArticles(query: String): Flow<List<Article>> = callbackFlow {
        // For a "Smart" search without external services like Algolia,
        // we'll fetch recent articles and filter client-side to allow title/content/author matching.
        // Precision Update: Prioritize Tags and Title over Content.
        val listener = firestore.collection("articles")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Limit to relevant recent articles
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allArticles = snapshot?.toObjects(Article::class.java) ?: emptyList()
                val q = query.trim()
                
                val filtered = allArticles.filter { article ->
                    // 1. Tag Match (Highest priority for topic search)
                    val tagMatch = article.tags.any { it.equals(q, ignoreCase = true) } ||
                                   article.tags.any { it.contains(q, ignoreCase = true) }
                    
                    // 2. Title Match (High priority)
                    val titleMatch = article.title.contains(q, ignoreCase = true)
                    
                    // 3. Content Match (Stricter: Avoid noisy partials if short query)
                    // Only check content if query length > 3 to avoid "j", "a" matching everything
                    // Or if query is specific.
                    val contentMatch = if (q.length > 2) article.content.contains(q, ignoreCase = true) else false
                    
                    // 4. Author: We want people who *publish* about the topic, not just mismatching names.
                    // But if searching for a person name "Ali", we want his articles.
                    val authorMatch = article.authorName.contains(q, ignoreCase = true)

                    tagMatch || titleMatch || contentMatch || authorMatch
                }.sortedByDescending { article ->
                     // Sort relevance: Tag/Title exact > Title partial > Content/Author
                     var score = 0
                     if (article.tags.any { it.equals(q, ignoreCase = true) }) score += 10
                     if (article.title.equals(q, ignoreCase = true)) score += 5
                     if (article.title.contains(q, ignoreCase = true)) score += 3
                     score
                }
                
                trySend(filtered)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getArticle(articleId: String): Result<Article> {
        return try {
            val snapshot = firestore.collection("articles").document(articleId).get().await()
            val article = snapshot.toObject(Article::class.java)
            // Simulation: Mark every 3rd article as Premium
            if (article != null) {
                val isPremium = Math.abs(article.id.hashCode() % 3) == 0
                Result.success(article.copy(isPremium = isPremium))
            } else {
                Result.failure(Exception("Article not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createArticle(
        title: String,
        content: String,
        coverImageUrl: String?,
        tags: List<String>
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            val newDocRef = firestore.collection("articles").document()
            
            // Fetch user data from Firestore to get the correct name
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("name") ?: currentUser.displayName ?: "Anonymous"
            val userAvatar = userDoc.getString("avatarUrl")
            
            // Calculate simplistic read time (approx 200 words per minute)
            val wordCount = content.split("\\s+".toRegex()).size
            val readTime = max(1, wordCount / 200)

            val article = Article(
                id = newDocRef.id,
                title = title,
                content = content,
                coverImageUrl = coverImageUrl,
                authorId = currentUser.uid,
                authorName = userName,
                authorAvatarUrl = userAvatar,
                tags = tags,
                readTimeMinutes = readTime
            )

            newDocRef.set(article).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(byteArray: ByteArray): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage.from("images")
            bucket.upload(fileName, byteArray)
            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likeArticle(articleId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val articleRef = firestore.collection("articles").document(articleId)
                val snapshot = transaction.get(articleRef)
                
                val currentLikedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val currentLikesCount = snapshot.getLong("likesCount")?.toInt() ?: 0
                
                // Add user to likedBy if not already present
                if (!currentLikedBy.contains(userId)) {
                    transaction.update(articleRef, mapOf(
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "likesCount" to currentLikesCount + 1
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeArticle(articleId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val articleRef = firestore.collection("articles").document(articleId)
                val snapshot = transaction.get(articleRef)
                
                val currentLikedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val currentLikesCount = snapshot.getLong("likesCount")?.toInt() ?: 0
                
                // Remove user from likedBy if present
                if (currentLikedBy.contains(userId)) {
                    transaction.update(articleRef, mapOf(
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "likesCount" to maxOf(0, currentLikesCount - 1)
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isArticleLiked(articleId: String, userId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("articles").document(articleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val likedBy = snapshot?.get("likedBy") as? List<*> ?: emptyList<String>()
                trySend(likedBy.contains(userId))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun bookmarkArticle(articleId: String, userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Add to user's bookmarks subcollection
            val bookmarkRef = firestore.collection("users")
                .document(userId)
                .collection("bookmarks")
                .document(articleId)
            batch.set(bookmarkRef, mapOf("timestamp" to com.google.firebase.Timestamp.now()))
            
            // Add to article's bookmarkedBy array
            val articleRef = firestore.collection("articles").document(articleId)
            batch.update(articleRef, "bookmarkedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unbookmarkArticle(articleId: String, userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Remove from user's bookmarks subcollection
            val bookmarkRef = firestore.collection("users")
                .document(userId)
                .collection("bookmarks")
                .document(articleId)
            batch.delete(bookmarkRef)
            
            // Remove from article's bookmarkedBy array
            val articleRef = firestore.collection("articles").document(articleId)
            batch.update(articleRef, "bookmarkedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isArticleBookmarked(articleId: String, userId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("bookmarks")
            .document(articleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookmarkedArticles(userId: String): Flow<List<Article>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("bookmarks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { bookmarksSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val articleIds = bookmarksSnapshot?.documents?.map { it.id } ?: emptyList()
                
                if (articleIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Fetch articles by IDs
                firestore.collection("articles")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), articleIds)
                    .get()
                    .addOnSuccessListener { articlesSnapshot ->
                        val articles = articlesSnapshot.toObjects(Article::class.java)
                        trySend(articles)
                    }
                    .addOnFailureListener { e ->
                        close(e)
                    }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteArticle(articleId: String, userId: String): Result<Unit> {
        return try {
            // Verify user owns the article
            val articleDoc = firestore.collection("articles").document(articleId).get().await()
            val article = articleDoc.toObject(Article::class.java)
            
            if (article?.authorId != userId) {
                return Result.failure(Exception("You don't have permission to delete this article"))
            }
            
            // Delete the article document
            firestore.collection("articles").document(articleId).delete().await()
            
            // Delete cover image from Supabase if exists
            article.coverImageUrl?.let { imageUrl ->
                try {
                    // Extract filename from URL and delete
                    val filename = imageUrl.substringAfterLast("/")
                    supabase.storage.from("articles").delete(filename)
                } catch (e: Exception) {
                    // Ignore storage errors, article is already deleted
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateArticle(
        articleId: String,
        title: String,
        content: String,
        coverImageUrl: String?,
        tags: List<String>
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            // Validate user permission (server-side rules should also enforce this)
            val articleRef = firestore.collection("articles").document(articleId)
            val articleDoc = articleRef.get().await()
            val article = articleDoc.toObject(Article::class.java)
            
            if (article?.authorId != currentUser.uid) {
                return Result.failure(Exception("You don't have permission to edit this article"))
            }

            // Calculate read time
            val wordCount = content.split("\\s+".toRegex()).size
            val readTime = max(1, wordCount / 200)

            val updates = mutableMapOf<String, Any>(
                "title" to title,
                "content" to content,
                "tags" to tags,
                "readTimeMinutes" to readTime,
                "timestamp" to com.google.firebase.Timestamp.now() // Optional: update timestamp on edit
            )
            
            if (coverImageUrl != null) {
                updates["coverImageUrl"] = coverImageUrl
            }

            articleRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(articleId: String): Flow<List<com.example.mediumclone.data.model.Comment>> = callbackFlow {
        val listener = firestore.collection("articles")
            .document(articleId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(com.example.mediumclone.data.model.Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(articleId: String, content: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            // Get user details
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("name") ?: currentUser.displayName ?: "Anonymous"
            val userAvatar = userDoc.getString("avatarUrl") ?: currentUser.photoUrl?.toString()

            val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document()

            val comment = com.example.mediumclone.data.model.Comment(
                id = commentRef.id,
                articleId = articleId,
                userId = currentUser.uid,
                userName = userName,
                userAvatarUrl = userAvatar,
                content = content,
                timestamp = com.google.firebase.Timestamp.now()
            )

            firestore.runTransaction { transaction ->
                val articleRef = firestore.collection("articles").document(articleId)
                
                // Add comment
                transaction.set(commentRef, comment)
                
                // Increment comments count on article
                transaction.update(articleRef, "commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun deleteComment(articleId: String, commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document(commentId)
            
            // Verify ownership
            val commentDoc = commentRef.get().await()
            val comment = commentDoc.toObject(com.example.mediumclone.data.model.Comment::class.java)
            
            if (comment?.userId != currentUser.uid) {
                return Result.failure(Exception("You don't have permission to delete this comment"))
            }

            firestore.runTransaction { transaction ->
                val articleRef = firestore.collection("articles").document(articleId)
                
                // Read article count first
                val snapshot = transaction.get(articleRef)
                val currentCount = snapshot.getLong("commentsCount")?.toInt() ?: 0
                
                // Delete comment
                transaction.delete(commentRef)
                
                // Decrement comments count
                transaction.update(articleRef, "commentsCount", maxOf(0, currentCount - 1))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likeComment(articleId: String, commentId: String, userId: String): Result<Unit> {
        return try {
            val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document(commentId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val currentLikedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val currentLikesCount = snapshot.getLong("likesCount")?.toInt() ?: 0

                if (!currentLikedBy.contains(userId)) {
                    transaction.update(commentRef, mapOf(
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "likesCount" to currentLikesCount + 1
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeComment(articleId: String, commentId: String, userId: String): Result<Unit> {
        return try {
             val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document(commentId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val currentLikedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val currentLikesCount = snapshot.getLong("likesCount")?.toInt() ?: 0

                if (currentLikedBy.contains(userId)) {
                    transaction.update(commentRef, mapOf(
                        "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "likesCount" to maxOf(0, currentLikesCount - 1)
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateComment(articleId: String, commentId: String, newContent: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Not logged in")
            
            val commentRef = firestore.collection("articles")
                .document(articleId)
                .collection("comments")
                .document(commentId)
            
            // Verify ownership
            val commentDoc = commentRef.get().await()
            val comment = commentDoc.toObject(com.example.mediumclone.data.model.Comment::class.java)
            
            if (comment?.userId != currentUser.uid) {
                return Result.failure(Exception("You don't have permission to edit this comment"))
            }

            commentRef.update("content", newContent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
