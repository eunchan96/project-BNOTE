package com.chan.bnote.data.bible

import org.json.JSONObject

/**
 * 예수님 말씀(붉은 글씨) 구간.
 *
 * 본문 JSON의 절 객체에 "jesus"가 있으면, 심을 때(BibleSeeder) 글자 위치로 바꿔 [BibleVerse.redLetter]에 저장하고,
 * 성경 탭(VerseAdapter)이 그 위치를 붉은 글씨로 칠한다.
 *
 * JSON 형식(Notion "예수님 말씀" 페이지):
 * - 구절 전체: `"jesus": { "all": true }`
 * - 일부: `"jesus": { "ranges": [ { "start": "이제", "end": "합당하니라" } ] }` — start로 시작하는 곳부터
 *   그 뒤에 처음 나오는 end가 끝나는 곳까지. 한 단어면 start와 end를 같게. 구간은 여러 개 둘 수 있다.
 *   start · end 모두 단어로 먼저 찾고(같은 글자가 다른 단어 안에 먼저 나와도 건너뜀), 단어로 못 찾으면
 *   글자 그대로 찾는다(그래서 end는 단어 중간에서 끝나도 된다). 같은 단어가 여러 번 나오면 처음 것을 쓴다.
 * - 같은 단어가 앞에도 있어서 위치를 정해야 할 때만 "startIndex" / "endIndex"(본문 안 글자 위치, 0부터)를 쓴다.
 *   startIndex는 start가 시작하는 위치. endIndex는 end가 시작하는 위치든 끝나는 위치든 둘 다 받아준다.
 *   적은 위치에 그 단어가 정확히 없으면 그 위치에서 가장 가까운 것을 쓴다.
 *
 * 절이 소제목으로 나뉜 경우(text_2)도 있어서, 저장할 땐 "조각:시작-끝"을 쉼표로 이어 쓴다.
 * 조각 0 = text, 1 = text2. 예: "0:3-15,1:0-20". 끝 위치는 포함하지 않는다.
 */
object RedLetter {

	data class Range(val segment: Int, val start: Int, val end: Int)

	/** 저장된 문자열 → 구간 목록. 형식이 이상한 조각은 건너뛴다. */
	fun decode(encoded: String?): List<Range> {
		if (encoded.isNullOrBlank()) return emptyList()
		return encoded.split(',').mapNotNull { part ->
			val segment = part.substringBefore(':', "").toIntOrNull() ?: return@mapNotNull null
			val span = part.substringAfter(':', "")
			val start = span.substringBefore('-', "").toIntOrNull() ?: return@mapNotNull null
			val end = span.substringAfter('-', "").toIntOrNull() ?: return@mapNotNull null
			if (end > start) Range(segment, start, end) else null
		}
	}

	private fun encode(ranges: List<Range>): String? =
		ranges.filter { it.end > it.start }
			.joinToString(",") { "${it.segment}:${it.start}-${it.end}" }
			.takeIf { it.isNotEmpty() }

	/**
	 * 절 객체의 "jesus"를 읽어 저장할 문자열로 바꾼다. 없거나 본문에서 못 찾으면 null(그 구간만 빠진다).
	 * [onMissing]에는 못 찾은 구간 설명을 넘긴다(로그용).
	 */
	fun resolve(
		jesus: JSONObject?,
		text: String,
		text2: String?,
		onMissing: (String) -> Unit = {}
	): String? {
		if (jesus == null) return null
		val ranges = mutableListOf<Range>()

		if (jesus.optBoolean("all", false)) {
			ranges += Range(0, 0, text.length)
			if (!text2.isNullOrEmpty()) ranges += Range(1, 0, text2.length)
			return encode(ranges)
		}

		val array = jesus.optJSONArray("ranges") ?: return null
		for (i in 0 until array.length()) {
			val item = array.optJSONObject(i) ?: continue
			val startWord = item.optString("start")
			val endWord = item.optString("end").ifEmpty { startWord }
			if (startWord.isEmpty()) continue
			val startIndex = item.optInt("startIndex", -1)
			val endIndex = item.optInt("endIndex", -1)

			val found = findRange(text, startWord, endWord, startIndex, endIndex)?.let {
				listOf(Range(0, it.first, it.second))
			} ?: text2?.let { second ->
				findRange(second, startWord, endWord, startIndex, endIndex)?.let {
					listOf(Range(1, it.first, it.second))
				}
			} ?: text2?.let { second ->
				// 드물게 소제목을 사이에 두고 말씀이 이어지는 경우: text에서 시작해 text2에서 끝난다.
				val start = findStart(text, startWord, startIndex) ?: return@let null
				val end = findEnd(second, endWord, 0, endIndex) ?: return@let null
				listOf(Range(0, start, text.length), Range(1, 0, end))
			}

			if (found == null) onMissing("start=\"$startWord\", end=\"$endWord\"") else ranges += found
		}
		return encode(ranges)
	}

