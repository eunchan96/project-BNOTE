package com.chan.bnote.data.mypage.verseofyear

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VerseOfYearRefDao {

	@Insert
	suspend fun insertAll(refs: List<VerseOfYearRef>)

	@Query("SELECT * FROM verse_of_year_refs WHERE year = :year ORDER BY id ASC")
	suspend fun getByYear(year: Int): List<VerseOfYearRef>

	// 암송 연습용: 연도 상관없이 저장된 모든 구절
	@Query("SELECT * FROM verse_of_year_refs ORDER BY year DESC, id ASC")
	suspend fun getAllRefs(): List<VerseOfYearRef>

	@Query("DELETE FROM verse_of_year_refs")
	suspend fun deleteAll()

	@Query("DELETE FROM verse_of_year_refs WHERE year = :year")
	suspend fun deleteByYear(year: Int)
}