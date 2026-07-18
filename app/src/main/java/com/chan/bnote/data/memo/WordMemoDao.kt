package com.chan.bnote.data.memo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface WordMemoDao {

	@Insert
	suspend fun insert(memo: WordMemo): Long

	@Query("SELECT COUNT(*) FROM word_memos")
	suspend fun count(): Int

	@Update
	suspend fun update(memo: WordMemo)

	@Delete
	suspend fun delete(memo: WordMemo)

	@Query("SELECT * FROM word_memos WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter")
	suspend fun getForChapter(translation: String, bookId: Int, chapter: Int): List<WordMemo>
}