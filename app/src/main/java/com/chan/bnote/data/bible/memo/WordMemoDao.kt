package com.chan.bnote.data.bible.memo

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

	@Query("SELECT * FROM word_memos ORDER BY bookId ASC, chapter ASC, verse ASC, startOffset ASC")
	suspend fun getAll(): List<WordMemo>

	@Query("SELECT * FROM word_memos ORDER BY updatedAt DESC LIMIT :limit")
	suspend fun getRecent(limit: Int): List<WordMemo>

	@Query("DELETE FROM word_memos")
	suspend fun deleteAll()

	@Query("SELECT * FROM word_memos WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): WordMemo?

	@Query(
		"""
    SELECT * FROM word_memos
    WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter AND verse = :verse
      AND startOffset = :startOffset AND endOffset = :endOffset AND segment = :segment
    ORDER BY id ASC
    """
	)
	suspend fun getAtPosition(
		translation: String,
		bookId: Int,
		chapter: Int,
		verse: Int,
		startOffset: Int,
		endOffset: Int,
		segment: Int
	): List<WordMemo>

	@Query(
		"""
    SELECT * FROM word_memos
    WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter AND verse = :verse
      AND segment = :segment
    ORDER BY startOffset ASC, endOffset ASC
    """
	)
	suspend fun getForVerseSegment(
		translation: String,
		bookId: Int,
		chapter: Int,
		verse: Int,
		segment: Int
	): List<WordMemo>
}