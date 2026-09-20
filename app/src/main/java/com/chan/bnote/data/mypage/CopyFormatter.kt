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
		val verseNumbers = sortedVerses.map { it.verse }

		fun verseLine(v: BibleVerse, showNumber: Boolean): String {
			var body = v.fullText()
			if (includeSecondary) {
				secondaryMap?.get(v.verse)?.let { sec -> body = "$body\n   ${sec.fullText}" }
			}
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
					buildInlineReference(bookId, chapter, verseNumbers, config)
				val showNumber = isMulti && config.showVerseNumberWhenMulti
				var body = sortedVerses.joinToString(multiSep) { verseLine(it, showNumber) }
				// 따옴표는 절 하나하나가 아니라, 선택한 구절 전체를 한 번만 감싼다.
				if (config.quoteVerse) body = "\"$body\""
				when (config.refPosition) {
					CopyFormatConfig.RefPosition.BEFORE -> "$reference $body"
					CopyFormatConfig.RefPosition.AFTER -> "$body $reference"
				}
			}

			CopyFormatConfig.Separator.NEWLINE -> {
				val header = buildHeaderReference(bookId, chapter, config)
				// 줄바꿈 형식에선 절 번호가 없으면 어느 절인지 알 수 없으니 항상 표시한다.
				var body = sortedVerses.joinToString(multiSep) { verseLine(it, true) }
				if (config.quoteVerse) body = "\"$body\""
				"$header\n$body"
			}
		}
	}

	/** 짧게(창 1:1) / 중간(창세기 1:1) / 길게(창세기 1장 1절) 형태의 인용 참조를 만든다.
	 * 절 번호가 연속이 아니면(예: 1절과 5절만 선택) "1~5절"처럼 뭉뚱그리지 않고
	 * 연속 구간별로 나눠서 "1,5절"처럼, 연속 구간이 섞여 있으면 "1~2,5절"처럼 보여준다. */
	private fun buildInlineReference(
		bookId: Int,
		chapter: Int,
		verseNumbers: List<Int>,
		config: CopyFormatConfig
	): String {
		val bookName = bookNameFor(bookId, config)
		val sp = if (config.refSpacing) " " else ""
		val rangeText = formatVerseRanges(verseNumbers)

		val core = if (config.refLength == CopyFormatConfig.RefLength.LONG) {
			val unit = BibleBooks.chapterUnit(bookId)
			"$bookName$sp$chapter$unit$sp${rangeText}절"
		} else {
			// 짧게/중간 둘 다 "장:절" 표기를 쓰고, 책이름 길이만 다르다.
			"$bookName$sp$chapter:$rangeText"
		}

		return wrapWithBracket(core, config.refBracket)
	}

	/** 정렬된 절 번호 목록을 연속 구간별로 묶어서 "1", "1~5", "1,5", "1~2,5" 같은 문자열로 만든다. */
	private fun formatVerseRanges(sortedVerseNumbers: List<Int>): String {
		if (sortedVerseNumbers.isEmpty()) return ""

		val ranges = mutableListOf<Pair<Int, Int>>()
		var rangeStart = sortedVerseNumbers.first()
		var rangeEnd = rangeStart
		for (verse in sortedVerseNumbers.drop(1)) {
			if (verse == rangeEnd + 1) {
				rangeEnd = verse
			} else {
				ranges.add(rangeStart to rangeEnd)
				rangeStart = verse
				rangeEnd = verse
			}
		}
		ranges.add(rangeStart to rangeEnd)

		return ranges.joinToString(",") { (start, end) ->
			if (start == end) "$start" else "$start~$end"
		}
	}

	/** 줄바꿈 형식일 때 맨 위에 오는 "(창세기 1장)" 같은 헤더. 절 정보는 각 줄에서 따로 보여주므로 안 붙인다. */
	private fun buildHeaderReference(bookId: Int, chapter: Int, config: CopyFormatConfig): String {
		val bookName = bookNameFor(bookId, config)
		val sp = if (config.refSpacing) " " else ""
		val unit = BibleBooks.chapterUnit(bookId)
		val core = "$bookName$sp$chapter$unit"
		return wrapWithBracket(core, config.refBracket)
	}

	private fun bookNameFor(bookId: Int, config: CopyFormatConfig): String =
		if (config.refLength == CopyFormatConfig.RefLength.SHORT) {
			BibleBooks.shortNameOf(bookId)
		} else {
			BibleBooks.nameOf(bookId)
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