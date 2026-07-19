package com.chan.bnote.data.sermon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SermonPhotoDao {

	@Insert
	suspend fun insertAll(photos: List<SermonPhoto>)

	@Query("SELECT * FROM sermon_photos WHERE sermonId = :sermonId ORDER BY sortOrder ASC")
	suspend fun getBySermon(sermonId: Long): List<SermonPhoto>

	@Query("SELECT * FROM sermon_photos")
	suspend fun getAll(): List<SermonPhoto>

	@Query("DELETE FROM sermon_photos")
	suspend fun deleteAll()

	@Query("DELETE FROM sermon_photos WHERE sermonId = :sermonId")
	suspend fun deleteBySermon(sermonId: Long)
}