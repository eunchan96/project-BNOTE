package com.chan.bnote.ui.common

import android.view.View

/**
 * 사진 뷰어 위에 떠 있는 버튼들(뒤로가기·카운터)을 삼성 갤러리처럼 사진을 한 번 탭하면 숨기고,
 * 다시 탭하면 보이게 한다. 사라지고 나타날 때는 짧게 서서히(페이드) 바뀐다.
 *
 * 숨길 때는 다 사라진 뒤 GONE으로 바꿔서, 안 보이는 버튼이 실수로 눌리지 않게 한다.
 * [controls]에는 지금 보여줘야 하는 것만 넣는다(예: 사진이 한 장이면 카운터는 빼고 넘긴다).
 */
class ViewerControlsToggle(private val controls: List<View>) {

	companion object {
		private const val FADE_DURATION_MS = 150L
	}

	var isShown = true
		private set

	fun toggle() {
		if (isShown) hide() else show()
	}

	private fun show() {
		isShown = true
		controls.forEach { view ->
			view.animate().cancel()
			view.alpha = 0f
			view.visibility = View.VISIBLE
			view.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
		}
	}

	private fun hide() {
		isShown = false
		controls.forEach { view ->
			view.animate().cancel()
			view.animate().alpha(0f).setDuration(FADE_DURATION_MS)
				.withEndAction { if (!isShown) view.visibility = View.GONE }
				.start()
		}
	}
}