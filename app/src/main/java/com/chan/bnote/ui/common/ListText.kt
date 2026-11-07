package com.chan.bnote.ui.common

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan
import android.widget.TextView
import com.chan.bnote.data.notice.NoticeHeadingSpan
import kotlin.math.roundToInt

/**
 * 목록("・ 항목", "1. 항목", "※ 항목")이 들어간 글을 읽기 좋게 다듬는다. 알림 상세 · 새 버전 안내 창 ·
 * 업데이트 내역이 같이 쓴다.
 *
 * - 불릿 "・"을 글꼴 기본 점보다 조금 큰 동그라미로 그린다([BulletDotSpan]). 폭은 원래 "・"과 같아서
 *   들여쓰기 위치는 그대로다.
 * - 한 항목이 길어서 두 줄 이상이 되면 둘째 줄부터 기호가 아니라 글자 시작 위치에 맞춰 들여쓴다.
 * - [paragraphGapEm]을 주면, 줄바꿈으로 나뉜 줄(항목 · 문단)마다 아래에 그만큼(글자 크기 배수) 간격을 둔다.
 *   그러면 TextView의 줄 간격은 좁게 둬도 — 한 항목 안에서 넘어간 줄끼리는 촘촘하고 — 항목 사이는 떨어져 보인다.
 *   빈 줄(문단 구분)은 한 줄 높이 대신 [blankLineEm]만큼만 차지한다.
 * - 소제목 줄([NoticeHeadingSpan]이 붙은 줄)은 위쪽을 [headingTopEm]만큼 더 띄우고, 아래 내용과는
 *   [headingBottomEm]만큼만 띄운다 — 소제목이 앞 구역과는 떨어지고 자기 내용과는 붙어 보이게.
 *
 * textSize를 먼저 정한 TextView를 넘겨야 paint로 기호 폭을 정확히 잰다.
 */
object ListText {

	/** 목록 줄의 앞부분: (들여쓰기) + "・ ", "※ ", "💡 " 또는 "1. " 같은 번호. */
	private val LIST_PREFIX = Regex("^\\s*(・|※|💡|\\d+\\.)\\s+")

	fun style(
		text: CharSequence,
		textView: TextView,
		paragraphGapEm: Float? = null,
		blankLineEm: Float = 0.5f,
		headingTopEm: Float = 0.8f,
		headingBottomEm: Float = 0.3f
	): CharSequence {
		val spannable = SpannableString(text)
		val textSizePx = textView.textSize
		val lines = text.split('\n')
		var lineStart = 0
		lines.forEachIndexed { index, line ->
			val lineEnd = lineStart + line.length
			val isLast = index == lines.lastIndex
			val endWithBreak = (lineEnd + 1).coerceAtMost(text.length)

			LIST_PREFIX.find(line)?.value?.let { prefix ->
				val margin = textView.paint.measureText(prefix).toInt()
				spannable.setSpan(
					LeadingMarginSpan.Standard(0, margin),
					lineStart, endWithBreak,
					Spannable.SPAN_INCLUSIVE_EXCLUSIVE
				)
				val bulletAt = prefix.indexOf('・')
				if (bulletAt >= 0) {
					spannable.setSpan(
						BulletDotSpan(),
						lineStart + bulletAt, lineStart + bulletAt + 1,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
					)
				}
			}

			val isHeading = lineEnd > lineStart &&
					spannable.getSpans(lineStart, lineEnd, NoticeHeadingSpan::class.java)
						.isNotEmpty()
			if (paragraphGapEm != null && isHeading && lineStart > 0) {
				spannable.setSpan(
					TopGapSpan((textSizePx * headingTopEm).roundToInt()),
					lineStart, endWithBreak,
					Spanned.SPAN_INCLUSIVE_EXCLUSIVE
				)
			}

			if (paragraphGapEm != null && !isLast) {
				if (line.isBlank()) {
					spannable.setSpan(
						FixedLineHeightSpan((textSizePx * blankLineEm).roundToInt()),
						lineStart, endWithBreak,
						Spanned.SPAN_INCLUSIVE_EXCLUSIVE
					)
				} else if (lines[index + 1].isNotBlank()) {
					// 다음 줄이 빈 줄이면 빈 줄이 이미 간격 역할을 하므로 더하지 않는다.
					val gapEm = if (isHeading) headingBottomEm else paragraphGapEm
					spannable.setSpan(
						ParagraphGapSpan((textSizePx * gapEm).roundToInt()),
						lineStart, endWithBreak,
						Spanned.SPAN_INCLUSIVE_EXCLUSIVE
					)
				}
			}
			lineStart = lineEnd + 1
		}
		return spannable
	}

