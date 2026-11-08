package com.chan.bnote.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

/**
 * 목록 줄 사이의 얇은 구분선. 양옆을 [insetDp]만큼 띄워서(알림 목록과 같은 모양) 줄이 덜 답답해 보이게 한다.
 * 마지막 줄 아래에는 그리지 않는다. 스크랩 · 북마크 · 암송 구절 · 검색 결과 · 약속의 말씀 목록이 같이 쓴다.
 */
class InsetDividerDecoration(context: Context, insetDp: Int = 16) : RecyclerView.ItemDecoration() {

	private val density = context.resources.displayMetrics.density
	private val insetPx = (insetDp * density).toInt()
	private val heightPx = (1 * density).coerceAtLeast(1f)
	private val paint = Paint().apply {
		color = ContextCompat.getColor(context, R.color.divider_light)
		style = Paint.Style.FILL
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		val lastPosition = (parent.adapter?.itemCount ?: 0) - 1
		val left = parent.paddingLeft + insetPx.toFloat()
		val right = parent.width - parent.paddingRight - insetPx.toFloat()
		for (i in 0 until parent.childCount) {
			val child: View = parent.getChildAt(i)
			val position = parent.getChildAdapterPosition(child)
			if (position == RecyclerView.NO_POSITION || position >= lastPosition) continue
			val bottom = child.bottom + child.translationY
			canvas.drawRect(left, bottom - heightPx, right, bottom, paint)
		}
	}
}