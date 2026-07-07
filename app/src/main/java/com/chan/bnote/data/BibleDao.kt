package com.chan.bnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BibleDao {

	@Insert
	suspend fun insertAll(verses: List<BibleVerse>)

	@Query("SELECT COUNT(*) FROM bible_verses")
	suspend fun count(): Int

	@Query("SELECT DISTINCT bookId FROM bible_verses ORDER BY bookId")
	suspend fun getBookIds(): List<Int>

	@Query("SELECT DISTINCT chapter FROM bible_verses WHERE bookId = :bookId ORDER BY chapter")
	suspend fun getChapters(bookId: Int): List<Int>

	@Query("SELECT * FROM bible_verses WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
	suspend fun getVerses(bookId: Int, chapter: Int): List<BibleVerse>
}