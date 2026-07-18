package com.chan.bnote.data.prayer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PrayerRequestDao {

	@Insert
	suspend fun insert(item: PrayerRequest): Long

	@Update
	suspend fun update(item: PrayerRequest)

	@Delete
	suspend fun delete(item: PrayerRequest)

	// 미응답 항목을 먼저, 그 안에서는 최근 등록 순으로 보여준다.
	@Query("SELECT * FROM prayer_requests ORDER BY isAnswered ASC, createdAt DESC")
	suspend fun getAll(): List<PrayerRequest>
}