package com.chan.bnote.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/**
 * 코드로 만드는 목록 줄(하이라이트 · 메모 모아보기 등)에 쓰는 공통 모양.
 * RecyclerView 목록은 [InsetDividerDecoration]을, LinearLayout에 줄을 직접 붙이는 화면은 이걸 쓴다.
 */
object ListRows {

	/** 눌렀을 때 물결이 퍼지는 기본 배경(예전 android.R.drawable.list_selector_background는 회색 사각형이 깜빡였다). */
	fun rippleBackground(context: Context): Drawable? {
		val value = TypedValue()
		context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
		return ContextCompat.getDrawable(context, value.resourceId)
	}

	/** 줄 사이의 얇은 구분선. 양옆을 [insetDp]만큼 띄운다(알림 목록 · 구절 목록과 같은 모양). */
	fun insetDivider(context: Context, insetDp: Int = 16): View {
		val density = context.resources.displayMetrics.density
		return View(context).apply {
			setBackgroundColor(ContextCompat.getColor(context, R.color.divider_light))
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt().coerceAtLeast(1)
			).apply {
				marginStart = (insetDp * density).toInt()
				marginEnd = (insetDp * density).toInt()
			}
		}
	}
}