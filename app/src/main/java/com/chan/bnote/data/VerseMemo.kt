package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "verse_memos",
	indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class VerseMemo(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val text: String,
	val updatedAt: Long = System.currentTimeMillis()
)