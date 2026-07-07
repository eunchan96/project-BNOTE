package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_verses")
data class BibleVerse(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,      // 1~66
	val chapter: Int,
	val verse: Int,
	val text: String,
	val title: String? = null
)