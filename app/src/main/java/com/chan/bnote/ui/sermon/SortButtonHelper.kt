package com.chan.bnote.ui.sermon

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/** 캘린더/성경별 탭 상단의 정렬 버튼(예: "카테고리순 ▾")을 눌렀을 때 작은 팝업으로 정렬을 고르게 한다. */
object SortButtonHelper {

	fun setup(button: TextView, target: SermonSortableFragment) {
		updateLabel(button, target)
		button.setOnClickListener { showPopup(button, target) }
	}

	private fun showPopup(anchor: TextView, target: SermonSortableFragment) {
		val context = anchor.context

		val container = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			background = ContextCompat.getDrawable(context, R.drawable.bg_popup_sort_menu)
		}

		val popupWindow = PopupWindow(
			container,
			LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT,
			true
		).apply {
			isOutsideTouchable = true
			isFocusable = true
			elevation = 8f
		}

		target.getSortOptions().forEach { (code, label) ->
			val row = TextView(context).apply {
				text = label
				textSize = 14f
				gravity = Gravity.START
				setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
				setTextColor(
					ContextCompat.getColor(
						context,
						if (code == target.getCurrentSortMode()) R.color.brown_primary else R.color.text_primary
					)
				)
				background =
					ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					target.setSortMode(code)
					updateLabel(anchor, target)
					popupWindow.dismiss()
				}
			}
			container.addView(row)
		}

		popupWindow.showAsDropDown(anchor, 0, 0, Gravity.END)
	}

	private fun updateLabel(button: TextView, target: SermonSortableFragment) {
		val currentLabel =
			target.getSortOptions().find { it.first == target.getCurrentSortMode() }?.second
				?: target.getSortOptions().firstOrNull()?.second ?: ""
		button.text = "$currentLabel ▾"
	}

	private fun dp(context: Context, value: Int): Int =
		(value * context.resources.displayMetrics.density).toInt()
}