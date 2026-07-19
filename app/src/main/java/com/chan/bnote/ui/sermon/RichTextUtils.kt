package com.chan.bnote.ui.sermon

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.core.text.HtmlCompat

/**
 * 설교 메모의 굵게/밑줄 서식을 저장 문자열(HTML)과 화면에 보여줄 Spanned 사이를 오간다.
 *
 * 서식이 없는 평범한 메모는 그냥 일반 텍스트로 저장한다 (기존에 저장된 메모들과 100% 호환).
 * 서식(굵게/밑줄)이 하나라도 있을 때만 HTML로 저장한다.
 */
object RichTextUtils {

	private val htmlLikeRegex = Regex("<(b|u|p|font)[ >]", RegexOption.IGNORE_CASE)

	/** 저장된 문자열을 화면/편집에 쓸 CharSequence로 되돌린다. */
	fun toEditable(raw: String): CharSequence {
		if (raw.isEmpty() || !looksLikeHtml(raw)) return raw
		var restored = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY) as CharSequence
		// Html.fromHtml이 문단마다 붙이는 끝 개행을 하나 정리한다.
		if (restored.isNotEmpty() && restored.last() == '\n') {
			restored = restored.subSequence(0, restored.length - 1)
		}
		return restored
	}

	/** 편집기의 텍스트를 저장용 문자열로 바꾼다. 서식이 없으면 순수 텍스트 그대로. */
	fun toStorageString(input: CharSequence): String {
		if (input !is Spanned || !hasRichSpans(input)) return input.toString()
		val spannable = if (input is Spannable) input else SpannableString(input)
		return HtmlCompat.toHtml(spannable, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
	}

	private fun looksLikeHtml(text: String): Boolean = htmlLikeRegex.containsMatchIn(text)

	private fun hasRichSpans(spanned: Spanned): Boolean {
		return spanned.getSpans(0, spanned.length, StyleSpan::class.java).isNotEmpty() ||
				spanned.getSpans(0, spanned.length, UnderlineSpan::class.java).isNotEmpty() ||
				spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java).isNotEmpty()
	}

	/** 선택 구간의 글자 색을 바꾼다. 이미 색이 지정돼 있으면 먼저 지우고 새로 씌운다. */
	fun applyColor(editable: Editable, start: Int, end: Int, color: Int) {
		if (start >= end) return
		val existing = editable.getSpans(start, end, ForegroundColorSpan::class.java)
		for (span in existing) {
			if (editable.getSpanStart(span) <= start && editable.getSpanEnd(span) >= end) {
				editable.removeSpan(span)
			}
		}
		editable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
	}

	/** 선택 구간에 굵게/밑줄이 이미 완전히 덮여있는지 보고, 없으면 씌우고 있으면 벗긴다. */
	fun toggleStyle(editable: Editable, start: Int, end: Int, bold: Boolean) {
		if (start >= end) return
		val spanClass = if (bold) StyleSpan::class.java else UnderlineSpan::class.java
		val existing = editable.getSpans(start, end, spanClass)
		val alreadyStyled = existing.any { span ->
			editable.getSpanStart(span) <= start && editable.getSpanEnd(span) >= end &&
					(!bold || (span as? StyleSpan)?.style == Typeface.BOLD)
		}
		if (alreadyStyled) {
			for (span in existing) {
				if (editable.getSpanStart(span) <= start && editable.getSpanEnd(span) >= end) {
					editable.removeSpan(span)
				}
			}
		} else {
			val newSpan = if (bold) StyleSpan(Typeface.BOLD) else UnderlineSpan()
			editable.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
	}
}