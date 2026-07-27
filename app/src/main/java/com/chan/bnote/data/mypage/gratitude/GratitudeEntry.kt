package com.chan.bnote.data.mypage.gratitude

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 감사노트 안의 "✓ ____" 한 줄 한 줄. */
@Entity(tableName = "gratitude_entries")
data class GratitudeEntry(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val noteId: Long,
	val text: String,
	val sortOrder: Int = 0
)