package com.example.mediumclone.data.repository

import com.example.mediumclone.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Fetch full user profile from Firestore
                firestore.collection("users").document(firebaseUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)
                        trySend(user)
                    }
                    .addOnFailureListener {
                        // Fallback to basic user object
                        trySend(
                            User(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email ?: ""
                            )
                        )
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            // Create user profile in Firestore
            if (firebaseUser != null) {
                val newUser = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email
                )
                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                
                // Update display name in Firebase Auth
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("avatarUrl", avatarUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(userId: String, name: String, bio: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "bio" to bio
            )
            firestore.collection("users").document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Add to current user's following subcollection
            val followingRef = firestore.collection("users")
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
            batch.set(followingRef, mapOf("timestamp" to com.google.firebase.Timestamp.now()))
            
            // Add to target user's followers subcollection
            val followerRef = firestore.collection("users")
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)
            batch.set(followerRef, mapOf("timestamp" to com.google.firebase.Timestamp.now()))
            
            // Increment current user's followingCount
            val currentUserRef = firestore.collection("users").document(currentUserId)
            batch.update(currentUserRef, "followingCount", com.google.firebase.firestore.FieldValue.increment(1))
            
            // Increment target user's followersCount
            val targetUserRef = firestore.collection("users").document(targetUserId)
            batch.update(targetUserRef, "followersCount", com.google.firebase.firestore.FieldValue.increment(1))
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Remove from current user's following subcollection
            val followingRef = firestore.collection("users")
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
            batch.delete(followingRef)
            
            // Remove from target user's followers subcollection
            val followerRef = firestore.collection("users")
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)
            batch.delete(followerRef)
            
            // Decrement current user's followingCount
            val currentUserRef = firestore.collection("users").document(currentUserId)
            batch.update(currentUserRef, "followingCount", com.google.firebase.firestore.FieldValue.increment(-1))
            
            // Decrement target user's followersCount
            val targetUserRef = firestore.collection("users").document(targetUserId)
            batch.update(targetUserRef, "followersCount", com.google.firebase.firestore.FieldValue.increment(-1))
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isFollowing(currentUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("users")
            .document(currentUserId)
            .collection("following")
            .document(targetUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    override fun getFollowingIds(userId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id } ?: emptyList()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }
}
