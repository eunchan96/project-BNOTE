package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partial_highlights")
data class PartialHighlight(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val translation: String,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val startOffset: Int,
	val endOffset: Int, // exclusive
	val colorHex: String = "#FFF9C4"
)