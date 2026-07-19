package com.chan.bnote.data.sermon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermon_photos")
data class SermonPhoto(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val sermonId: Long,
	val filePath: String, // 앱 내부 저장소(files/sermon_photos/) 절대경로
	val sortOrder: Int = 0
)