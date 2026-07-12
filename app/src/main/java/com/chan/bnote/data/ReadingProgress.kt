package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "reading_progress",
	indices = [Index(value = ["bookId", "chapter"], unique = true)]
)
data class ReadingProgress(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,
	val chapter: Int,
	val readAt: Long = System.currentTimeMillis()
)