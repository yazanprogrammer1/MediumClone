package com.example.mediumclone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Draft::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}
