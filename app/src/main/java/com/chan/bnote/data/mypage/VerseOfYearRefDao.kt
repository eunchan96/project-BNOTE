package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VerseOfYearRefDao {

	@Insert
	suspend fun insertAll(refs: List<VerseOfYearRef>)

	@Query("SELECT * FROM verse_of_year_refs WHERE year = :year ORDER BY id ASC")
	suspend fun getByYear(year: Int): List<VerseOfYearRef>

	@Query("DELETE FROM verse_of_year_refs WHERE year = :year")
	suspend fun deleteByYear(year: Int)
}