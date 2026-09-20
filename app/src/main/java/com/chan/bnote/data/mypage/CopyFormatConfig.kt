package com.chan.bnote.data.mypage

import org.json.JSONObject

/**
 * 복사 형식 설정값 전체. bottom sheet에서 사용자가 고른 조합을 담는다.
 * Room에는 이 값을 JSON 문자열로 직렬화해서 저장한다(옵션이 많아 컬럼을 따로 두지 않음).
 */
data class CopyFormatConfig(
	// 본문(참조)과 구절 텍스트 사이 구분자
	val refVerseSeparator: Separator = Separator.SPACE,
	// 구절이 여러 개일 때, 구절끼리의 구분자
	val multiVerseSeparator: Separator = Separator.NEWLINE,
	// refVerseSeparator가 SPACE일 때, 참조가 텍스트 앞/뒤 어디에 붙는지
	val refPosition: RefPosition = RefPosition.BEFORE,
	// 참조 길이: 짧게(창 1:1) / 중간(창세기 1:1) / 길게(창세기 1장 1절)
	val refLength: RefLength = RefLength.SHORT,
	// 책이름과 장절 사이 띄어쓰기 여부: "창 1:1" vs "창1:1"
	val refSpacing: Boolean = true,
	// 참조를 감싸는 괄호
	val refBracket: RefBracket = RefBracket.PAREN,
	// 구절 텍스트 앞뒤에 큰따옴표를 붙일지
	val quoteVerse: Boolean = false,
	// 구절이 여러 개일 때 각 구절 앞에 절 번호를 표시할지 (refVerseSeparator=NEWLINE이면 항상 표시)
	val showVerseNumberWhenMulti: Boolean = false,
	// 절 번호 표시 유형: 1 / 1. / [1]
	val verseNumberStyle: VerseNumberStyle = VerseNumberStyle.PLAIN,
	// 절 번호와 본문 사이 띄어쓰기 개수 (1~3칸)
	val verseNumberSpacing: Int = 1
) {
	enum class Separator { NEWLINE, SPACE }
	enum class RefPosition { BEFORE, AFTER }
	enum class RefLength { SHORT, MEDIUM, LONG }
	enum class RefBracket { NONE, PAREN, SQUARE }
	enum class VerseNumberStyle { PLAIN, DOT, BRACKET }

	fun toJson(): String {
		val o = JSONObject()
		o.put("refVerseSeparator", refVerseSeparator.name)
		o.put("multiVerseSeparator", multiVerseSeparator.name)
		o.put("refPosition", refPosition.name)
		o.put("refLength", refLength.name)
		o.put("refSpacing", refSpacing)
		o.put("refBracket", refBracket.name)
		o.put("quoteVerse", quoteVerse)
		o.put("showVerseNumberWhenMulti", showVerseNumberWhenMulti)
		o.put("verseNumberStyle", verseNumberStyle.name)
		o.put("verseNumberSpacing", verseNumberSpacing)
		return o.toString()
	}

	companion object {
		fun fromJson(json: String): CopyFormatConfig {
			return try {
				val o = JSONObject(json)
				CopyFormatConfig(
					refVerseSeparator = Separator.valueOf(
						o.optString(
							"refVerseSeparator",
							"SPACE"
						)
					),
					multiVerseSeparator = Separator.valueOf(
						o.optString(
							"multiVerseSeparator",
							"NEWLINE"
						)
					),
					refPosition = RefPosition.valueOf(o.optString("refPosition", "BEFORE")),
					refLength = RefLength.valueOf(o.optString("refLength", "SHORT")),
					refSpacing = o.optBoolean("refSpacing", true),
					refBracket = RefBracket.valueOf(o.optString("refBracket", "PAREN")),
					quoteVerse = o.optBoolean("quoteVerse", false),
					showVerseNumberWhenMulti = o.optBoolean("showVerseNumberWhenMulti", false),
					verseNumberStyle = VerseNumberStyle.valueOf(
						o.optString(
							"verseNumberStyle",
							"PLAIN"
						)
					),
					verseNumberSpacing = o.optInt("verseNumberSpacing", 1)
				)
			} catch (e: Exception) {
				CopyFormatConfig()
			}
		}
	}
}