package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chan.bnote.data.bible.BibleBooks

@Entity(tableName = "verse_of_year_refs")
data class VerseOfYearRef(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val year: Int,
	val startBookId: Int,
	val startChapter: Int,
	val startVerse: Int,
	val endBookId: Int, // 대부분 startBookId와 동일
	val endChapter: Int,
	val endVerse: Int,
	val verseText: String // 조회 편의를 위해 텍스트도 같이 저장 (번역본이 바뀌어도 그 해 기록은 유지되도록)
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