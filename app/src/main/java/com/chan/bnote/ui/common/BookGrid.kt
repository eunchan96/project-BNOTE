package com.chan.bnote.ui.common

import android.content.Context

/**
 * 책 이름을 그리드로 보여주는 화면들(성경 이동, 성경읽기표, 설교/적용 본문 선택 등)이 공통으로 쓰는
 * 한 줄 칸 수 결정 규칙.
 *
 * 한 줄에 4권이면 "데살로니가전서" 같은 긴 이름이 좁은 칸에서 세 줄 넘게 쪼개져서 뒷부분("전서")이
 * 잘린다. 기본 글꼴 크기의 넓은 화면에서는 그대로 4권이지만, 핸드폰 설정에서 글꼴을 키웠거나(1.3배 이상)
 * 화면이 좁으면(화면 확대로 dp 폭이 줄어든 경우 포함) 3권씩 보여서 칸을 넓힌다.
 */
object BookGrid {

	private const val DEFAULT_COLUMNS = 4
	private const val LARGE_TEXT_COLUMNS = 3
	private const val LARGE_TEXT_SCALE = 1.3f
	private const val NARROW_SCREEN_DP = 340

	fun columns(context: Context): Int {
		val resources = context.resources
		val fontScale = resources.configuration.fontScale
		val widthDp = resources.configuration.screenWidthDp
		return if (fontScale >= LARGE_TEXT_SCALE || widthDp < NARROW_SCREEN_DP) {
			LARGE_TEXT_COLUMNS
		} else {
			DEFAULT_COLUMNS
		}
	}
}