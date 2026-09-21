package com.chan.bnote.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlin.math.max

/**
 * 캘린더 상단처럼 "왼쪽 버튼 · 가운데 ◀ 월 ▶ · 오른쪽 버튼"이 한 줄로 놓이는 머리줄용 레이아웃.
 *
 * 예전에는 왼쪽/오른쪽 칸을 layout_weight로 똑같이 나눠 가졌는데, 글꼴을 키우면 가운데 ◀ 월 ▶가
 * 커지는 만큼 양옆 칸이 줄어들어서 "카테고리순 ▾" 같은 버튼 글자가 두 줄로 쪼개지거나 잘렸다.
 * 이 레이아웃은 버튼을 절대 쪼개지 않고, 아래 순서대로 자리를 잡는다.
 *
 *  1. 세 개가 한 줄에 다 들어가면 한 줄로 둔다. 가운데는 화면 정중앙에 두되, 양옆 버튼과 겹치게
 *     되면 그만큼만 옆으로 비켜준다.
 *  2. 한 줄에 다 안 들어가면(글꼴이 아주 크거나 화면이 좁을 때) 가운데 ◀ 월 ▶를 윗줄에 두고,
 *     왼쪽/오른쪽 버튼은 그 아랫줄의 양 끝에 둔다.
 *
 * 자식은 layout_gravity로 역할을 정한다: start = 왼쪽 버튼, end = 오른쪽 버튼, 그 외(center_horizontal
 * 또는 지정 안 함) = 가운데. 각각 하나씩만 둔다. 세로 위치는 각 줄 안에서 항상 가운데 맞춤이다.
 */
class ResponsiveHeaderLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private var startChild: View? = null
	private var centerChild: View? = null
	private var endChild: View? = null

	// 측정 결과: 한 줄에 다 안 들어가서 두 줄(또는 세 줄)로 쌓았는가
	private var stacked = false

	// 쌓았을 때 왼쪽/오른쪽 버튼도 서로 한 줄에 못 들어가면 각각 다른 줄에 둔다.
	private var splitBottomRows = false

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = MeasureSpec.getSize(widthMeasureSpec)
		startChild = null
		centerChild = null
		endChild = null

		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child.visibility == GONE) continue
			val lp = child.layoutParams as LayoutParams
			when (lp.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
				Gravity.LEFT -> startChild = child
				Gravity.RIGHT -> endChild = child
				else -> centerChild = child
			}
			// 자식은 항상 "내용만큼"의 크기로 재고(한 줄에 다 안 들어가도 폭을 줄여 쪼개지 않는다),
			// 쌓을지 말지는 아래에서 그 크기를 보고 정한다.
			child.measure(
				MeasureSpec.makeMeasureSpec(
					(width - lp.leftMargin - lp.rightMargin).coerceAtLeast(0), MeasureSpec.AT_MOST
				),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
			)
		}

		val startW = startChild?.let(::outerWidth) ?: 0
		val centerW = centerChild?.let(::outerWidth) ?: 0
		val endW = endChild?.let(::outerWidth) ?: 0
		val startH = startChild?.let(::outerHeight) ?: 0
		val centerH = centerChild?.let(::outerHeight) ?: 0
		val endH = endChild?.let(::outerHeight) ?: 0

		stacked = startW + centerW + endW > width
		splitBottomRows = stacked && startW + endW > width

		val contentHeight = when {
			!stacked -> max(startH, max(centerH, endH))
			splitBottomRows -> centerH + startH + endH
			else -> centerH + max(startH, endH)
		}
		setMeasuredDimension(
			resolveSize(width, widthMeasureSpec),
			resolveSize(contentHeight + paddingTop + paddingBottom, heightMeasureSpec)
		)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		val width = right - left
		val start = startChild
		val center = centerChild
		val end = endChild
		val startW = start?.let(::outerWidth) ?: 0
		val centerW = center?.let(::outerWidth) ?: 0
		val endW = end?.let(::outerWidth) ?: 0
		val startH = start?.let(::outerHeight) ?: 0
		val centerH = center?.let(::outerHeight) ?: 0
		val endH = end?.let(::outerHeight) ?: 0

		if (!stacked) {
			val rowHeight = max(startH, max(centerH, endH))
			start?.let { place(it, 0, paddingTop, rowHeight) }
			end?.let { place(it, width - endW, paddingTop, rowHeight) }
			center?.let {
				// 정중앙에 두되, 양옆 버튼과 겹치지 않는 범위 안으로만 움직인다.
				val centered = (width - centerW) / 2
				val x = centered.coerceIn(startW, (width - endW - centerW).coerceAtLeast(startW))
				place(it, x, paddingTop, rowHeight)
			}
			return
		}

		// 윗줄: 가운데 ◀ 월 ▶
		center?.let { place(it, ((width - centerW) / 2).coerceAtLeast(0), paddingTop, centerH) }

		// 아랫줄: 왼쪽 버튼은 왼쪽 끝, 오른쪽 버튼은 오른쪽 끝
		val rowTop = paddingTop + centerH
		if (!splitBottomRows) {
			val rowHeight = max(startH, endH)
			start?.let { place(it, 0, rowTop, rowHeight) }
			end?.let { place(it, width - endW, rowTop, rowHeight) }
		} else {
			start?.let { place(it, 0, rowTop, startH) }
			end?.let { place(it, width - endW, rowTop + startH, endH) }
		}
	}

	/** 자식(여백 포함)의 왼쪽 위를 ([outerLeft], [rowTop])에 두고, 줄 높이 안에서 세로 가운데로 맞춘다. */
	private fun place(child: View, outerLeft: Int, rowTop: Int, rowHeight: Int) {
		val lp = child.layoutParams as LayoutParams
		val childLeft = outerLeft + lp.leftMargin
		val childTop = rowTop + (rowHeight - outerHeight(child)) / 2 + lp.topMargin
		child.layout(
			childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight
		)
	}

	private fun outerWidth(child: View): Int {
		val lp = child.layoutParams as LayoutParams
		return child.measuredWidth + lp.leftMargin + lp.rightMargin
	}

	private fun outerHeight(child: View): Int {
		val lp = child.layoutParams as LayoutParams
		return child.measuredHeight + lp.topMargin + lp.bottomMargin
	}
}