package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerseMemorizationProgressDao {

	@Query("SELECT * FROM verse_memorization_progress WHERE verseRefId = :verseRefId LIMIT 1")
	suspend fun getByRefId(verseRefId: Long): VerseMemorizationProgress?

	@Query("SELECT * FROM verse_memorization_progress")
	suspend fun getAll(): List<VerseMemorizationProgress>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(progress: VerseMemorizationProgress)
}