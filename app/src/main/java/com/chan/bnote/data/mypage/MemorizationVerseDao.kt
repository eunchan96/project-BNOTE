package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MemorizationVerseDao {

	@Insert
	suspend fun insert(item: MemorizationVerse): Long

	@Update
	suspend fun update(item: MemorizationVerse)

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

	// --- 그룹 ---

	@Query("SELECT COUNT(*) FROM memorization_groups")
	suspend fun countGroups(): Int

	@Insert
	suspend fun insertGroup(group: MemorizationGroup): Long

	@Update
	suspend fun updateGroup(group: MemorizationGroup)

	@Delete
	suspend fun deleteGroup(group: MemorizationGroup)

	@Query("SELECT * FROM memorization_groups ORDER BY sortOrder")
	suspend fun getAllGroups(): List<MemorizationGroup>

	@Query("SELECT * FROM memorization_verses WHERE groupId = :groupId ORDER BY createdAt DESC")
	suspend fun getByGroup(groupId: Long): List<MemorizationVerse>

	// 그룹 삭제 시 그 그룹의 암송 구절도 같이 정리
	@Query("DELETE FROM memorization_verses WHERE groupId = :groupId")
	suspend fun deleteByGroup(groupId: Long)
}