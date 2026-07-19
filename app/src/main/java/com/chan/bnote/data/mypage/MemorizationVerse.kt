package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chan.bnote.data.bible.BibleBooks

/** 암송 구절 리스트 항목. 사용자가 직접 추가하거나, 올해 약속의 말씀에 등록할 때 함께 추가된다. */
@Entity(tableName = "memorization_verses")
data class MemorizationVerse(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val groupId: Long,
	val startBookId: Int,
	val startChapter: Int,
	val startVerse: Int,
	val endBookId: Int, // 대부분 startBookId와 동일
	val endChapter: Int,
	val endVerse: Int,
	val verseText: String,
	val note: String = "",
	val createdAt: Long = System.currentTimeMillis()
) {
	fun toDisplayLabel(): String {
		val bookName = BibleBooks.nameOf(startBookId)
		val unit = BibleBooks.chapterUnit(startBookId)
		return if (startChapter == endChapter) {
			if (startVerse == endVerse) {
				"$bookName ${startChapter}${unit} ${startVerse}절"
			} else {
				"$bookName ${startChapter}${unit} ${startVerse}~${endVerse}절"
			}
		} else {
			"$bookName ${startChapter}${unit} ${startVerse}절~${endChapter}${unit} ${endVerse}절"
		}
	}

	fun toShortLabel(): String {
		val bookAbbr = BibleBooks.shortNameOf(startBookId)
		return if (startBookId == endBookId && startChapter == endChapter) {
			if (startVerse == endVerse) {
				"$bookAbbr $startChapter:$startVerse"
			} else {
				"$bookAbbr $startChapter:$startVerse~$endVerse"
			}
		} else {
			"$bookAbbr $startChapter:$startVerse~${endChapter}:$endVerse"
		}
	}
}