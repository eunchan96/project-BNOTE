package com.chan.bnote.ui.common

import android.content.Context
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/**
 * 절 본문처럼 이미 배경색(하이라이트)이 깔려있을 수 있는 텍스트에서, 드래그/롱프레스로 선택한
 * 구간을 확실히 보여주기 위한 TextView.
 *
 * 안드로이드 기본 선택 표시(옅은 배경 칠하기)는, 우리가 미리 칠해둔 하이라이트(BackgroundColorSpan)보다
 * 먼저 그려지는 구조라서 하이라이트 색에 항상 가려 안 보였다. 그래서 안드로이드의 자체 선택 표시에
 * 기대는 대신, 하이라이트와 똑같은 방식(스패너블에 직접 배경 스팬을 추가)으로 회색 배경을 직접
 * 얹는다 — 나중에 추가된 스팬이라 하이라이트 위에 그려져서 확실히 보인다. 글자색은 그대로 둔다.
 */
class SelectionAwareTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

	private var activeSelectionSpan: Any? = null

	override fun onSelectionChanged(selStart: Int, selEnd: Int) {
		super.onSelectionChanged(selStart, selEnd)

		val spannable = text as? Spannable ?: return

		activeSelectionSpan?.let { spannable.removeSpan(it) }
		activeSelectionSpan = null

		if (selStart < 0 || selEnd < 0) return
		val start = minOf(selStart, selEnd)
		val end = maxOf(selStart, selEnd)
		if (start == end || start !in 0..spannable.length || end !in 0..spannable.length) return

		val bgSpan =
			BackgroundColorSpan(ContextCompat.getColor(context, R.color.verse_selection_background))
		spannable.setSpan(bgSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		activeSelectionSpan = bgSpan
	}
}