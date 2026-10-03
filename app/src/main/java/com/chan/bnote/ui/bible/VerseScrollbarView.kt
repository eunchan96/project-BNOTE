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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

/**
 * 성경 탭 오른쪽에 보이는 "지금 읽는 위치" 표시용 스크롤바를 직접 그린다.
 *
 * 안드로이드 기본 android:scrollbars는 기기/제조사 테마에 따라 끝부분에 둥근 여백(inset)이
 * 들어가 있는 경우가 많아서(특히 삼성 One UI 등), scrollbarSize/scrollbarStyle만으로는 위아래
 * 끝까지 정확히 닿는 모양을 만들 수 없다. 이 뷰는 스크롤 범위/위치를 직접 계산해서 얇은 막대를
 * 그리므로, 실제로 맨 위/맨 아래까지 스크롤했을 때 픽셀 단위로 정확히 끝에 닿는다.
 *
 * 위치 계산은 RecyclerView의 computeVerticalScroll*()를 쓰지 않는다. 그 값은 "지금 화면에 보이는
 * 절들의 평균 높이"로 전체 길이를 추정한 것이라, 짧은 절·긴 절·소제목이 섞인 성경 본문에서는 보이는
 * 절이 바뀔 때마다 평균이 흔들려서 막대가 튀었다(특히 위로 올릴 때). 대신 화면에 한 번이라도 그려진
 * 절의 실제 높이를 위치별로 기억해두고(heights), 아직 안 그려진 절만 기억해둔 높이들의 평균으로
 * 채운다. 한 번 지나간 구간은 실제 값이 되므로 오가도 막대가 흔들리지 않는다.
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

	/** 위치(어댑터 position)별로 실제로 잰 높이(px). 아직 한 번도 안 그려진 위치는 UNKNOWN. */
	private var heights = IntArray(0)

	/** 지금 heights가 어떤 어댑터 기준인지. 다른 장으로 바뀌어(어댑터 교체) 오면 기억을 비운다. */
	private var trackedAdapter: RecyclerView.Adapter<*>? = null

	/**
	 * 절이 추가·삭제되면 위치가 밀리므로 기억을 비운다. 하이라이트·메모·글자 크기처럼 내용만 바뀌는
	 * 경우(notifyDataSetChanged/notifyItemChanged)는 비우지 않는다 — 대부분 높이가 그대로라 비우면
	 * 오히려 다시 추정값으로 돌아가 막대가 튀고, 실제로 높이가 바뀐 절은 화면에 보일 때 새 값으로
	 * 덮어써진다.
	 */
	private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = clearHeights()
		override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = clearHeights()
		override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
			clearHeights()
	}

	private val fadeHandler = Handler(Looper.getMainLooper())
	private val fadeOutRunnable = Runnable {
		animate().alpha(0f).setDuration(FADE_OUT_DURATION_MS).start()
	}

	companion object {
		private const val VISIBLE_DURATION_MS = 800L
		private const val FADE_OUT_DURATION_MS = 300L
		private const val UNKNOWN = -1
	}

	/** RecyclerView의 지금 스크롤 상태를 반영해서 다시 그린다. 스크롤할 내용이 뷰 안에 다 들어가면
	 * (스크롤할 필요 자체가 없으면) 아예 숨긴다. 그 외엔 일단 보여주고, 잠시 뒤 저절로 옅어진다. */
	fun updateFrom(recyclerView: RecyclerView) {
		trackAdapter(recyclerView.adapter)

		val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
		val itemCount = recyclerView.adapter?.itemCount ?: 0
		if (layoutManager == null || itemCount == 0 || height <= 0) {
			hide()
			return
		}
		if (heights.size != itemCount) heights = IntArray(itemCount) { UNKNOWN }

		recordVisibleHeights(recyclerView, layoutManager)

		val firstPosition = layoutManager.findFirstVisibleItemPosition()
		val firstView = layoutManager.findViewByPosition(firstPosition)
		if (firstPosition == RecyclerView.NO_POSITION || firstView == null) {
			hide()
			return
		}

		// 아직 안 그려진 절은 지금까지 잰 높이들의 평균으로 채운다.
		var knownSum = 0L
		var knownCount = 0
		for (h in heights) {
			if (h != UNKNOWN) {
				knownSum += h
				knownCount++
			}
		}
		if (knownCount == 0) {
			hide()
			return
		}
		val estimatedHeight = (knownSum / knownCount).toInt()
		fun heightAt(position: Int): Int =
			heights[position].takeIf { it != UNKNOWN } ?: estimatedHeight

		var contentHeight = 0L
		var heightAboveFirst = 0L
		for (position in 0 until itemCount) {
			val h = heightAt(position)
			contentHeight += h
			if (position < firstPosition) heightAboveFirst += h
		}

		val paddingTop = recyclerView.paddingTop
		val range = paddingTop + contentHeight + recyclerView.paddingBottom
		val extent = recyclerView.height.toLong()
		if (range <= extent) {
			hide()
			return
		}

		// 첫 번째로 보이는 절이 위로 가려진 만큼(맨 위에 딱 붙어 있으면 0).
		val firstViewTop =
			layoutManager.getDecoratedTop(firstView) - layoutParamsOf(firstView).topMargin
		val offset = (heightAboveFirst + paddingTop - firstViewTop)
			.coerceIn(0L, range - extent)

		hasContentToShow = true
		val trackHeight = height.toFloat()
		thumbHeight = (trackHeight * extent / range)
			.coerceAtLeast(minThumbHeightPx)
			.coerceAtMost(trackHeight)
		val maxThumbTop = trackHeight - thumbHeight
		thumbTop = (maxThumbTop * offset / (range - extent)).coerceIn(0f, maxThumbTop)

		invalidate()
		showThenScheduleFadeOut()
	}

	/** 지금 화면에 붙어 있는 절들의 실제 높이(위아래 장식·여백 포함)를 기억해 둔다. */
	private fun recordVisibleHeights(
		recyclerView: RecyclerView,
		layoutManager: LinearLayoutManager
	) {
		for (i in 0 until recyclerView.childCount) {
			val child = recyclerView.getChildAt(i)
			val position = recyclerView.getChildAdapterPosition(child)
			if (position == RecyclerView.NO_POSITION || position >= heights.size) continue
			val lp = layoutParamsOf(child)
			heights[position] =
				layoutManager.getDecoratedMeasuredHeight(child) + lp.topMargin + lp.bottomMargin
		}
	}

	private fun layoutParamsOf(view: View): RecyclerView.LayoutParams =
		view.layoutParams as RecyclerView.LayoutParams

	/** 다른 장으로 바뀌어 어댑터가 교체됐으면, 이전 장 기준으로 잰 높이는 버리고 새 어댑터를 지켜본다. */
	private fun trackAdapter(adapter: RecyclerView.Adapter<*>?) {
		if (adapter === trackedAdapter) return
		trackedAdapter?.unregisterAdapterDataObserver(adapterObserver)
		trackedAdapter = adapter
		adapter?.registerAdapterDataObserver(adapterObserver)
		clearHeights()
	}

	private fun clearHeights() {
		heights.fill(UNKNOWN)
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
		// 어댑터에 붙여둔 감시자도 떼어낸다. 다시 화면에 붙으면 다음 updateFrom()에서 새로 붙고,
		// 그 사이 바뀌었을지 모르니 높이 기억도 그때 새로 시작한다.
		trackedAdapter?.unregisterAdapterDataObserver(adapterObserver)
		trackedAdapter = null
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