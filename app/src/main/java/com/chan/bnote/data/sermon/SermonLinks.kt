package com.chan.bnote.data.sermon

/**
 * 설교 링크 여러 개를 Sermon.link 한 칸에 담는 규칙. 줄바꿈으로 이어 붙여 저장한다.
 * 예전처럼 링크가 하나뿐이면 저장 모양이 그대로라, DB 구조 · 데이터 내보내기/불러오기를 바꾸지 않아도 된다.
 */
object SermonLinks {

	/** 저장된 값을 링크 목록으로 나눈다(빈 줄은 뺀다). */
	fun split(raw: String?): List<String> =
		raw.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }

	/** 링크 목록을 저장할 값으로 합친다. 없으면 null. */
	fun join(links: List<String>): String? =
		links.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").ifEmpty { null }

	/** 사용자가 붙여넣은 주소를 다듬는다. "www.youtube.com/..."처럼 앞부분이 빠졌으면 https://를 붙인다. */
	fun normalize(input: String): String {
		val text = input.trim()
		if (text.isEmpty()) return text
		return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) text else "https://$text"
	}
}