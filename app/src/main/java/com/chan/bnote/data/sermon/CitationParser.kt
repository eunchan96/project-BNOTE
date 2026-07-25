package com.chan.bnote.data.sermon

import com.chan.bnote.data.bible.BibleBooks

data class VerseSegment(val start: Int, val end: Int)
data class CitationGroup(val chapter: Int, val segments: List<VerseSegment>)

data class CitationMatch(
	val range: IntRange,
	val bookId: Int,
	val groups: List<CitationGroup>,
	val rawLabel: String
)

object CitationParser {

	private val bookAltPattern: String by lazy {
		(1..66).map { BibleBooks.shortNameOf(it) }
			.sortedByDescending { it.length }
			.joinToString("|") { Regex.escape(it) }
	}

	// 예: "1:1~3, 5, 2:1" 형태를 통째로 캡처
	private val bodyPattern: String by lazy {
		"\\d{1,3}:\\d{1,3}(?:~\\d{1,3})?(?:,\\s?(?:\\d{1,3}:)?\\d{1,3}(?:~\\d{1,3})?)*"
	}

	private val citationRegex: Regex by lazy {
		Regex("\\(?($bookAltPattern)\\s?($bodyPattern)\\)?")
	}

	fun findCitations(text: String): List<CitationMatch> {
		val results = mutableListOf<CitationMatch>()
		for (match in citationRegex.findAll(text)) {
			val bookAbbr = match.groupValues[1]
			val bookId = BibleBooks.shortNameToId(bookAbbr) ?: continue
			val body = match.groupValues[2]

			val groups = parseBody(body) ?: continue
			if (groups.isEmpty()) continue

			results.add(CitationMatch(match.range, bookId, groups, match.value))
		}
		return results
	}

	// 예: "1절", "23절", "1~5절", "1,3절", "1~2,5절" — 책/장 표기가 없는 절 번호(단독/범위/콤마로 여러 개)만.
	// 본문(SermonBibleRef)이 하나뿐인 설교 메모에서, 그 본문의 책/장을 문맥으로 삼아 절 번호만으로도
	// 인용을 찾아줄 때 쓴다.
	private val verseOnlyRegex: Regex by lazy {
		Regex("((?:\\d{1,3}(?:~\\d{1,3})?)(?:,\\s?\\d{1,3}(?:~\\d{1,3})?)*)절")
	}

	fun findVerseOnlyCitations(text: String, bookId: Int, chapter: Int): List<CitationMatch> {
		val results = mutableListOf<CitationMatch>()
		for (match in verseOnlyRegex.findAll(text)) {
			val body = match.groupValues[1]
			val segments = body.split(",").mapNotNull { parseVerseToken(it.trim()) }
			if (segments.isEmpty()) continue
			results.add(
				CitationMatch(
					range = match.range,
					bookId = bookId,
					groups = listOf(CitationGroup(chapter, segments)),
					rawLabel = match.value
				)
			)
		}
		return results
	}

	// 예: "(2:1)", "(2:1~5)" — 괄호 안에 책 표기 없이 장:절만. 본문(SermonBibleRef)이 하나뿐인 설교
	// 메모에서, 그 본문의 책을 문맥으로 삼아 다른 장/절을 가리킬 때 쓴다.
	private val chapterVerseRegex: Regex by lazy { Regex("\\(($bodyPattern)\\)") }

	fun findChapterVerseCitations(text: String, bookId: Int): List<CitationMatch> {
		val results = mutableListOf<CitationMatch>()
		for (match in chapterVerseRegex.findAll(text)) {
			val body = match.groupValues[1]
			val groups = parseBody(body) ?: continue
			if (groups.isEmpty()) continue
			results.add(CitationMatch(match.range, bookId, groups, match.value))
		}
		return results
	}

	private fun parseBody(body: String): List<CitationGroup>? {
		val tokens = body.split(",").map { it.trim() }
		val groups = mutableListOf<CitationGroup>()
		var currentChapter: Int? = null
		var currentSegments = mutableListOf<VerseSegment>()

		for (token in tokens) {
			val colonIndex = token.indexOf(':')
			if (colonIndex >= 0) {
				if (currentChapter != null && currentSegments.isNotEmpty()) {
					groups.add(CitationGroup(currentChapter, currentSegments))
				}
				val chapter = token.substring(0, colonIndex).toIntOrNull() ?: return null
				currentChapter = chapter
				currentSegments = mutableListOf()
				parseVerseToken(token.substring(colonIndex + 1))?.let { currentSegments.add(it) }
			} else {
				if (currentChapter == null) return null
				parseVerseToken(token)?.let { currentSegments.add(it) }
			}
		}
		if (currentChapter != null && currentSegments.isNotEmpty()) {
			groups.add(CitationGroup(currentChapter, currentSegments))
		}
		return groups
	}

	private fun parseVerseToken(token: String): VerseSegment? {
		val parts = token.split("~")
		val start = parts.getOrNull(0)?.toIntOrNull() ?: return null
		val end = parts.getOrNull(1)?.toIntOrNull() ?: start
		return VerseSegment(start, end)
	}
}