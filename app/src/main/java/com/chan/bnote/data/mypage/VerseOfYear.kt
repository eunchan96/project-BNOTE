package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verse_of_year")
data class VerseOfYear(
	@PrimaryKey
	val year: Int, // 연도 자체를 PK로 사용, 연도당 1개만 존재
	val bookId: Int,
	val chapter: Int,
	val verse: Int,
	val verseText: String, // 조회 편의를 위해 텍스트도 같이 저장 (번역본 바뀌어도 그 해 기록은 유지되도록)
	val note: String = ""
)