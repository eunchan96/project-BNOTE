package com.chan.bnote.ui.common

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window

/**
 * 오른쪽에서 펼쳐지는 메뉴 패널(성경 탭·설교 탭의 ≡ 메뉴)의 크기를 정한다.
 *
 * 예전에는 폭이 280dp로 고정이라, 화면이 좁은 폰에서는 화면을 더 많이 덮고 넓은 폰에서는 덜 덮어서
 * 기기마다 비율이 달랐다. 갤럭시 S24+(기본 화면 크기 기준 폭 384dp)에서 280dp가 차지하던 비율(약 73%)을
 * 기준으로 삼아, 어떤 폰에서든 화면 폭의 같은 비율로 열리게 한다. 태블릿·폴드를 펼친 화면처럼 아주 넓은
 * 곳에서는 메뉴가 지나치게 넓어지지 않도록 MAX_WIDTH_DP에서 멈춘다. 높이는 지금처럼 화면 위아래를 꽉 채운다.
 */
object RightPanelSize {

	/** 화면 폭 대비 메뉴 폭 */
	private const val WIDTH_RATIO = 0.78f

	/** 넓은 화면(태블릿·폴드 펼침)에서의 최대 폭 */
	private const val MAX_WIDTH_DP = 420

	fun apply(window: Window, context: Context) {
		val metrics = context.resources.displayMetrics
		val maxWidthPx = (MAX_WIDTH_DP * metrics.density).toInt()
		val widthPx = (metrics.widthPixels * WIDTH_RATIO).toInt().coerceAtMost(maxWidthPx)
		window.setGravity(Gravity.END)
		window.setLayout(widthPx, ViewGroup.LayoutParams.MATCH_PARENT)
	}
}