package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermons")
data class Sermon(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val preacher: String,
	val sermonDate: Long,        // 설교 날짜 (millis, 그날 00:00 기준)
	val bookId: Int? = null,     // 관련 성경책 (선택)
	val chapter: Int? = null,    // 관련 장 (선택)
	val memo: String = "",
	val createdAt: Long = System.currentTimeMillis()
)