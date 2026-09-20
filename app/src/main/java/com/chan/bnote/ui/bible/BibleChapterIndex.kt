package com.chan.bnote.ui.bible

import com.chan.bnote.data.BibleDatabase

/**
 * ViewPager2에서 "몇 번째 페이지"를 실제 (책, 장)으로 바꾸거나 그 반대로 바꾸기 위한 표.
 * 창세기 1장 = 0번, 창세기 2장 = 1번, ... 요한계시록 마지막 장 = 마지막 번, 이런 식으로 성경
 * 전체 장을 하나로 쭉 이어서 순서를 매긴다. 앱이 켜져 있는 동안 한 번만 계산해서 캐싱해둔다
 * (번역본이 바뀌어도 장 수 자체는 같으니 다시 계산할 필요 없다).
 */
object BibleChapterIndex {

	private var chapterCounts: IntArray? = null
	private var cumulativeOffsets: IntArray? = null

	suspend fun ensureLoaded(db: BibleDatabase, translation: String) {
		if (chapterCounts != null) return

		val counts = IntArray(66)
		for (bookId in 1..66) {
			counts[bookId - 1] = db.bibleDao().getMaxChapter(translation, bookId).coerceAtLeast(1)
		}

		val offsets = IntArray(66)
		var sum = 0
		for (i in 0 until 66) {
			offsets[i] = sum
			sum += counts[i]
		}

		chapterCounts = counts
		cumulativeOffsets = offsets
	}

	val isReady: Boolean get() = chapterCounts != null

	fun totalPages(): Int = chapterCounts?.sum() ?: 0

	fun positionOf(bookId: Int, chapter: Int): Int {
		val offsets = cumulativeOffsets ?: return 0
		if (bookId !in 1..66) return 0
		return offsets[bookId - 1] + (chapter - 1)
	}

	/** position -> (bookId, chapter). 범위를 벗어나면 null. */
	fun chapterAt(position: Int): Pair<Int, Int>? {
		val counts = chapterCounts ?: return null
		if (position < 0) return null
		var remaining = position
		for (bookId in 1..66) {
			val count = counts[bookId - 1]
			if (remaining < count) return bookId to (remaining + 1)
			remaining -= count
		}
		return null
	}
}