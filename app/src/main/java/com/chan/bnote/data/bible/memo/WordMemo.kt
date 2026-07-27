package com.chan.bnote.data.bible.memo

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
	// 절이 중간에 소제목으로 쪼개진 경우(BibleVerse.text2), 이 메모가 text(0)와 text2(1) 중
	// 어느 쪽을 가리키는지. 대부분의 절은 안 쪼개져 있으니 기본값 0(=text)이면 된다.
	val segment: Int = 0,
	val text: String,
	// "다른 구절에도 추가"로 복사돼 생긴 메모면 원본 위치("창 1:1" 형태)를 담는다. 직접 쓴 메모는 null.
	val sourceLabel: String? = null,
	val updatedAt: Long = System.currentTimeMillis()
)