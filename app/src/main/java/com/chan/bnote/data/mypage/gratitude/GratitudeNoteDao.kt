package com.chan.bnote.data.mypage.gratitude

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GratitudeNoteDao {

	@Insert
	suspend fun insert(note: GratitudeNote): Long

	@Delete
	suspend fun delete(note: GratitudeNote)

	@Query("SELECT * FROM gratitude_notes WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): GratitudeNote?

	@Query("SELECT * FROM gratitude_notes WHERE date = :dateMillis ORDER BY createdAt DESC")
	suspend fun getByDate(dateMillis: Long): List<GratitudeNote>

	@Query("SELECT * FROM gratitude_notes")
	suspend fun getAll(): List<GratitudeNote>

	@Query("SELECT DISTINCT date FROM gratitude_notes WHERE date >= :startMillis AND date < :endMillis")
	suspend fun getDatesInRange(startMillis: Long, endMillis: Long): List<Long>

	@Query("DELETE FROM gratitude_notes")
	suspend fun deleteAll()
}