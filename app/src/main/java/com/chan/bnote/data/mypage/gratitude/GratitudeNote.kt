package com.chan.bnote.data.mypage.gratitude

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gratitude_notes")
data class GratitudeNote(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val date: Long,
	val createdAt: Long = System.currentTimeMillis()
)