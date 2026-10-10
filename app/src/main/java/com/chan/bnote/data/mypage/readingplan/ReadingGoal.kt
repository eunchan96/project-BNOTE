package com.chan.bnote.data.mypage.readingplan

import android.content.Context
import com.chan.bnote.data.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * 성경읽기표의 "읽기 목표" — 어떤 책을(범위), 언제부터 언제까지(기간) 읽을지.
 * 읽기표의 진행률·하루 분량·계획 대비 계산이 이 목표를 기준으로 한다(읽음 기록 자체는 그대로).
 *
 * @param bookIds 읽을 책(1~66). 66권 전부면 "전체".
 * @param startMillis 시작일(그날 0시).
 * @param endMillis 종료일(그날 0시, 이날까지 포함).
 */
data class ReadingGoal(
	val bookIds: Set<Int>,
	val startMillis: Long,
	val endMillis: Long
) {
	val isAllBooks: Boolean get() = bookIds.containsAll(ReadingGoalStore.ALL_BOOKS)
}

/**
 * 읽기 목표 저장소(SharedPreferences — DB 테이블이 아니라서 migration이 필요 없다).
 *
 * 따로 정하지 않은 값은 저장하지 않고 기본값으로 계산한다: 책은 66권 전체, 기간은 "올해 1월 1일 ~ 12월 31일".
 * 그래서 한 번도 목표를 안 바꾼 사용자는 해가 바뀌면 기간도 자동으로 새해 기준이 된다(예전 읽기표와 같은 동작).
 */
object ReadingGoalStore {
	private const val PREF_NAME = "reading_goal"
	private const val KEY_BOOK_IDS = "book_ids" // 쉼표로 이은 책 번호, 없으면 전체
	private const val KEY_START = "start_millis" // 없으면 올해 1월 1일
	private const val KEY_END = "end_millis" // 없으면 올해 12월 31일

	val ALL_BOOKS: Set<Int> = (1..66).toSet()
	val OLD_TESTAMENT: Set<Int> = (1..39).toSet()
	val NEW_TESTAMENT: Set<Int> = (40..66).toSet()

	private fun prefs(context: Context) =
		context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	fun thisYearStart(): Long {
		val cal = Calendar.getInstance()
		cal.set(cal.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0)
		return DateUtils.normalizeToDayStart(cal.timeInMillis)
	}

	fun thisYearEnd(): Long {
		val cal = Calendar.getInstance()
		cal.set(cal.get(Calendar.YEAR), Calendar.DECEMBER, 31, 0, 0, 0)
		return DateUtils.normalizeToDayStart(cal.timeInMillis)
	}

	fun defaultGoal(): ReadingGoal = ReadingGoal(ALL_BOOKS, thisYearStart(), thisYearEnd())

	fun load(context: Context): ReadingGoal {
		val p = prefs(context)
		val books = p.getString(KEY_BOOK_IDS, null)
			?.split(",")
			?.mapNotNull { it.trim().toIntOrNull() }
			?.filter { it in 1..66 }
			?.toSet()
			?.takeIf { it.isNotEmpty() }
			?: ALL_BOOKS
		val start = if (p.contains(KEY_START)) p.getLong(KEY_START, 0L) else thisYearStart()
		val end = if (p.contains(KEY_END)) p.getLong(KEY_END, 0L) else thisYearEnd()
		return ReadingGoal(books, start, end)
	}

	/** 기본값과 같은 항목은 저장하지 않는다(그래야 기본 기간이 해마다 자동으로 새해를 따라간다). */
	fun save(context: Context, goal: ReadingGoal) {
		val editor = prefs(context).edit()
		if (goal.isAllBooks) {
			editor.remove(KEY_BOOK_IDS)
		} else {
			editor.putString(KEY_BOOK_IDS, goal.bookIds.sorted().joinToString(","))
		}
		if (goal.startMillis == thisYearStart()) editor.remove(KEY_START)
		else editor.putLong(KEY_START, goal.startMillis)
		if (goal.endMillis == thisYearEnd()) editor.remove(KEY_END)
		else editor.putLong(KEY_END, goal.endMillis)
		editor.apply()
	}

	fun clear(context: Context) {
		prefs(context).edit().clear().apply()
	}

	// --- 데이터 내보내기/불러오기 ---

	/** 백업 JSON에 넣을 값. 직접 정한 항목만 들어간다(하나도 없으면 빈 객체). */
	fun toBackupJson(context: Context): JSONObject {
		val p = prefs(context)
		return JSONObject().apply {
			p.getString(KEY_BOOK_IDS, null)?.let { ids ->
				put("bookIds", JSONArray(ids.split(",").mapNotNull { it.trim().toIntOrNull() }))
			}
			if (p.contains(KEY_START)) put("startMillis", p.getLong(KEY_START, 0L))
			if (p.contains(KEY_END)) put("endMillis", p.getLong(KEY_END, 0L))
		}
	}

	/**
	 * 백업에서 읽기 목표를 되살린다. 이 키가 없는 예전 백업이면 [obj]가 null이라, 지금 설정을 그대로 둔다.
	 * 키가 있으면 백업 내용으로 통째로 바꾼다(빠진 항목은 기본값).
	 */
	fun restoreFromBackupJson(context: Context, obj: JSONObject?) {
		if (obj == null) return
		val editor = prefs(context).edit().clear()
		obj.optJSONArray("bookIds")?.let { arr ->
			val ids = (0 until arr.length()).map { arr.getInt(it) }.filter { it in 1..66 }
			if (ids.isNotEmpty() && ids.size < 66) editor.putString(
				KEY_BOOK_IDS,
				ids.joinToString(",")
			)
		}
		if (obj.has("startMillis")) editor.putLong(KEY_START, obj.getLong("startMillis"))
		if (obj.has("endMillis")) editor.putLong(KEY_END, obj.getLong("endMillis"))
		editor.apply()
	}
}