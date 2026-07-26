package com.chan.bnote.data.application

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chan.bnote.data.bible.BibleBooks

@Entity(tableName = "application_bible_refs")
data class ApplicationBibleRef(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val applicationId: Long,
	val startBookId: Int,
	val startChapter: Int,
	val startVerse: Int,
	val endBookId: Int,
	val endChapter: Int,
	val endVerse: Int,
	// "장만 선택"으로 고른 경우 true. 이때는 절 정보 없이 몇 장부터 몇 장까지만 의미가 있다
	// (startVerse/endVerse는 그냥 1/마지막 절로 채워두되 화면 표시에서는 안 쓴다).
	val isChapterOnly: Boolean = false
) {
	fun toDisplayLabel(): String {
		val bookName = BibleBooks.nameOf(startBookId)
		val unit = BibleBooks.chapterUnit(startBookId)
		if (isChapterOnly) {
			return if (startChapter == endChapter) {
				"$bookName ${startChapter}${unit}"
			} else {
				"$bookName ${startChapter}${unit}~${endChapter}${unit}"
			}
		}
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
}