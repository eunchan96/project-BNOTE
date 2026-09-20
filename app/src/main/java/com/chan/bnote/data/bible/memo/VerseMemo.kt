package com.chan.bnote.data.bible.memo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verse_memos")
data class VerseMemo(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val text: String,
	val updatedAt: Long = System.currentTimeMillis()
)