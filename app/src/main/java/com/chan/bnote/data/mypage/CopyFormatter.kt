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
		config: CopyFormatConfig
	): String {
		val sortedVerses = verses.filter { it.verse in selectedVerseNumbers }.sortedBy { it.verse }
		if (sortedVerses.isEmpty()) return ""

		val isMulti = sortedVerses.size > 1
		val firstVerse = sortedVerses.first().verse
		val lastVerse = sortedVerses.last().verse

		fun verseLine(v: BibleVerse, showNumber: Boolean): String {
			var body = v.fullText()
			if (includeSecondary) {
				secondaryMap?.get(v.verse)?.let { sec -> body = "$body\n   ${sec.fullText}" }
			}
			if (config.quoteVerse) body = "\"$body\""
			return if (showNumber) {
				"${
					formatVerseNumber(
						v.verse,
						config.verseNumberStyle
					)
				}${" ".repeat(config.verseNumberSpacing)}$body"
			} else {
				body
			}
		}

		val multiSep = when (config.multiVerseSeparator) {
			CopyFormatConfig.Separator.NEWLINE -> "\n"
			CopyFormatConfig.Separator.SPACE -> " "
		}

		return when (config.refVerseSeparator) {
			CopyFormatConfig.Separator.SPACE -> {
				val reference =
					buildInlineReference(bookId, chapter, firstVerse, lastVerse, isMulti, config)
				val showNumber = isMulti && config.showVerseNumberWhenMulti
				val body = sortedVerses.joinToString(multiSep) { verseLine(it, showNumber) }
				when (config.refPosition) {
					CopyFormatConfig.RefPosition.BEFORE -> "$reference $body"
					CopyFormatConfig.RefPosition.AFTER -> "$body $reference"
				}
			}

			CopyFormatConfig.Separator.NEWLINE -> {
				val header = buildHeaderReference(bookId, chapter, config)
				// 줄바꿈 형식에선 절 번호가 없으면 어느 절인지 알 수 없으니 항상 표시한다.
				val body = sortedVerses.joinToString(multiSep) { verseLine(it, true) }
				"$header\n$body"
			}
		}
	}

	/** 짧게(창 1:1) / 중간(창세기 1:1) / 길게(창세기 1장 1절) 형태의 인용 참조를 만든다. */
	private fun buildInlineReference(
		bookId: Int,
		chapter: Int,
		firstVerse: Int,
		lastVerse: Int,
		isMulti: Boolean,
		config: CopyFormatConfig
	): String {
		val bookName = if (config.refLength == CopyFormatConfig.RefLength.LONG) {
			BibleBooks.nameOf(bookId)
		} else {
			BibleBooks.shortNameOf(bookId)
		}
		val sp = if (config.refSpacing) " " else ""

		val core = if (config.refLength == CopyFormatConfig.RefLength.LONG) {
			val unit = BibleBooks.chapterUnit(bookId)
			val versePart = if (isMulti) "$firstVerse~${lastVerse}절" else "${firstVerse}절"
			"$bookName$sp$chapter$unit$sp$versePart"
		} else {
			val versePart = if (isMulti) "$firstVerse~$lastVerse" else "$firstVerse"
			"$bookName$sp$chapter:$versePart"
		}

		return wrapWithBracket(core, config.refBracket)
	}

	/** 줄바꿈 형식일 때 맨 위에 오는 "창세기 1장" 같은 헤더. 절 정보는 각 줄에서 따로 보여주므로 안 붙인다. */
	private fun buildHeaderReference(bookId: Int, chapter: Int, config: CopyFormatConfig): String {
		val bookName = if (config.refLength == CopyFormatConfig.RefLength.LONG) {
			BibleBooks.nameOf(bookId)
		} else {
			BibleBooks.shortNameOf(bookId)
		}
		val sp = if (config.refSpacing) " " else ""
		val unit = BibleBooks.chapterUnit(bookId)
		return "$bookName$sp$chapter$unit"
	}

	private fun wrapWithBracket(text: String, bracket: CopyFormatConfig.RefBracket): String =
		when (bracket) {
			CopyFormatConfig.RefBracket.NONE -> text
			CopyFormatConfig.RefBracket.PAREN -> "($text)"
			CopyFormatConfig.RefBracket.SQUARE -> "[$text]"
		}

	private fun formatVerseNumber(verse: Int, style: CopyFormatConfig.VerseNumberStyle): String =
		when (style) {
			CopyFormatConfig.VerseNumberStyle.PLAIN -> "$verse"
			CopyFormatConfig.VerseNumberStyle.DOT -> "$verse."
			CopyFormatConfig.VerseNumberStyle.BRACKET -> "[$verse]"
		}
}