package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemorizationVerseDao {

	@Insert
	suspend fun insert(item: MemorizationVerse): Long

	@Delete
	suspend fun delete(item: MemorizationVerse)

	@Query("SELECT * FROM memorization_verses WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): MemorizationVerse?

	@Query("SELECT * FROM memorization_verses ORDER BY createdAt DESC")
	suspend fun getAll(): List<MemorizationVerse>

	@Query("SELECT COUNT(*) FROM memorization_verses")
	suspend fun count(): Int

	// 같은 구절 범위가 이미 등록되어 있는지 확인 (약속의 말씀에서 자동 추가할 때 중복 방지용)
	@Query(
		"""
		SELECT COUNT(*) FROM memorization_verses
		WHERE startBookId = :startBookId AND startChapter = :startChapter AND startVerse = :startVerse
		  AND endBookId = :endBookId AND endChapter = :endChapter AND endVerse = :endVerse
		"""
	)
	suspend fun existsCount(
		startBookId: Int, startChapter: Int, startVerse: Int,
		endBookId: Int, endChapter: Int, endVerse: Int
	): Int
}