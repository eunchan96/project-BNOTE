package com.chan.bnote.ui.bible

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.memo.CitationMatch
import com.chan.bnote.data.memo.CitationParser
import com.chan.bnote.ui.sermon.TriangleBubbleDrawable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object CitationBubbleHelper {

	fun applySpans(editable: Editable): List<CitationMatch> {
		val existing = editable.getSpans(0, editable.length, UnderlineSpan::class.java)
		for (span in existing) editable.removeSpan(span)

		val citations = CitationParser.findCitations(editable.toString())
		for (c in citations) {
			editable.setSpan(
				UnderlineSpan(),
				c.range.first,
				c.range.last + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		return citations
	}

	fun buildSpannedText(text: String): Pair<SpannableString, List<CitationMatch>> {
		val citations = CitationParser.findCitations(text)
		val spannable = SpannableString(text)
		for (c in citations) {
			spannable.setSpan(
				UnderlineSpan(),
				c.range.first,
				c.range.last + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		return spannable to citations
	}

	fun attachTouchHandling(
		textView: TextView,
		getCitations: () -> List<CitationMatch>,
		scope: LifecycleCoroutineScope
	) {
		var popupWindow: PopupWindow? = null
		var loadJob: Job? = null

		textView.setOnTouchListener { view, event ->
			val tv = view as TextView
			val layout = tv.layout ?: return@setOnTouchListener false
			val citations = getCitations()
			if (citations.isEmpty()) return@setOnTouchListener false

			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					val touchX = event.x - tv.totalPaddingLeft + tv.scrollX
					val touchY = event.y - tv.totalPaddingTop + tv.scrollY
					val line = layout.getLineForVertical(touchY.toInt())
					val charOffset = layout.getOffsetForHorizontal(line, touchX)

					val hitCitation = citations.firstOrNull { c ->
						charOffset >= c.range.first && charOffset <= c.range.last + 1 &&
								layout.getLineForOffset(c.range.first) == line
					}

					if (hitCitation != null) {
						val lineTop = layout.getLineTop(line) + tv.totalPaddingTop - tv.scrollY
						val startX =
							layout.getPrimaryHorizontal(hitCitation.range.first) + tv.totalPaddingLeft - tv.scrollX
						val safeEnd = (hitCitation.range.last + 1).coerceAtMost(layout.text.length)
						val endX =
							layout.getPrimaryHorizontal(safeEnd) + tv.totalPaddingLeft - tv.scrollX
						val centerX = (startX + endX) / 2f

						loadJob?.cancel()
						loadJob = scope.launch {
							val content = buildQuoteText(tv.context, hitCitation)
							popupWindow?.dismiss()
							popupWindow = showBubble(tv, content, centerX, lineTop)
						}
						true
					} else {
						false
					}
				}

				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					loadJob?.cancel()
					popupWindow?.dismiss()
					popupWindow = null
					false
				}

				else -> false
			}
		}
	}

	private suspend fun buildQuoteText(context: Context, citation: CitationMatch): String {
		val db = BibleDatabase.getInstance(context.applicationContext)
		val bookName = BibleBooks.nameOf(citation.bookId)

		val blocks = citation.groups.map { group ->
			val versesLabel = group.segments.joinToString(", ") { seg ->
				if (seg.start == seg.end) "${seg.start}" else "${seg.start}~${seg.end}"
			}
			val matchedVerses = group.segments.flatMap { it.start..it.end }.toSet()
			val allVerses = db.bibleDao().getVerses("GAEYEOK", citation.bookId, group.chapter)
			val bodyText = allVerses.filter { it.verse in matchedVerses }
				.sortedBy { it.verse }
				.joinToString("\n") { it.text }

			"$bookName ${group.chapter}장 ${versesLabel}절\n$bodyText"
		}
		return blocks.joinToString("\n")
	}

	private fun showBubble(
		anchor: TextView,
		text: String,
		localCenterX: Float,
		localTopY: Int
	): PopupWindow {
		val context = anchor.context
		val density = context.resources.displayMetrics.density
		val screenWidth = context.resources.displayMetrics.widthPixels

		val popupWidth = (screenWidth * 0.9f).toInt()
		val sideMargin = (screenWidth - popupWidth) / 2 // 화면 중앙 배치 시 좌우 여백 (자동으로 10%씩)

		val loc = IntArray(2)
		anchor.getLocationOnScreen(loc)

		// 화면 정중앙에 고정 배치
		val actualX = sideMargin.toFloat()

		// 꼬리가 가리켜야 할 실제 지점(인용구 중앙)이 말풍선 내에서 몇 % 위치인지 계산
		val citationScreenX = loc[0] + localCenterX
		val tailRatio = ((citationScreenX - actualX) / popupWidth).coerceIn(0.08f, 0.92f)

		val bubbleText = TextView(context).apply {
			setText(text)
			setTextColor(Color.WHITE)
			textSize = 13f
			val horizontalPad = (16 * density).toInt()
			setPadding(horizontalPad, (12 * density).toInt(), horizontalPad, (16 * density).toInt())
			background = TriangleBubbleDrawable(tailRatio, density)
		}

		bubbleText.measure(
			View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		)
		val popupHeight = bubbleText.measuredHeight

		val popup = PopupWindow(bubbleText, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
		popup.isFocusable = false
		popup.isTouchable = false
		popup.elevation = 8f

		val y = (loc[1] + localTopY - popupHeight - (6 * density)).toInt()

		popup.showAtLocation(anchor, Gravity.NO_GRAVITY, actualX.toInt(), y.coerceAtLeast(0))
		return popup
	}
}