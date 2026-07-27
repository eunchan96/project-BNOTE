package com.chan.bnote.ui.common

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * 안드로이드 기본 ScrollView는 안에 있는 EditText 등이 포커스를 받으면, 그 뷰(박스) 전체를 화면에
 * 담으려고 위쪽으로 크게 끌어올린다. 큰 멀티라인 EditText(설교 메모 등)에서는 이게 위쪽 내용(제목 등)을
 * 필요 이상으로 화면 밖으로 밀어내서 불편하다.
 *
 * (이전 시도) requestChildFocus를 통째로 오버라이드해서 super를 안 불렀더니, 포커스가 프레임워크에
 * 제대로 전파가 안 돼서 키보드(IME)가 이상하게 동작하는(엔터/백스페이스 안 먹는) 부작용이 생겼다.
 * 포커스 전파는 반드시 정상적으로 이뤄져야 한다.
 *
 * 그래서 방식을 바꿨다: requestChildFocus는 건드리지 않고(포커스 전파 정상), 스크롤이 "얼마나"
 * 움직일지 계산하는 computeScrollDeltaToGetChildRectOnScreen만 가로채서, 포커스된 박스가 아무리 커도
 * 위쪽 일부만 화면에 들어오면 되는 걸로 계산하게 만들었다. 이러면 스크롤 자체(과하게 끌어올리는 것)만
 * 줄어들고, 포커스/키보드 동작은 원래 그대로 정상 작동한다.
 */
class NoAutoScrollScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

	// 포커스 받은 박스가 아무리 커도, 이 높이 정도만 화면에 보이면 충분하다고 보고 그만큼만 끌어올린다.
	private val maxFocusScrollTargetDp = 120

	override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int {
		if (rect == null) return super.computeScrollDeltaToGetChildRectOnScreen(rect)

		val maxHeightPx = (maxFocusScrollTargetDp * resources.displayMetrics.density).toInt()
		if (rect.height() <= maxHeightPx) {
			return super.computeScrollDeltaToGetChildRectOnScreen(rect)
		}

		// 사각형(포커스된 박스)이 너무 크면, 위쪽 일부만 담은 사각형으로 줄여서 그만큼만 스크롤되게 한다.
		val limited = Rect(rect.left, rect.top, rect.right, rect.top + maxHeightPx)
		return super.computeScrollDeltaToGetChildRectOnScreen(limited)
	}
}