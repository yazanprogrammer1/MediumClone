package com.example.mediumclone.di

import com.example.mediumclone.data.repository.DraftsRepository
import com.example.mediumclone.data.repository.DraftsRepositoryImpl
import com.example.mediumclone.data.repository.SubscriptionRepository
import com.example.mediumclone.data.repository.SubscriptionRepositoryImpl
import com.example.mediumclone.data.repository.AuthRepository
import com.example.mediumclone.data.repository.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.mediumclone.data.repository.ArticlesRepository
import com.example.mediumclone.data.repository.ArticlesRepositoryImpl
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepositoryImpl(auth, firestore)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): SubscriptionRepository {
        return SubscriptionRepositoryImpl(context, auth, firestore)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): io.github.jan.supabase.SupabaseClient {
        return io.github.jan.supabase.createSupabaseClient(
            supabaseUrl = "https://ldrxkscsbzrmqurjdfzg.supabase.co", // User must replace this
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imxkcnhrc2NzYnpybXF1cmpkZnpnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjcyMDQwNTIsImV4cCI6MjA4Mjc4MDA1Mn0.USYI8KesRr6EQyagXVSNIiFxAZbb3lk9P_HDkb5TKOA"  // User must replace this
        ) {
            install(io.github.jan.supabase.storage.Storage)
        }
    }

    @Provides
    @Singleton
    fun provideArticlesRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        supabase: SupabaseClient
    ): ArticlesRepository {
        return ArticlesRepositoryImpl(firestore, auth, supabase)
    }

    @Provides
    @Singleton
    fun provideGenerativeAIService(): com.example.mediumclone.data.service.GenerativeAIService {
        // Mock service replaced with Real service
        // return com.example.mediumclone.data.service.MockGenerativeAIService()
        return com.example.mediumclone.data.service.RealGenerativeAIService()
    }

    @Provides
    @Singleton
    fun provideCommentsRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        authRepository: AuthRepository
    ): com.example.mediumclone.data.repository.CommentsRepository =
        com.example.mediumclone.data.repository.CommentsRepositoryImpl(firestore, auth, authRepository)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.example.mediumclone.data.local.AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.example.mediumclone.data.local.AppDatabase::class.java,
            "medium-clone-db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDraftDao(appDatabase: com.example.mediumclone.data.local.AppDatabase): com.example.mediumclone.data.local.DraftDao {
        return appDatabase.draftDao()
    }

    @Provides
    @Singleton
    fun provideDraftsRepository(
        draftDao: com.example.mediumclone.data.local.DraftDao
    ): com.example.mediumclone.data.repository.DraftsRepository {
        return com.example.mediumclone.data.repository.DraftsRepositoryImpl(draftDao)
    }
}
