package com.chan.bnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerseOfYearDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(entry: VerseOfYear)

	@Query("SELECT * FROM verse_of_year WHERE year = :year LIMIT 1")
	suspend fun getByYear(year: Int): VerseOfYear?

	@Query("SELECT * FROM verse_of_year ORDER BY year DESC")
	suspend fun getAll(): List<VerseOfYear>
}