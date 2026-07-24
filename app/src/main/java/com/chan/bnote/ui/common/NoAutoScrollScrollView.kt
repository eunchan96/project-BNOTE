package com.chan.bnote.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

/**
 * 안드로이드 기본 ScrollView는 안에 있는 EditText 등이 포커스를 받으면, 그 뷰(박스) 전체를 화면에
 * 담으려고 위쪽으로 크게 끌어올린다. 큰 멀티라인 EditText(설교 메모 등)에서는 이게 위쪽 내용(제목 등)을
 * 필요 이상으로 화면 밖으로 밀어내서 불편하다.
 *
 * 그렇다고 자동 스크롤을 아예 꺼버리면, 포커스 받은 필드가 화면 아래쪽(키보드에 가려진 곳)에 있을 때
 * 전혀 안 끌어올려져서 오히려 안 보이는 문제가 생긴다. 그래서 "박스 전체"가 아니라 "박스의 맨 위 부분
 * 정도만" 화면에 들어오도록 최소한으로만 스크롤하도록 바꿨다 — 필요할 땐 끌어올리되, 박스가 크다고
 * 필요 이상으로 많이 스크롤하지는 않는다.
 *
 * 타이핑하면서 커서 위치를 따라가는 스크롤(requestChildRectangleOnScreen, EditText가 커서만 보이게
 * 최소한으로 스크롤하는 것)은 건드리지 않는다 — 타이핑 중엔 원래도 필요한 만큼만 스크롤되는 동작이다.
 */
class NoAutoScrollScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

	// 포커스 받은 박스가 아무리 커도, 이 높이 정도만 화면에 보이면 충분하다고 보고 그만큼만 끌어올린다.
	private val maxFocusScrollTargetDp = 120

	override fun requestChildFocus(child: View?, focused: View?) {
		val target = focused ?: return
		val maxHeightPx = (maxFocusScrollTargetDp * resources.displayMetrics.density).toInt()
		val limitedHeight = minOf(target.height, maxHeightPx)
		// 박스 전체가 아니라 위쪽 일부만 담은 사각형을 화면에 보이게 요청 → 그만큼만 스크롤된다.
		requestChildRectangleOnScreen(
			target,
			android.graphics.Rect(0, 0, target.width, limitedHeight),
			false
		)
	}
}