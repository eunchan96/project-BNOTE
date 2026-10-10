package com.chan.bnote.data.notice

import com.chan.bnote.data.notice.NoticeFormatter.toDisplayText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 알림 본문을 화면에 그릴 때의 한 덩어리 — 글 또는 사진. */
sealed class NoticeBlock {
	data class Text(val text: String) : NoticeBlock()
	data class Image(val url: String) : NoticeBlock()
}

/** 알림 본문(GitHub 이슈 마크다운)과 날짜를 화면에 보이기 좋게 다듬는다. */
object NoticeFormatter {

	/**
	 * 본문 속 사진. GitHub에 사진을 끌어다 놓으면 들어가는 `<img ... src="주소">` 태그와
	 * 마크다운 `![설명](주소)` 둘 다 알아본다. 주소는 group 1(img 태그) 또는 group 2(마크다운).
	 */
	private val IMAGE = Regex(
		"<img\\b[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>|!\\[[^\\]]*]\\((https?://[^)\\s]+)[^)]*\\)",
		RegexOption.IGNORE_CASE
	)

	private val MARKDOWN_LINK = Regex("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)")
	private val HTML_COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)

	/**
	 * 이슈 템플릿에 들어 있는 작성 안내(<!-- ... -->, 여러 줄 가능)를 지운다. GitHub 화면에서도 안 보이는
	 * 부분이라 앱에서도 빼고, 줄바꿈은 "\n"으로 통일한다.
	 */
	fun stripHtmlComments(text: String): String =
		text.replace("\r\n", "\n").replace(HTML_COMMENT, "")

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

	/**
	 * 상세 화면용 — 본문을 사진 자리에서 나눠 글과 사진 순서대로 돌려준다. 글 조각은 [toDisplayText]로
	 * 다듬고, 다듬은 뒤 비어 있는 조각(사진 사이의 빈 줄 등)은 뺀다.
	 */
	fun toBlocks(markdown: String): List<NoticeBlock> {
		val blocks = mutableListOf<NoticeBlock>()
		fun addText(raw: String) {
			val text = toDisplayText(raw)
			if (text.isNotBlank()) blocks.add(NoticeBlock.Text(text))
		}

		var last = 0
		for (match in IMAGE.findAll(markdown)) {
			addText(markdown.substring(last, match.range.first))
			val url = match.groupValues[1].ifBlank { match.groupValues[2] }.trim()
			if (url.startsWith("http://") || url.startsWith("https://")) {
				blocks.add(NoticeBlock.Image(url))
			}
			last = match.range.last + 1
		}
		addText(markdown.substring(last))
		return blocks
	}

	/** 목록 미리보기용 — 사진은 빼고 줄바꿈 없이 한 덩어리로. 사진만 있는 공지면 "📷 사진". */
	fun preview(markdown: String): String {
		val text = toDisplayText(markdown.replace(IMAGE, "\n"))
			.lines().filter { it.isNotBlank() }.joinToString(" ")
		return if (text.isBlank() && IMAGE.containsMatchIn(markdown)) "📷 사진" else text
	}

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