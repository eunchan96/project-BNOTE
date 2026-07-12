package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_memos")
data class WordMemo(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val translation: String,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val startOffset: Int,
	val endOffset: Int,
	val text: String,
	val updatedAt: Long = System.currentTimeMillis()
)