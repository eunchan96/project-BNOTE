package com.chan.bnote.data.memo

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
	val text: String,
	// "다른 구절에도 추가"로 복사돼 생긴 메모면 원본 위치("창 1:1" 형태)를 담는다. 직접 쓴 메모는 null.
	val sourceLabel: String? = null,
	val updatedAt: Long = System.currentTimeMillis()
)