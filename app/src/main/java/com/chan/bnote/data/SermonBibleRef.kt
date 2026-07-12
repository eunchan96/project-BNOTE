package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermon_bible_refs")
data class SermonBibleRef(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val sermonId: Long,
	val startBookId: Int,
	val startChapter: Int,
	val startVerse: Int,
	val endBookId: Int,     // 대부분 startBookId와 동일, 책이 바뀌는 경우는 거의 없지만 확장 가능하게 둠
	val endChapter: Int,
	val endVerse: Int
) {
	// 화면 표시용 짧은 텍스트, ex) "창 1:1~10" 또는 "창 1:1~2:1"
	fun toShortLabel(): String {
		val bookAbbr = BibleBooks.nameOf(startBookId).take(1) // 임시 축약, 필요시 별도 약칭 테이블로 교체 가능
		return if (startBookId == endBookId && startChapter == endChapter) {
			"$bookAbbr$startChapter:$startVerse~$endVerse"
		} else {
			"$bookAbbr$startChapter:$startVerse~${endChapter}:$endVerse"
		}
	}
}