package com.chan.bnote.ui.bible

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

/**
 * 성경 탭 오른쪽에 보이는 "지금 읽는 위치" 표시용 스크롤바를 직접 그린다.
 *
 * 안드로이드 기본 android:scrollbars는 기기/제조사 테마에 따라 끝부분에 둥근 여백(inset)이
 * 들어가 있는 경우가 많아서(특히 삼성 One UI 등), scrollbarSize/scrollbarStyle만으로는 위아래
 * 끝까지 정확히 닿는 모양을 만들 수 없다. 이 뷰는 RecyclerView의 스크롤 범위/위치를 직접 계산해서
 * 얇은 막대를 그리므로, 실제로 맨 위/맨 아래까지 스크롤했을 때 픽셀 단위로 정확히 끝에 닿는다.
 *
 * 원래 시스템 스크롤바처럼, 스크롤이 일어날 때만 보이고 잠시 멈춰 있으면 서서히 사라진다.
 */
class VerseScrollbarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : View(context, attrs) {

	private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = ContextCompat.getColor(context, R.color.verse_scrollbar_thumb)
	}
	private val thumbWidthPx = 3 * resources.displayMetrics.density
	private val minThumbHeightPx = 24 * resources.displayMetrics.density

	private var thumbTop = 0f
	private var thumbHeight = 0f
	private var hasContentToShow = false

	private val fadeHandler = Handler(Looper.getMainLooper())
	private val fadeOutRunnable = Runnable {
		animate().alpha(0f).setDuration(FADE_OUT_DURATION_MS).start()
	}

	companion object {
		private const val VISIBLE_DURATION_MS = 800L
		private const val FADE_OUT_DURATION_MS = 300L
	}

	/** RecyclerView의 지금 스크롤 상태를 반영해서 다시 그린다. 스크롤할 내용이 뷰 안에 다 들어가면
	 * (스크롤할 필요 자체가 없으면) 아예 숨긴다. 그 외엔 일단 보여주고, 잠시 뒤 저절로 옅어진다. */
	fun updateFrom(recyclerView: RecyclerView) {
		val range = recyclerView.computeVerticalScrollRange()
		val extent = recyclerView.computeVerticalScrollExtent()
		val offset = recyclerView.computeVerticalScrollOffset()

		if (range <= extent || height <= 0) {
			hide()
			return
		}
		hasContentToShow = true

		val trackHeight = height.toFloat()
		val rawThumbHeight = trackHeight * extent / range
		thumbHeight = rawThumbHeight.coerceAtLeast(minThumbHeightPx).coerceAtMost(trackHeight)

		val maxOffset = (range - extent).coerceAtLeast(1)
		val maxThumbTop = trackHeight - thumbHeight
		thumbTop = (maxThumbTop * offset / maxOffset).coerceIn(0f, maxThumbTop)

		invalidate()
		showThenScheduleFadeOut()
	}

	private fun hide() {
		hasContentToShow = false
		fadeHandler.removeCallbacks(fadeOutRunnable)
		alpha = 0f
	}

	/** 지금 바로 보이게 하고(혹시 페이드아웃 중이었다면 멈추고), 잠시 뒤 다시 옅어지도록 예약한다.
	 * 스크롤이 계속되는 동안엔 매번 이 예약이 새로 걸리므로, 실제로는 스크롤이 멈추고 나서야
	 * VISIBLE_DURATION_MS만큼 지난 뒤에 사라진다. */
	private fun showThenScheduleFadeOut() {
		fadeHandler.removeCallbacks(fadeOutRunnable)
		animate().cancel()
		alpha = 1f
		fadeHandler.postDelayed(fadeOutRunnable, VISIBLE_DURATION_MS)
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		fadeHandler.removeCallbacks(fadeOutRunnable)
	}

	/** 뷰홀더가 재활용돼서 다른 장으로 바뀔 때 호출한다. 예약해둔 페이드아웃 타이머가 남아있으면
	 * 새 장의 스크롤바가 뜬금없이 옅어져 버릴 수 있어서, 재활용 시점에 확실히 지워준다. */
	fun cancelPendingFade() {
		fadeHandler.removeCallbacks(fadeOutRunnable)
		animate().cancel()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (!hasContentToShow) return
		val left = width - thumbWidthPx
		canvas.drawRoundRect(
			RectF(left, thumbTop, width.toFloat(), thumbTop + thumbHeight),
			thumbWidthPx / 2,
			thumbWidthPx / 2,
			thumbPaint
		)
	}
}