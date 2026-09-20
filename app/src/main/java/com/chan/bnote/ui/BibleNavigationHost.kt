package com.chan.bnote.ui

interface BibleNavigationHost {
	fun navigateToBibleChapter(bookId: Int, chapter: Int, verse: Int? = null)

	/** 그 구절로 이동한 뒤 구절 메모 편집 시트를 자동으로 띄운다(메모 목록·최근 활동에서 사용). */
	fun navigateToBibleChapterAndOpenVerseMemo(bookId: Int, chapter: Int, verse: Int)

	/** 위와 같은 이유로, 단어 메모 버전. 시작/끝 오프셋까지 알아야 어떤 단어 메모인지 특정할 수 있다. */
	fun navigateToBibleChapterAndOpenWordMemo(
		bookId: Int,
		chapter: Int,
		verse: Int,
		startOffset: Int,
		endOffset: Int,
		segment: Int
	)
}