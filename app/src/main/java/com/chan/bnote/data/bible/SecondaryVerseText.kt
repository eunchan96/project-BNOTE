package com.chan.bnote.data.bible

/**
 * 함께보기(대역본) 절 텍스트. 주성경이 절 중간 소제목으로 쪼개진 극소수 예외 구절(예: 창 35:22)에서는
 * 함께보기도 같은 지점에서 문단이 나뉘어야 자연스러워서(소제목 자체는 안 보여줘도) text2를 같이 들고 다닌다.
 */
data class SecondaryVerseText(val text: String, val text2: String? = null) {
	/** 복사하기 등, 나뉜 걸 신경 안 써도 되는 곳에서 쓰는 전체 텍스트. */
	val fullText: String
		get() = if (!text2.isNullOrBlank()) "$text $text2" else text
}