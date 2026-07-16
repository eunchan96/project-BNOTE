package com.chan.bnote.data.bookmark

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "bible_bookmarks",
	indices = [Index(value = ["bookId", "chapter", "verse"], unique = true)]
)
data class BibleBookmark(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val isBookmarked: Boolean = false,
	val isHighlighted: Boolean = false,
	val updatedAt: Long = System.currentTimeMillis()
)