package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_requests")
data class PrayerRequest(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val content: String,
	val createdAt: Long = System.currentTimeMillis(),
	val isAnswered: Boolean = false,
	val answeredAt: Long? = null
)