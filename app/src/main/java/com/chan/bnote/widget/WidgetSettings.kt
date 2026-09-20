package com.chan.bnote.widget

import android.content.Context
import com.chan.bnote.widget.WidgetSettings.clear

/**
 * 위젯별 설정(테마, 배경 투명도, 암송 위젯이 보여줄 그룹, 넘긴 횟수). 같은 위젯을 여러 개 올려도 서로 다른
 * 설정을 가질 수 있도록 위젯 id를 키에 붙여서 저장한다. 위젯을 지우면 [clear]로 함께 지운다.
 *
 * 앱 설정(AppSettings)과 달리 이 기기의 홈 화면 배치에 묶인 값이라 데이터 내보내기/불러오기
 * 대상이 아니다.
 */
object WidgetSettings {

	private const val PREF_NAME = "bnote_widgets"

	/** 암송 위젯이 보여줄 그룹. id가 바뀌어도(데이터 불러오기 등) 이름으로 다시 찾을 수 있게 이름도 함께 둔다. */
	data class GroupChoice(val id: Long, val name: String)

	private fun prefs(context: Context) =
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	fun getTheme(context: Context, appWidgetId: Int): WidgetTheme =
		WidgetTheme.fromCode(prefs(context).getString("theme_$appWidgetId", null))

	fun setTheme(context: Context, appWidgetId: Int, theme: WidgetTheme) {
		prefs(context).edit().putString("theme_$appWidgetId", theme.code).apply()
	}

	/** 위젯 배경의 투명도(%). 0이면 완전 불투명, 100이면 완전 투명. 글자에는 적용되지 않는다. */
	fun getTransparency(context: Context, appWidgetId: Int): Int =
		prefs(context).getInt("transparency_$appWidgetId", 0)

	fun setTransparency(context: Context, appWidgetId: Int, percent: Int) {
		prefs(context).edit()
			.putInt("transparency_$appWidgetId", percent.coerceIn(0, 100))
			.apply()
	}

	/** 고른 암송 그룹. "전체 그룹"이면 null. */
	fun getGroup(context: Context, appWidgetId: Int): GroupChoice? {
		val prefs = prefs(context)
		val id = prefs.getLong("group_id_$appWidgetId", -1L)
		val name = prefs.getString("group_name_$appWidgetId", null)
		return if (id == -1L || name == null) null else GroupChoice(id, name)
	}

	fun setGroup(context: Context, appWidgetId: Int, group: GroupChoice?) {
		val editor = prefs(context).edit()
		if (group == null) {
			editor.remove("group_id_$appWidgetId").remove("group_name_$appWidgetId")
		} else {
			editor.putLong("group_id_$appWidgetId", group.id)
				.putString("group_name_$appWidgetId", group.name)
		}
		editor.apply()
	}

	/** 암송 위젯에서 "다음"을 누른 횟수. 오늘 기준 구절 위치에 더해서 보여줄 구절을 정한다. */
	fun getOffset(context: Context, appWidgetId: Int): Int =
		prefs(context).getInt("offset_$appWidgetId", 0)

	fun setOffset(context: Context, appWidgetId: Int, offset: Int) {
		prefs(context).edit().putInt("offset_$appWidgetId", offset).apply()
	}

	fun clear(context: Context, appWidgetIds: IntArray) {
		val editor = prefs(context).edit()
		for (id in appWidgetIds) {
			editor.remove("theme_$id")
				.remove("transparency_$id")
				.remove("group_id_$id")
				.remove("group_name_$id")
				.remove("offset_$id")
		}
		editor.apply()
	}
}