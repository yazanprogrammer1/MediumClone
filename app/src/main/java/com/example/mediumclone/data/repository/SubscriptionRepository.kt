package com.example.mediumclone.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SubscriptionRepository {
    val isPremium: StateFlow<Boolean>
    fun upgradeToPremium()
    fun getSubscriptionStatus(): Boolean
    fun downgrade() // For testing
}

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : SubscriptionRepository {

    private val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
    
    // Start with data from Prefs for instant load, then sync with Firestore
    private val _isPremium = MutableStateFlow(prefs.getBoolean("is_premium", false))
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        // Listen for real-time updates from Firestore
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Listen to user document changes
                firestore.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        
                        if (snapshot != null && snapshot.exists()) {
                            val isPro = snapshot.getBoolean("isPremium") ?: false
                            // Sync Prefs
                            if (_isPremium.value != isPro) {
                                prefs.edit { putBoolean("is_premium", isPro) }
                                _isPremium.value = isPro
                            }
                        }
                    }
            } else {
                // User logged out, reset to false
                _isPremium.value = false
                prefs.edit { putBoolean("is_premium", false) }
            }
        }
    }

    override fun upgradeToPremium() {
        // 1. Optimistic Update (Local)
        prefs.edit { putBoolean("is_premium", true) }
        _isPremium.value = true
        
        // 2. Persistent Update (Remote)
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .update("isPremium", true)
                .addOnFailureListener {
                    // Ideally handle rollback or retry, but for "Mock Payment" this is acceptable consistency
                    // If it fails, the SnapshotListener won't fire, but we already set local state
                }
        }
    }

    override fun getSubscriptionStatus(): Boolean {
        return _isPremium.value
    }

    override fun downgrade() {
        // Downgrade both local and remote
        prefs.edit { putBoolean("is_premium", false) }
        _isPremium.value = false
        
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .update("isPremium", false)
        }
    }
}
