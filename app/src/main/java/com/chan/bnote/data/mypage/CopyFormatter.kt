package com.chan.bnote.data.mypage

import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.SecondaryVerseText

object CopyFormatter {

	private fun BibleVerse.fullText(): String =
		if (!text2.isNullOrBlank()) "$text $text2" else text

	fun format(
		bookId: Int,
		chapter: Int,
		verses: List<BibleVerse>,
		selectedVerseNumbers: Set<Int>,
		secondaryMap: Map<Int, SecondaryVerseText>?,
		includeSecondary: Boolean,
		referenceStyle: String // "NONE" | "SHORT" | "LONG"
	): String {
		val sortedVerses = verses.filter { it.verse in selectedVerseNumbers }.sortedBy { it.verse }
		val sb = StringBuilder()

		if (referenceStyle == "NONE") {
			// 기존 형식: 책명 장 헤더 + "절  본문"
			val bookName = BibleBooks.nameOf(bookId)
			sb.append("$bookName ${chapter}${BibleBooks.chapterUnit(bookId)}\n")

			for (v in sortedVerses) {
				sb.append("${v.verse}  ${v.fullText()}\n")
				if (includeSecondary) {
					secondaryMap?.get(v.verse)?.let { sb.append("   ${it.fullText}\n") }
				}
			}
		} else {
			// SHORT/LONG: 헤더 없이, 절마다 참조를 줄 앞에 붙임
			for (v in sortedVerses) {
				val reference = when (referenceStyle) {
					"SHORT" -> "(${BibleBooks.shortNameOf(bookId)} $chapter:${v.verse})"
					else -> "(${BibleBooks.nameOf(bookId)} ${chapter}${BibleBooks.chapterUnit(bookId)} ${v.verse}절)"
				}
				sb.append("$reference ${v.fullText()}\n")
				if (includeSecondary) {
					secondaryMap?.get(v.verse)?.let { sb.append("   ${it.fullText}\n") }
				}
			}
		}

		return sb.toString().trimEnd()
	}
}