	/** text 안에서 [시작, 끝) 위치. 못 찾으면 null. */
	private fun findRange(
		text: String,
		startWord: String,
		endWord: String,
		startIndex: Int,
		endIndex: Int
	): Pair<Int, Int>? {
		val start = findStart(text, startWord, startIndex) ?: return null
		val end = findEnd(text, endWord, start, endIndex) ?: return null
		return start to end
	}

	/**
	 * start가 시작하는 위치. startIndex가 없으면 "단어"로 찾는다 — 그냥 처음 나오는 글자를 찾으면
	 * start "이"가 "이에"의 "이"에 걸리는 것처럼 엉뚱한 단어 안에서 시작해 버려서:
	 * 1. 앞뒤가 띄어쓰기 · 문장부호로 끊긴 같은 단어(예: "이 글이"의 "이")
	 * 2. 없으면 단어의 시작 부분(예: start "예수" → "예수께서")
	 * 3. 그래도 없으면 글자 그대로 처음 나오는 곳
	 * 순서로 찾는다. 같은 조건에 맞는 곳이 여러 개면(같은 단어가 두 번 나오는 등) 처음 나오는 것을 쓴다.
	 */
	private fun findStart(text: String, word: String, index: Int): Int? {
		if (index >= 0 && text.startsWith(word, index)) return index
		if (index >= 0) return nearestOccurrence(text, word, index)
		val positions = occurrences(text, word)
		return positions.firstOrNull {
			isBoundaryBefore(text, it) && isBoundaryAfter(
				text,
				it + word.length
			)
		}
			?: positions.firstOrNull { isBoundaryBefore(text, it) }
			?: positions.firstOrNull()
	}

	private fun occurrences(text: String, word: String): List<Int> {
		val result = mutableListOf<Int>()
		var pos = text.indexOf(word)
		while (pos >= 0) {
			result += pos
			pos = text.indexOf(word, pos + 1)
		}
		return result
	}

	/** 단어 경계: 글 처음 · 끝이거나, 띄어쓰기 · 문장부호(따옴표 · 괄호 포함)와 맞닿은 곳. */
	private fun isBoundaryBefore(text: String, pos: Int): Boolean =
		pos == 0 || isSeparator(text[pos - 1])

	private fun isBoundaryAfter(text: String, pos: Int): Boolean =
		pos >= text.length || isSeparator(text[pos])

	private fun isSeparator(c: Char): Boolean = !c.isLetterOrDigit()

	/**
	 * end 단어가 끝나는 위치(포함 안 함). [from] 이후에서 찾는다. endIndex가 없으면 start처럼 "단어"로 먼저 찾는다:
	 * 1. 앞뒤가 띄어쓰기 · 문장부호로 끊긴 같은 단어
	 * 2. 없으면 단어의 끝부분(예: end "느니라" → "응하였느니라")
	 * 3. 그래도 없으면 글자 그대로 처음 나오는 곳(단어 중간에서 끝나는 경우)
	 * 같은 조건에 맞는 곳이 여러 개면 start 뒤에서 처음 나오는 것을 쓴다.
	 */
	private fun findEnd(text: String, word: String, from: Int, index: Int): Int? {
		if (index >= 0) {
			// endIndex가 end 단어의 시작 위치인 경우
			if (index >= from && text.startsWith(word, index)) return index + word.length
			// endIndex가 end 단어의 끝 위치인 경우
			val begin = index - word.length
			if (begin >= from && text.startsWith(word, begin)) return index
			return nearestOccurrence(text, word, index, from)?.let { it + word.length }
		}
		val positions = occurrences(text, word).filter { it >= from }
		val pos = positions.firstOrNull {
			isBoundaryBefore(text, it) && isBoundaryAfter(
				text,
				it + word.length
			)
		}
			?: positions.firstOrNull { isBoundaryAfter(text, it + word.length) }
			?: positions.firstOrNull()
		return pos?.let { it + word.length }
	}

	private fun nearestOccurrence(text: String, word: String, near: Int, from: Int = 0): Int? {
		var best: Int? = null
		var pos = text.indexOf(word, from)
		while (pos >= 0) {
			if (best == null || kotlin.math.abs(pos - near) < kotlin.math.abs(best - near)) best =
				pos
			pos = text.indexOf(word, pos + 1)
		}
		return best
	}
}