package com.chan.bnote.data.notice

import android.content.Context
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.Translation

/**
 * 성경 본문 오류 공지를 성경 탭에서 알려주기 위한 도우미. 지금 보고 있는 장에 오류 안내가 있으면
 * 상단바에 표시를 띄우고, 누르면 그 공지를 연다(BibleFragment).
 *
 * 공지 제목 · 본문에서 구절 위치를 찾는다:
 * - "예레미야 42장 14절", "시편 23편 1절"처럼 책 이름 + 장(편)
 * - "렘 42:14"처럼 약어 + 장:절
 * - "예레미야 42장 14절 · 43장 13절"처럼 책 이름 없이 이어진 장은 바로 앞의 책으로 본다.
 *
 * 공지에 번역본 이름(예: "개역개정")이 나오면 그 번역본을 보고 있을 때만 표시하고, 없으면 모든 번역본에서 표시한다.
 * "해결됨" 공지는 표시하지 않는다. 단 "대상: 1.14 이하"처럼 버전을 정해 둔 공지는 그 버전 사용자에게만 보이는
 * 공지라(아직 안 고쳐진 버전), 해결됨이어도 표시한다.
 */
object BibleErrataNotices {

	data class ChapterRef(val bookId: Int, val chapter: Int)

	/** 긴 이름부터 맞춰야 "요한"이 "요한일서"를 먼저 먹지 않는다. */
	private val fullRefRegex: Regex by lazy {
		val names = BibleBooks.allNames.sortedByDescending { it.length }
			.joinToString("|") { Regex.escape(it) }
		Regex("(?:($names)\\s*)?(\\d{1,3})\\s*[장편]")
	}

	private val shortRefRegex: Regex by lazy {
		val names = BibleBooks.allShortNames.sortedByDescending { it.length }
			.joinToString("|") { Regex.escape(it) }
		// 앞이 한글이면 낱말 중간이라 약어가 아니다(예: "사랑 3:16"의 "사").
		Regex("(?<![가-힣])($names)\\s*(\\d{1,3})\\s*:\\s*\\d{1,3}")
	}

	/** 지금 보는 장([bookId], [chapter])에 해당하는 본문 오류 공지. 여러 개면 가장 최근 것. */
	fun findFor(
		context: Context,
		bookId: Int,
		chapter: Int,
		primary: Translation,
		secondary: Translation?
	): Notice? {
		val target = ChapterRef(bookId, chapter)
		return NoticeRepository.visibleNotices(context)
			.filter { it.type == NoticeType.BIBLE_TEXT_ERROR }
			.filter { !it.isResolved || it.minVersion != null || it.maxVersion != null }
			.filter { appliesToTranslation(it, primary, secondary) }
			.filter { target in chapterRefs(it.title + "\n" + it.body) }
			.maxByOrNull { it.createdAt }
	}

	private fun appliesToTranslation(
		notice: Notice,
		primary: Translation,
		secondary: Translation?
	): Boolean {
		val text = notice.title + "\n" + notice.body
		val mentioned = Translation.entries.filter { text.contains(shortName(it)) }
		if (mentioned.isEmpty()) return true
		return primary in mentioned || (secondary != null && secondary in mentioned)
	}

	/** "NIV (New International Version)" → "NIV", "개역개정" → "개역개정". */
	private fun shortName(translation: Translation): String =
		translation.displayName.substringBefore(" (")

	fun chapterRefs(text: String): Set<ChapterRef> {
		val refs = mutableSetOf<ChapterRef>()
		var lastBookId: Int? = null
		for (match in fullRefRegex.findAll(text)) {
			val bookId = match.groupValues[1].takeIf { it.isNotEmpty() }
				?.let { BibleBooks.nameToId(it) } ?: lastBookId ?: continue
			lastBookId = bookId
			match.groupValues[2].toIntOrNull()?.let { refs.add(ChapterRef(bookId, it)) }
		}
		for (match in shortRefRegex.findAll(text)) {
			val bookId = BibleBooks.shortNameToId(match.groupValues[1]) ?: continue
			match.groupValues[2].toIntOrNull()?.let { refs.add(ChapterRef(bookId, it)) }
		}
		return refs
	}
}