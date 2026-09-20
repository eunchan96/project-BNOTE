package com.chan.bnote.data.bible.bookmark

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkDao {

	@Query("SELECT * FROM bible_bookmarks WHERE bookId = :bookId AND chapter = :chapter")
	suspend fun getBookmarksForChapter(bookId: Int, chapter: Int): List<BibleBookmark>

	@Query("SELECT * FROM bible_bookmarks")
	suspend fun getAll(): List<BibleBookmark>

	@Query("DELETE FROM bible_bookmarks")
	suspend fun deleteAll()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(bookmark: BibleBookmark)

	@Query("SELECT COUNT(*) FROM bible_bookmarks WHERE isBookmarked = 1")
	suspend fun countBookmarks(): Int

	@Query(
		"UPDATE bible_bookmarks SET isBookmarked = 0 WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse"
	)
	suspend fun removeBookmark(bookId: Int, chapter: Int, verse: Int)

	@Query(
		"""
        SELECT bv.bookId as bookId, bv.chapter as chapter, bv.verse as verse, bv.text as text
        FROM bible_bookmarks bb
        JOIN bible_verses bv 
            ON bv.bookId = bb.bookId AND bv.chapter = bb.chapter AND bv.verse = bb.verse
        WHERE bb.isBookmarked = 1 AND bv.translation = :translation
        ORDER BY bb.updatedAt DESC
        """
	)
	suspend fun getBookmarkedVerses(translation: String): List<BookmarkedVerseRow>
}

data class BookmarkedVerseRow(
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val text: String
)