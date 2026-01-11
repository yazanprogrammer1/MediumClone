package com.example.mediumclone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey
    val id: String, // UUID generated locally
    val title: String,
    val content: String,
    val coverImageUrl: String?,
    val tags: String, // Comma separated tags
    val lastModified: Long
)
