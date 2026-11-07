package com.chan.bnote.ui.common

import android.graphics.Color
import com.chan.bnote.ui.common.HighlightColors.contrastTextColor

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

	/**
	 * 하이라이트 위에 놓이는 예수님 말씀(붉은 글씨) 색. 하이라이트 배경은 다크모드에서도 같은 파스텔이라
	 * 화면 모드가 아니라 배경 밝기를 보고 고른다 — 밝은 배경엔 짙은 붉은색(라이트모드 red_letter와 같은 색),
	 * 어두운 배경엔 밝은 붉은색. [contrastTextColor]와 같은 기준이라 일반 글자와 붉은 글씨가 같이 바뀐다.
	 */
	fun redLetterColor(colorHex: String): Int =
		if (contrastTextColor(colorHex) == Color.parseColor("#212121")) Color.parseColor("#8E3A2E")
		else Color.parseColor("#F0B3A8")
}