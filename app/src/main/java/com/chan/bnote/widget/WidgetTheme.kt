package com.chan.bnote.widget

import com.chan.bnote.R

/**
 * 홈 화면 위젯의 라이트/다크 테마. 앱 안의 다크모드 설정과는 별개로, 위젯마다 따로 고른다.
 *
 * RemoteViews는 앱이 아니라 시스템(런처)이 그리기 때문에 앱의 다크모드 토글(AppCompatDelegate)을
 * 따라가지 못하고, 리소스 한정자(values-night)도 앱 설정이 아닌 시스템 설정을 기준으로 고른다.
 * 그래서 위젯 색은 values-night와 무관한 전용 색(widget_*)으로 따로 정의해두고 여기서 골라 쓴다.
 */
enum class WidgetTheme(
	val code: String,
	val backgroundRes: Int,
	val primaryTextRes: Int,
	val secondaryTextRes: Int,
	val accentRes: Int,
	val pillRes: Int
) {
	LIGHT(
		"LIGHT",
		R.drawable.bg_widget_light,
		R.color.widget_light_text_primary,
		R.color.widget_light_text_secondary,
		R.color.widget_light_accent,
		R.drawable.bg_widget_pill_light
	),
	DARK(
		"DARK",
		R.drawable.bg_widget_dark,
		R.color.widget_dark_text_primary,
		R.color.widget_dark_text_secondary,
		R.color.widget_dark_accent,
		R.drawable.bg_widget_pill_dark
	);

	companion object {
		fun fromCode(code: String?): WidgetTheme = values().firstOrNull { it.code == code } ?: LIGHT
	}
}