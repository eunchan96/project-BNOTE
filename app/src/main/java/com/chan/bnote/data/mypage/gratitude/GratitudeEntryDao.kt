package com.chan.bnote.data.mypage.gratitude

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GratitudeEntryDao {

	@Insert
	suspend fun insertAll(entries: List<GratitudeEntry>)

	@Query("SELECT * FROM gratitude_entries WHERE noteId = :noteId ORDER BY sortOrder")
	suspend fun getByNote(noteId: Long): List<GratitudeEntry>

	@Query("DELETE FROM gratitude_entries WHERE noteId = :noteId")
	suspend fun deleteByNote(noteId: Long)

	@Query("DELETE FROM gratitude_entries")
	suspend fun deleteAll()
}