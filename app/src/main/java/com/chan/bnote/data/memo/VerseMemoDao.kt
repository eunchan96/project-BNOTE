package com.chan.bnote.data.memo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerseMemoDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(memo: VerseMemo)

	@Query("SELECT COUNT(*) FROM verse_memos")
	suspend fun count(): Int

	@Query("SELECT * FROM verse_memos WHERE bookId = :bookId AND chapter = :chapter")
	suspend fun getForChapter(bookId: Int, chapter: Int): List<VerseMemo>

	@Query("DELETE FROM verse_memos WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
	suspend fun delete(bookId: Int, chapter: Int, verse: Int)
}