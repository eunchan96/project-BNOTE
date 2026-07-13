package com.chan.bnote.data.partialhighlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PartialHighlightDao {

	@Insert
	suspend fun insert(highlight: PartialHighlight): Long

	@Delete
	suspend fun delete(highlight: PartialHighlight)

	@Query("SELECT * FROM partial_highlights WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter")
	suspend fun getForChapter(
		translation: String,
		bookId: Int,
		chapter: Int
	): List<PartialHighlight>

	@Query(
		"DELETE FROM partial_highlights WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter AND verse = :verse"
	)
	suspend fun deleteAllForVerse(translation: String, bookId: Int, chapter: Int, verse: Int)
}