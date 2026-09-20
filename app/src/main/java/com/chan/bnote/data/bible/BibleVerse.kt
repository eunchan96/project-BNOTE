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
	val title: String? = null,
	// 아주 드물게 절 중간에 소제목이 끼어드는 경우(예: 창 35:22)에만 쓴다.
	// title2가 있으면 text 다음에 소제목처럼 표시되고, 이어서 text2가 나온다. 같은 절 번호를 공유한다.
	val title2: String? = null,
	val text2: String? = null
)