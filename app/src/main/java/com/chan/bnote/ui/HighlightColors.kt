package com.chan.bnote.ui

import android.graphics.Color

object HighlightColors {
	val palette = listOf(
		"#FFF9C4", // 파스텔 옐로우
		"#FFE0B2", // 파스텔 오렌지
		"#FFCCBC", // 파스텔 코랄
		"#F8BBD0", // 파스텔 핑크
		"#E1BEE7", // 파스텔 퍼플
		"#C5CAE9", // 파스텔 인디고
		"#B3E5FC", // 파스텔 블루
		"#B2DFDB", // 파스텔 틸
		"#C8E6C9", // 파스텔 그린
		"#F0F4C3"  // 파스텔 라임
	)

	// 배경 밝기에 따라 검정/흰 글자색 자동 선택
	fun contrastTextColor(colorHex: String): Int {
		val color = try {
			Color.parseColor(colorHex)
		} catch (e: Exception) {
			Color.YELLOW
		}
		val r = Color.red(color)
		val g = Color.green(color)
		val b = Color.blue(color)
		val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
		return if (luminance > 0.6) Color.parseColor("#212121") else Color.parseColor("#F5F5F5")
	}
}