package com.example.mediumclone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY lastModified DESC")
    fun getAllDrafts(): Flow<List<Draft>>

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: String): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: Draft)

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteDraft(draftId: String)
}
