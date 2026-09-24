package com.chan.bnote.ui.sermon

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import kotlin.math.ceil

/** 캘린더/성경별 탭 상단의 정렬 버튼(예: "카테고리순 ▾")을 눌렀을 때 작은 팝업으로 정렬을 고르게 한다. */
object SortButtonHelper {

	fun setup(button: TextView, target: SermonSortableFragment) {
		reserveWidestLabelWidth(button, target)
		updateLabel(button, target)
		button.setOnClickListener { showPopup(button, target) }
	}

	/**
	 * 정렬 옵션 중 가장 긴 이름("카테고리순 ▾" 등) 기준으로 버튼의 최소 폭을 잡아둔다. 그래야 정렬을 바꿀 때마다
	 * 버튼 폭이 달라져서 머리줄(한 줄 ↔ 두 줄) 배치가 덜컥 바뀌는 일이 없다. 글꼴 크기 설정이 반영된
	 * 실제 글자 폭으로 재기 때문에 큰 글꼴에서도 맞는다.
	 */
	private fun reserveWidestLabelWidth(button: TextView, target: SermonSortableFragment) {
		val paint = button.paint
		val widest =
			target.getSortOptions().maxOfOrNull { paint.measureText("${it.second} ▾") } ?: 0f
		button.minWidth = ceil(widest).toInt() + button.paddingLeft + button.paddingRight
		button.gravity = Gravity.END or Gravity.CENTER_VERTICAL
	}

	private fun showPopup(anchor: TextView, target: SermonSortableFragment) {
		val context = anchor.context

		val container = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			background = ContextCompat.getDrawable(context, R.drawable.bg_popup_sort_menu)
			minimumWidth = dp(context, 110)
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
				setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
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

		// showAsDropDown(gravity=END)는 기대한 대로 동작하지 않아서, 화면 좌표를 직접 계산해
		// 앵커 오른쪽 끝보다 살짝 왼쪽에 딱 맞춰 띄운다.
		container.measure(
			android.view.View.MeasureSpec.makeMeasureSpec(
				0,
				android.view.View.MeasureSpec.UNSPECIFIED
			),
			android.view.View.MeasureSpec.makeMeasureSpec(
				0,
				android.view.View.MeasureSpec.UNSPECIFIED
			)
		)
		val popupWidth = container.measuredWidth

		val anchorLocation = IntArray(2)
		anchor.getLocationOnScreen(anchorLocation)

		val x = anchorLocation[0] + anchor.width - popupWidth - dp(context, 8)
		val y = anchorLocation[1] + anchor.height

		popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
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