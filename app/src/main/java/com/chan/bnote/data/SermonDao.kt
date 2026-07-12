package com.chan.bnote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SermonDao {

	@Insert
	suspend fun insert(sermon: Sermon): Long

	@Update
	suspend fun update(sermon: Sermon)

	@Delete
	suspend fun delete(sermon: Sermon)

	@Query("SELECT * FROM sermons WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): Sermon?

	@Query("SELECT * FROM sermons WHERE sermonDate = :dateMillis ORDER BY createdAt DESC")
	suspend fun getByDate(dateMillis: Long): List<Sermon>

	@Query("SELECT DISTINCT sermonDate FROM sermons")
	suspend fun getAllSermonDates(): List<Long>

	@Query("SELECT * FROM sermons WHERE preacher = :preacher")
	suspend fun getByPreacher(preacher: String): List<Sermon>

	@Query("SELECT DISTINCT preacher FROM sermons ORDER BY preacher")
	suspend fun getAllPreachers(): List<String>

	@Query(
		"""
        SELECT * FROM sermons 
        WHERE title LIKE '%' || :keyword || '%' OR memo LIKE '%' || :keyword || '%'
        ORDER BY sermonDate DESC
        """
	)
	suspend fun search(keyword: String): List<Sermon>

	@Query(
		"""
    SELECT s.sermonDate as sermonDate, c.colorHex as colorHex
    FROM sermons s
    LEFT JOIN sermon_categories c ON s.categoryId = c.id
    WHERE s.sermonDate >= :startMillis AND s.sermonDate < :endMillis
    ORDER BY s.sermonDate, s.createdAt
    """
	)
	suspend fun getSermonMarkersInRange(startMillis: Long, endMillis: Long): List<SermonMarker>
}

data class SermonMarker(val sermonDate: Long, val colorHex: String?)