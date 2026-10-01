package com.chan.bnote.ui.bible

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * 성경 탭 화면의 최상위 레이아웃. 화면 어디를 누르든 "눌렀다"는 사실(ACTION_DOWN)만 먼저
 * 알려주고, 터치 자체는 가로채지 않고 원래대로 아래 뷰(본문 스크롤·절 선택 등)에 그대로 넘긴다.
 *
 * 재생 툴바가 열려 있을 때 툴바 바깥을 누르면 툴바를 닫는 용도로 쓴다(BibleFragment) — 닫는 것과
 * 동시에 누른 동작(스크롤, 절 탭 등)도 그대로 실행되므로, 닫기 위해 한 번 더 누를 필요가 없다.
 */
class TouchObservingFrameLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	var onTouchDown: ((MotionEvent) -> Unit)? = null

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		if (ev.actionMasked == MotionEvent.ACTION_DOWN) onTouchDown?.invoke(ev)
		return super.dispatchTouchEvent(ev)
	}
}