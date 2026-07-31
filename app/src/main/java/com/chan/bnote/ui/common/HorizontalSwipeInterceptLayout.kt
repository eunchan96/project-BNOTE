package com.chan.bnote.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 설교·적용 탭은 서브탭(캘린더/성경별/적용)을 좌우로 스와이프해서 넘기는 ViewPager2 안에 들어있다.
 * 그런데 각 서브탭 안에서도 "캘린더(또는 장 그리드) 영역"은 좌우로 스와이프하면 이전/다음
 * 월(또는 책)로 넘어가는 게 자연스럽다 — 지금까지는 이 영역을 스와이프해도 그냥 서브탭이
 * 넘어가버렸다.
 *
 * 이 뷰로 그 영역(달력 헤더 + 그리드)을 감싸면: 가로로 뚜렷하게 드래그하는 제스처만 이 뷰가
 * 가로채서 [onSwipeLeft]/[onSwipeRight]를 호출하고(그리고 부모 ViewPager2가 같이 페이지를
 * 넘기지 않도록 `requestDisallowInterceptTouchEvent`), 탭이나 세로 스크롤 같은 나머지 제스처는
 * 평소처럼 자식 뷰(날짜 칸 클릭 등)에 그대로 전달된다.
 */
class HorizontalSwipeInterceptLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

	var onSwipeLeft: (() -> Unit)? = null
	var onSwipeRight: (() -> Unit)? = null

	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private var startX = 0f
	private var startY = 0f
	private var dragging = false
	private var handled = false

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				startX = ev.x
				startY = ev.y
				dragging = false
				handled = false
				// ViewPager2(조상 뷰)가 먼저 가로채 서브탭을 넘겨버리지 않도록, 이 영역에서
				// 터치가 시작되면 바로 막아둔다 — 우리가 직접 탭/가로 드래그/세로 스크롤을
				// 구분해서 처리한다.
				parent?.requestDisallowInterceptTouchEvent(true)
			}

			MotionEvent.ACTION_MOVE -> {
				val dx = ev.x - startX
				val dy = ev.y - startY
				if (!dragging && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
					dragging = true
					return true
				}
			}
		}
		return false
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_MOVE -> {
				val dx = event.x - startX
				if (!handled && abs(dx) > width / 4f) {
					handled = true
					if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
				}
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				dragging = false
				handled = false
			}
		}
		return true
	}
}