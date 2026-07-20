package com.chan.bnote.data.sermon.preacher

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PreacherDao {

	@Insert
	suspend fun insert(preacher: Preacher): Long

	@Update
	suspend fun update(preacher: Preacher)

	@Delete
	suspend fun delete(preacher: Preacher)

	@Query("SELECT * FROM preachers ORDER BY sortOrder")
	suspend fun getAll(): List<Preacher>

	@Query("DELETE FROM preachers")
	suspend fun deleteAll()

	@Query("SELECT * FROM preachers WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): Preacher?

	// 설교자 삭제 전에 호출: 그 설교자를 쓰던 설교들을 '미지정' 상태로 정리
	@Query("UPDATE sermons SET preacherId = NULL WHERE preacherId = :id")
	suspend fun clearPreacherFromSermons(id: Long)
}