package com.chan.bnote.ui.common

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/** "성경 구절 추가" 버튼처럼, 구절이 하나도 없을 때는 박스가 있는 전체 너비 버튼으로 크게
 * 보여주고 이미 하나라도 있을 때는 박스 없이 작은 글자 링크로 줄이는 스타일을, 약속의 말씀 ·
 * 암송 구절 편집 화면 둘 다에서 똑같이 쓴다. */
object RefBoxStyle {

	fun applyAddButtonStyle(context: Context, addBtn: TextView, hasAnyRef: Boolean) {
		val params = addBtn.layoutParams as LinearLayout.LayoutParams
		if (!hasAnyRef) {
			params.width = LinearLayout.LayoutParams.MATCH_PARENT
			params.gravity = Gravity.NO_GRAVITY
			addBtn.layoutParams = params
			addBtn.background = ContextCompat.getDrawable(context, R.drawable.bg_book_button)
			addBtn.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
			addBtn.textSize = 15f
		} else {
			params.width = LinearLayout.LayoutParams.WRAP_CONTENT
			params.gravity = Gravity.START
			addBtn.layoutParams = params
			addBtn.background = null
			addBtn.setPadding(dp(context, 4), dp(context, 6), dp(context, 4), dp(context, 6))
			addBtn.textSize = 14f
		}
	}

	private fun dp(context: Context, value: Int): Int =
		(value * context.resources.displayMetrics.density).toInt()
}