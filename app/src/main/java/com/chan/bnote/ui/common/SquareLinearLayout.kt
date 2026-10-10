package com.chan.bnote.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * 세로 길이를 가로 길이와 같게(1:1) 맞추는 LinearLayout. 마이페이지 메뉴 카드처럼 폭은 weight로 나누고
 * 높이는 그 폭에 맞춰 정사각형으로 보여주고 싶을 때 쓴다.
 * 글꼴을 아주 크게 키워서 내용이 정사각형에 다 안 들어가면, 잘리지 않게 내용 높이만큼 더 커진다.
 */
class SquareLinearLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		if (measuredHeight < measuredWidth) {
			super.onMeasure(
				widthMeasureSpec,
				MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
			)
		}
	}
}