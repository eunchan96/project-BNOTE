package com.chan.bnote.data.notice

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 알림 본문(GitHub 이슈 마크다운)과 날짜를 화면에 보이기 좋게 다듬는다. */
object NoticeFormatter {

	private val MARKDOWN_LINK = Regex("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)")

	/**
	 * 마크다운을 읽기 좋은 일반 텍스트로: "## 제목" → "제목", "- 항목" → "・ 항목", "**굵게**" → "굵게",
	 * "[글자](주소)" → "글자 (주소)"(주소는 화면에서 링크로 눌린다), 빈 줄이 여러 개면 한 줄로.
	 */
	fun toDisplayText(markdown: String): String {
		val lines = markdown.replace("\r\n", "\n").lines().map { raw ->
			var line = raw.trimEnd()
			line = line.replace(Regex("^\\s*#{1,6}\\s+"), "")
			line = line.replace(Regex("^(\\s*)[-*]\\s+"), "$1・ ")
			line = line.replace("**", "").replace("__", "")
			line = MARKDOWN_LINK.replace(line) { "${it.groupValues[1]} (${it.groupValues[2]})" }
			line
		}
		return lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
	}

	/** 목록 미리보기용 — 줄바꿈 없이 한 덩어리로. */
	fun preview(markdown: String): String =
		toDisplayText(markdown).lines().filter { it.isNotBlank() }.joinToString(" ")

	/** 목록의 날짜 — 오늘이면 "오후 3:20", 올해면 "10월 9일", 그 전이면 "2025.12.3". */
	fun listDate(millis: Long, now: Long = System.currentTimeMillis()): String {
		val target = Calendar.getInstance().apply { timeInMillis = millis }
		val today = Calendar.getInstance().apply { timeInMillis = now }
		val pattern = when {
			target.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> "yyyy.M.d"
			target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "a h:mm"
			else -> "M월 d일"
		}
		return SimpleDateFormat(pattern, Locale.KOREA).format(millis)
	}

	/** 상세 화면의 날짜 — "2026년 10월 10일 오후 3:20". */
	fun fullDate(millis: Long): String =
		SimpleDateFormat("yyyy년 M월 d일 a h:mm", Locale.KOREA).format(millis)
}