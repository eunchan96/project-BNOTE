package com.chan.bnote.data.bible

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "bible_verses",
	indices = [Index(value = ["translation", "bookId", "chapter", "verse"])]
)
data class BibleVerse(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val translation: String,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val text: String,
	val title: String? = null
)