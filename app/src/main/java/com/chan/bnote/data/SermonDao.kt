package com.chan.bnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SermonDao {

	@Insert
	suspend fun insert(sermon: Sermon): Long

	@Query("SELECT * FROM sermons WHERE sermonDate = :dateMillis ORDER BY createdAt DESC")
	suspend fun getByDate(dateMillis: Long): List<Sermon>

	@Query("SELECT DISTINCT sermonDate FROM sermons")
	suspend fun getAllSermonDates(): List<Long>

	@Query("SELECT * FROM sermons WHERE bookId = :bookId ORDER BY sermonDate DESC")
	suspend fun getByBook(bookId: Int): List<Sermon>

	@Query("SELECT DISTINCT bookId FROM sermons WHERE bookId IS NOT NULL ORDER BY bookId")
	suspend fun getBooksWithSermons(): List<Int>

	@Query("SELECT * FROM sermons WHERE preacher = :preacher ORDER BY sermonDate DESC")
	suspend fun getByPreacher(preacher: String): List<Sermon>

	@Query("SELECT DISTINCT preacher FROM sermons ORDER BY preacher")
	suspend fun getAllPreachers(): List<String>
}