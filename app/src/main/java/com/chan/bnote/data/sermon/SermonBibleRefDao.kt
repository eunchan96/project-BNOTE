package com.chan.bnote.data.sermon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SermonBibleRefDao {

	@Insert
	suspend fun insertAll(refs: List<SermonBibleRef>)

	@Query("SELECT * FROM sermon_bible_refs WHERE sermonId = :sermonId")
	suspend fun getBySermon(sermonId: Long): List<SermonBibleRef>

	@Query("SELECT * FROM sermon_bible_refs")
	suspend fun getAll(): List<SermonBibleRef>

	@Query("DELETE FROM sermon_bible_refs")
	suspend fun deleteAll()

	@Query("DELETE FROM sermon_bible_refs WHERE sermonId = :sermonId")
	suspend fun deleteBySermon(sermonId: Long)

	// 성경별 탭(C-4)에서 특정 책의 장 범위에 걸치는 설교 찾을 때 사용
	@Query(
		"""
        SELECT * FROM sermon_bible_refs 
        WHERE startBookId = :bookId 
        AND NOT (endChapter < :chapter OR startChapter > :chapter)
        """
	)
	suspend fun getRefsCoveringChapter(bookId: Int, chapter: Int): List<SermonBibleRef>

	@Query(
		"SELECT * FROM sermon_bible_refs WHERE sermonId = :sermonId ORDER BY startBookId, startChapter, startVerse LIMIT 1"
	)
	suspend fun getFirstRef(sermonId: Long): SermonBibleRef?
}