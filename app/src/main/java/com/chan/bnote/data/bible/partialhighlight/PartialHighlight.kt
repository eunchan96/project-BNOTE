package com.chan.bnote.data.bible.partialhighlight

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
	// 절이 중간에 소제목으로 쪼개진 경우(BibleVerse.text2), 이 하이라이트가 text(0)와 text2(1) 중
	// 어느 쪽을 가리키는지. 대부분의 절은 안 쪼개져 있으니 기본값 0(=text)이면 된다.
	val segment: Int = 0,
	val colorHex: String = "#FFF9C4"
)