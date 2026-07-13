package com.chan.bnote.data.sermon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermons")
data class Sermon(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val preacherId: Long?,
	val sermonDate: Long,
	val categoryId: Long?,
	val memo: String = "",
	val createdAt: Long = System.currentTimeMillis()
)