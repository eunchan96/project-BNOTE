package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermons")
data class Sermon(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val preacher: String,
	val sermonDate: Long,
	val categoryId: Long?,   // SermonCategory 참조 (nullable: 카테고리 미지정 허용)
	val memo: String = "",
	val createdAt: Long = System.currentTimeMillis()
)