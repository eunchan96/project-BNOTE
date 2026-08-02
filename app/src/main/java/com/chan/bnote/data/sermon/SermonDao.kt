package com.chan.bnote.data.sermon

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SermonDao {

	@Insert
	suspend fun insert(sermon: Sermon): Long

	@Query("SELECT COUNT(*) FROM sermons")
	suspend fun count(): Int

	@Update
	suspend fun update(sermon: Sermon)

	@Delete
	suspend fun delete(sermon: Sermon)

	@Query("SELECT * FROM sermons WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): Sermon?

	@Query("SELECT * FROM sermons")
	suspend fun getAll(): List<Sermon>

	@Query("DELETE FROM sermons")
	suspend fun deleteAll()

	@Query("SELECT * FROM sermons ORDER BY createdAt DESC LIMIT :limit")
	suspend fun getRecent(limit: Int): List<Sermon>

	@Query("SELECT * FROM sermons WHERE sermonDate = :dateMillis ORDER BY createdAt DESC")
	suspend fun getByDate(dateMillis: Long): List<Sermon>

	@Query("SELECT DISTINCT sermonDate FROM sermons")
	suspend fun getAllSermonDates(): List<Long>

	@Query("SELECT * FROM sermons WHERE preacherId = :preacherId ORDER BY sermonDate DESC")
	suspend fun getByPreacherId(preacherId: Long): List<Sermon>

	@Query(
		"""
        SELECT * FROM sermons 
        WHERE title LIKE '%' || :keyword || '%' OR memo LIKE '%' || :keyword || '%'
        ORDER BY sermonDate DESC
        """
	)
	suspend fun search(keyword: String): List<Sermon>

	@Query(
		"""
        SELECT * FROM sermons 
        WHERE id IN (
            SELECT sermonId FROM sermon_bible_refs 
            WHERE startBookId = :bookId AND startChapter <= :chapter AND endChapter >= :chapter
        )
        ORDER BY sermonDate DESC
        """
	)
	suspend fun getByBookChapter(bookId: Int, chapter: Int): List<Sermon>

	@Query(
		"""
        SELECT s.id as id, r.startChapter as startChapter, r.endChapter as endChapter, s.categoryId as categoryId, c.colorHex as colorHex
        FROM sermon_bible_refs r
        JOIN sermons s ON s.id = r.sermonId
        LEFT JOIN sermon_categories c ON c.id = s.categoryId
        WHERE r.startBookId = :bookId
        """
	)
	suspend fun getChapterMarkersForBook(bookId: Int): List<ChapterMarker>

	@Query(
		"""
        SELECT s.id as id, s.sermonDate as sermonDate, s.categoryId as categoryId, c.colorHex as colorHex
        FROM sermons s
        LEFT JOIN sermon_categories c ON s.categoryId = c.id
        WHERE s.sermonDate >= :startMillis AND s.sermonDate < :endMillis
        ORDER BY s.sermonDate, s.createdAt
        """
	)
	suspend fun getSermonMarkersInRange(startMillis: Long, endMillis: Long): List<SermonMarker>
}

data class SermonMarker(
	val id: Long,
	val sermonDate: Long,
	val categoryId: Long?,
	val colorHex: String?
)

data class ChapterMarker(
	val id: Long,
	val startChapter: Int,
	val endChapter: Int,
	val categoryId: Long?,
	val colorHex: String?
)