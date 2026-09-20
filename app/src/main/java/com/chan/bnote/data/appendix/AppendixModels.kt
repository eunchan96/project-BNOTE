package com.chan.bnote.data.appendix

/**
 * 주기도문 / 사도신경처럼 "여러 번역본(version) + 줄 단위 텍스트" 형태의 콘텐츠 공통 모델.
 */
data class TextVersion(
	val id: String,
	val label: String,
	val lines: List<String>
)

data class VersionedTextContent(
	val title: String,
	val versions: List<TextVersion>
)

/**
 * 십계명 전용 모델.
 */
data class CommandmentItem(
	val number: Int,
	val text: String
)

data class CommandmentSummary(
	val text: String,
	val reference: String
)

data class TenCommandmentsContent(
	val title: String,
	val intro: List<String>,
	val commandments: List<CommandmentItem>,
	val reference: String,
	val summary: CommandmentSummary
)

/**
 * 교독문 전용 모델.
 */
enum class ReadingSpeaker {
	LEADER,
	CONGREGATION,
	UNISON;

	companion object {
		fun fromJson(value: String): ReadingSpeaker = when (value) {
			"congregation" -> CONGREGATION
			"unison" -> UNISON
			else -> LEADER
		}
	}
}

data class ResponsiveReadingLine(
	val speaker: ReadingSpeaker,
	val text: String
)

data class ResponsiveReading(
	val number: Int,
	val title: String,
	val lines: List<ResponsiveReadingLine>
)