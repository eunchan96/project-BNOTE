package com.chan.bnote.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import com.chan.bnote.R

/**
 * 일반 ScrollView는 android:maxHeight를 지원하지 않아서, 내용이 적어도 항상 지정한 고정
 * 높이를 다 차지해버린다(메모 바텀시트에서 박스가 하나만 있어도 밑에 여백이 크게 남던 문제).
 *
 * 이 뷰는 wrap_content로 두면 내용만큼만 차지하고, 내용이 maxHeight를 넘어갈 때만 그 높이로
 * 잘리면서 스크롤이 가능해진다.
 */
class MaxHeightScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

	private var maxHeightPx: Int = Int.MAX_VALUE

	init {
		val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView)
		try {
			maxHeightPx =
				a.getDimensionPixelSize(R.styleable.MaxHeightScrollView_maxHeight, Int.MAX_VALUE)
		} finally {
			a.recycle()
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val cappedHeightSpec = if (maxHeightPx != Int.MAX_VALUE) {
			MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
		} else {
			heightMeasureSpec
		}
		super.onMeasure(widthMeasureSpec, cappedHeightSpec)
	}
}