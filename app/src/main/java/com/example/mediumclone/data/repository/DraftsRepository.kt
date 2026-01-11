package com.example.mediumclone.data.repository

import com.example.mediumclone.data.local.Draft
import kotlinx.coroutines.flow.Flow

interface DraftsRepository {
    fun getAllDrafts(): Flow<List<Draft>>
    suspend fun getDraft(draftId: String): Draft?
    suspend fun saveDraft(draft: Draft)
    suspend fun deleteDraft(draftId: String)
}
