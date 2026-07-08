package com.chan.bnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkDao {

	@Query("SELECT * FROM bible_bookmarks WHERE bookId = :bookId AND chapter = :chapter")
	suspend fun getBookmarksForChapter(bookId: Int, chapter: Int): List<BibleBookmark>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(bookmark: BibleBookmark)

	@Query(
		"""
        SELECT bv.bookId as bookId, bv.chapter as chapter, bv.verse as verse, bv.text as text
        FROM bible_bookmarks bb
        JOIN bible_verses bv 
            ON bv.bookId = bb.bookId AND bv.chapter = bb.chapter AND bv.verse = bb.verse
        WHERE bb.isFavorite = 1
        ORDER BY bb.updatedAt DESC
        """
	)
	suspend fun getFavoriteVerses(): List<FavoriteVerseRow>
}

data class FavoriteVerseRow(
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val text: String
)