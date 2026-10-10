package com.chan.bnote.ui.sermon.addsermon

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.chan.bnote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 글자 색을 고르는 작은 창. 설교 노트 메모와 적용 노트(묵상하기 · 기도하기 · 적용하기)에서 같이 쓴다.
 * 색을 누르면 [onPicked]로 그 색을 넘기고 창을 닫는다.
 */
object RichTextColorPicker {

	private val COLORS = listOf(
		"#000000" to "검정", "#795548" to "브라운", "#E53935" to "빨강",
		"#1E88E5" to "파랑", "#43A047" to "초록", "#FB8C00" to "주황"
	)

	fun show(context: Context, onPicked: (Int) -> Unit) {
		val density = context.resources.displayMetrics.density
		fun dp(value: Int) = (value * density).toInt()

		val row = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding(dp(16), dp(8), dp(16), dp(8))
		}
		lateinit var dialog: AlertDialog
		for ((hex, name) in COLORS) {
			row.addView(View(context).apply {
				contentDescription = name
				background = GradientDrawable().apply {
					shape = GradientDrawable.OVAL
					setColor(Color.parseColor(hex))
				}
				layoutParams =
					LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) }
				isClickable = true
				isFocusable = true
				setOnClickListener {
					onPicked(Color.parseColor(hex))
					dialog.dismiss()
				}
			})
		}

		dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("글자 색")
			.setView(row)
			.setNegativeButton("취소", null)
			.show()
	}
}