package com.chan.bnote.ui.bible.picker

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/**
 * 성경 선택 bottom sheet 상단 탭(성경 | 장 | 절) 한 칸을 나타낸다.
 */
data class PickerTab(
	val label: String,
	val enabled: Boolean,
	val selected: Boolean,
	val onClick: () -> Unit
)

/**
 * [container]에 [tabs]를 균등한 너비로 그린다.
 * BookChapterPickerBottomSheet, BibleRangePickerBottomSheet에서 공통으로 사용한다.
 */
fun renderPickerTabs(context: Context, container: LinearLayout, tabs: List<PickerTab>) {
	container.removeAllViews()
	val density = context.resources.displayMetrics.density
	fun dp(value: Int) = (value * density).toInt()

	for (tab in tabs) {
		val textView = TextView(context).apply {
			text = tab.label
			gravity = Gravity.CENTER
			textSize = 15f
			setPadding(0, dp(12), 0, dp(12))
			layoutParams = LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
			)

			when {
				tab.selected -> {
					setTextColor(ContextCompat.getColor(context, R.color.brown_primary))
					setTypeface(typeface, Typeface.BOLD)
					background = ContextCompat.getDrawable(context, R.drawable.bg_tab_selected)
				}

				tab.enabled -> {
					setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
					setTypeface(typeface, Typeface.NORMAL)
					val outValue = TypedValue()
					context.theme.resolveAttribute(
						android.R.attr.selectableItemBackground, outValue, true
					)
					setBackgroundResource(outValue.resourceId)
				}

				else -> {
					setTextColor(ContextCompat.getColor(context, R.color.text_hint))
					setTypeface(typeface, Typeface.NORMAL)
					background = null
				}
			}

			isClickable = tab.enabled
			isFocusable = tab.enabled
			if (tab.enabled) {
				setOnClickListener { tab.onClick() }
			}
		}
		container.addView(textView)
	}
}