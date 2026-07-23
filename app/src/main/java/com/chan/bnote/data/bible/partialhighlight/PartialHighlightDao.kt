package com.chan.bnote.data.bible.partialhighlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PartialHighlightDao {

	@Insert
	suspend fun insert(highlight: PartialHighlight): Long

	@Update
	suspend fun update(highlight: PartialHighlight)

	@Query("SELECT COUNT(*) FROM partial_highlights")
	suspend fun countAll(): Int

	@Query(
		"""
        SELECT colorHex, COUNT(*) as count FROM partial_highlights
        GROUP BY colorHex ORDER BY count DESC LIMIT 1
        """
	)
	suspend fun getMostUsedColor(): HighlightColorUsage?

	@Delete
	suspend fun delete(highlight: PartialHighlight)

	@Query("SELECT * FROM partial_highlights WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter")
	suspend fun getForChapter(
		translation: String,
		bookId: Int,
		chapter: Int
	): List<PartialHighlight>

	@Query("SELECT * FROM partial_highlights ORDER BY bookId ASC, chapter ASC, verse ASC")
	suspend fun getAll(): List<PartialHighlight>

	@Query("DELETE FROM partial_highlights")
	suspend fun deleteAll()

	@Query(
		"DELETE FROM partial_highlights WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter AND verse = :verse"
	)
	suspend fun deleteAllForVerse(translation: String, bookId: Int, chapter: Int, verse: Int)

	@Query(
		"""
        DELETE FROM partial_highlights
        WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter AND verse = :verse
          AND NOT (endOffset <= :start OR startOffset >= :end)
        """
	)
	suspend fun deleteOverlapping(
		translation: String,
		bookId: Int,
		chapter: Int,
		verse: Int,
		start: Int,
		end: Int
	)
}

data class HighlightColorUsage(val colorHex: String, val count: Int)