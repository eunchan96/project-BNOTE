package com.chan.bnote.data.scrap

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ScrapDao {

	@Query("SELECT COUNT(*) FROM scrap_groups")
	suspend fun countGroups(): Int

	@Query("SELECT COUNT(*) FROM scraps")
	suspend fun countAllScraps(): Int

	@Insert
	suspend fun insertGroup(group: ScrapGroup): Long

	@Update
	suspend fun updateGroup(group: ScrapGroup)

	@Delete
	suspend fun deleteGroup(group: ScrapGroup)

	@Query("SELECT * FROM scrap_groups ORDER BY sortOrder")
	suspend fun getAllGroups(): List<ScrapGroup>

	@Insert
	suspend fun insertScrap(scrap: Scrap): Long

	@Delete
	suspend fun deleteScrap(scrap: Scrap)

	@Query("SELECT * FROM scraps WHERE groupId = :groupId ORDER BY createdAt DESC")
	suspend fun getScrapsByGroup(groupId: Long): List<Scrap>

	// 그룹 삭제 시 그 그룹의 스크랩도 같이 정리
	@Query("DELETE FROM scraps WHERE groupId = :groupId")
	suspend fun deleteScrapsByGroup(groupId: Long)
}