package com.chan.bnote.data.bible.memo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VerseMemoDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(memo: VerseMemo)

	@Insert
	suspend fun insert(memo: VerseMemo): Long

	@Update
	suspend fun update(memo: VerseMemo)

	@Delete
	suspend fun delete(memo: VerseMemo)

	@Query(
		"SELECT * FROM verse_memos WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse ORDER BY id ASC"
	)
	suspend fun getAtPosition(bookId: Int, chapter: Int, verse: Int): List<VerseMemo>

	@Query("SELECT COUNT(*) FROM verse_memos")
	suspend fun count(): Int

	@Query("SELECT * FROM verse_memos WHERE bookId = :bookId AND chapter = :chapter")
	suspend fun getForChapter(bookId: Int, chapter: Int): List<VerseMemo>

	@Query("SELECT * FROM verse_memos ORDER BY bookId ASC, chapter ASC, verse ASC")
	suspend fun getAll(): List<VerseMemo>

	@Query("SELECT * FROM verse_memos ORDER BY updatedAt DESC LIMIT :limit")
	suspend fun getRecent(limit: Int): List<VerseMemo>

	@Query("DELETE FROM verse_memos")
	suspend fun deleteAll()

	@Query("SELECT * FROM verse_memos WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): VerseMemo?

	@Query("DELETE FROM verse_memos WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
	suspend fun delete(bookId: Int, chapter: Int, verse: Int)
}