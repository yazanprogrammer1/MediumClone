package com.example.mediumclone.data.repository

import com.example.mediumclone.data.local.Draft
import com.example.mediumclone.data.local.DraftDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DraftsRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao
) : DraftsRepository {
    override fun getAllDrafts(): Flow<List<Draft>> = draftDao.getAllDrafts()
    
    override suspend fun getDraft(draftId: String): Draft? = draftDao.getDraftById(draftId)
    
    override suspend fun saveDraft(draft: Draft) = draftDao.insertDraft(draft)
    
    override suspend fun deleteDraft(draftId: String) = draftDao.deleteDraft(draftId)
}