	/**
	 * "・" 자리에 조금 더 큰 점을 그린다. 차지하는 폭은 원래 "・"과 같고(들여쓰기 계산이 그대로 맞음),
	 * 줄 높이는 건드리지 않는다. 크기 · 높이는 글자 크기 기준이라 제목처럼 큰 글씨에서도 비율이 같다.
	 */
	class BulletDotSpan : ReplacementSpan() {
		override fun getSize(
			paint: Paint,
			text: CharSequence?,
			start: Int,
			end: Int,
			fm: Paint.FontMetricsInt?
		): Int {
			if (fm != null) {
				val metrics = paint.fontMetricsInt
				fm.ascent = metrics.ascent
				fm.descent = metrics.descent
				fm.top = metrics.top
				fm.bottom = metrics.bottom
			}
			return paint.measureText("・").roundToInt()
		}

		override fun draw(
			canvas: Canvas,
			text: CharSequence?,
			start: Int,
			end: Int,
			x: Float,
			top: Int,
			y: Int,
			bottom: Int,
			paint: Paint
		) {
			val width = paint.measureText("・")
			val radius = paint.textSize * DOT_RADIUS_EM
			// 한글 글자의 세로 가운데쯤(기준선에서 글자 크기의 약 0.36배 위)에 맞춘다.
			val cy = y - paint.textSize * DOT_CENTER_EM
			val oldStyle = paint.style
			paint.style = Paint.Style.FILL
			canvas.drawCircle(x + width / 2f, cy, radius, paint)
			paint.style = oldStyle
		}

		private companion object {
			const val DOT_RADIUS_EM = 0.085f
			const val DOT_CENTER_EM = 0.36f
		}
	}

	/** 이 span이 시작하는 줄(소제목의 첫 줄) 위에만 [gapPx]만큼 간격을 더한다. */
	private class TopGapSpan(private val gapPx: Int) : LineHeightSpan {
		override fun chooseHeight(
			text: CharSequence,
			start: Int,
			end: Int,
			spanstartv: Int,
			lineHeight: Int,
			fm: Paint.FontMetricsInt
		) {
			val spanStart = (text as? Spanned)?.getSpanStart(this) ?: return
			if (start == spanStart) {
				fm.ascent -= gapPx
				fm.top -= gapPx
			}
		}
	}

	/** 이 span이 끝나는 줄(문단의 마지막 줄) 아래에만 [gapPx]만큼 간격을 더한다. */
	private class ParagraphGapSpan(private val gapPx: Int) : LineHeightSpan {
		override fun chooseHeight(
			text: CharSequence,
			start: Int,
			end: Int,
			spanstartv: Int,
			lineHeight: Int,
			fm: Paint.FontMetricsInt
		) {
			val spanEnd = (text as? Spanned)?.getSpanEnd(this) ?: return
			if (end == spanEnd) {
				fm.descent += gapPx
				fm.bottom += gapPx
			}
		}
	}

	/** 빈 줄의 높이를 [heightPx]로 줄인다(한 줄을 통째로 비우면 문단 사이가 너무 벌어져 보여서). */
	private class FixedLineHeightSpan(private val heightPx: Int) : LineHeightSpan {
		override fun chooseHeight(
			text: CharSequence,
			start: Int,
			end: Int,
			spanstartv: Int,
			lineHeight: Int,
			fm: Paint.FontMetricsInt
		) {
			val current = fm.descent - fm.ascent
			if (current <= 0 || heightPx >= current) return
			fm.ascent = -heightPx
			fm.descent = 0
			fm.top = fm.ascent
			fm.bottom = 0
		}
	}
}