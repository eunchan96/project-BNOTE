package com.chan.bnote.data.bible

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BibleDao {

	@Insert
	suspend fun insertAll(verses: List<BibleVerse>)

	@Query("SELECT COUNT(*) FROM bible_verses WHERE translation = :translation")
	suspend fun countForTranslation(translation: String): Int

	// 성경 본문 데이터를 다시 심을 때(재시딩) 그 번역본만 싹 지우고 새로 넣기 위한 것
	@Query("DELETE FROM bible_verses WHERE translation = :translation")
	suspend fun deleteTranslation(translation: String)

	@Query("SELECT DISTINCT bookId FROM bible_verses WHERE translation = :translation ORDER BY bookId")
	suspend fun getBookIds(translation: String): List<Int>

	@Query("SELECT DISTINCT chapter FROM bible_verses WHERE translation = :translation AND bookId = :bookId ORDER BY chapter")
	suspend fun getChapters(translation: String, bookId: Int): List<Int>

	@Query("SELECT * FROM bible_verses WHERE translation = :translation AND bookId = :bookId AND chapter = :chapter ORDER BY verse")
	suspend fun getVerses(translation: String, bookId: Int, chapter: Int): List<BibleVerse>

	@Query("SELECT MAX(chapter) FROM bible_verses WHERE translation = :translation AND bookId = :bookId")
	suspend fun getMaxChapter(translation: String, bookId: Int): Int

	@Query(
		"""
    SELECT * FROM bible_verses 
    WHERE translation = :translation 
    AND REPLACE(text, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%' 
    ORDER BY bookId, chapter, verse 
    LIMIT 200
    """
	)
	suspend fun searchVerses(translation: String, keyword: String): List<BibleVerse>

	// 단어 메모 "다른 구절에도 추가"용 — 공백을 지우지 않고 그대로 부분 문자열을 찾는다
	// (본문에서 정확한 시작/끝 위치를 다시 계산해야 하기 때문에 공백 제거 검색은 쓸 수 없음).
	@Query(
		"""
    SELECT * FROM bible_verses
    WHERE translation = :translation AND text LIKE '%' || :keyword || '%'
    ORDER BY bookId, chapter, verse
    """
	)
	suspend fun findVersesContainingExact(translation: String, keyword: String): List<BibleVerse>

	@Query("SELECT * FROM bible_verses WHERE translation = :translation ORDER BY RANDOM() LIMIT 1")
	suspend fun getRandomVerse(translation: String): BibleVerse?
}