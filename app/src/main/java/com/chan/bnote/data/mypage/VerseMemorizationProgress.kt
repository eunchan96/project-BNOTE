package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** VerseOfYearRef 하나에 대한 암송 연습 진행 상태. */
@Entity(
	tableName = "verse_memorization_progress",
	indices = [Index(value = ["verseRefId"], unique = true)]
)
data class VerseMemorizationProgress(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val verseRefId: Long,
	val reviewCount: Int = 0,
	val lastReviewedAt: Long? = null,
	val isMastered: Boolean = false
)