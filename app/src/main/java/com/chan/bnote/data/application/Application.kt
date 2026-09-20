package com.chan.bnote.data.application

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "applications")
data class Application(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val categoryId: Long?,
	val applicationDate: Long,
	val meditationMemo: String = "", // 묵상하기
	val prayerMemo: String = "",     // 기도하기
	val obedienceMemo: String = "",  // 순종하기
	val createdAt: Long = System.currentTimeMillis()
)