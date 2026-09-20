package com.chan.bnote.data.bible.scrap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scraps")
data class Scrap(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val groupId: Long,
	val bookId: Int,
	val chapter: Int,
	val startVerse: Int,
	val endVerse: Int,
	val verseText: String, // 스크랩 당시 본문 저장 (번역본 바뀌어도 유지)
	val createdAt: Long = System.currentTimeMillis()
)