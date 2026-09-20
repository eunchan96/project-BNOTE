package com.chan.bnote.notification

import android.content.Context
import org.json.JSONArray

/**
 * 매일 말씀 알림에 쓸 구절 목록. 성경 전체에서 완전 무작위로 뽑으면 족보나 율법 조항처럼
 * 알림으로 보기에 뜬금없는 구절이 나올 수 있어서, assets/notification/daily_verses.json에
 * 미리 골라둔 구절(2000여 개, 절 범위 포함 가능) 중에서만 뽑는다.
 */
object CuratedVerses {

	data class VerseRef(val bookId: Int, val chapter: Int, val startVerse: Int, val endVerse: Int)

	private var cache: List<VerseRef>? = null

	fun load(context: Context): List<VerseRef> {
		cache?.let { return it }

		val json = context.assets.open("notification/daily_verses.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }

		val array = JSONArray(json)
		val list = mutableListOf<VerseRef>()
		for (i in 0 until array.length()) {
			val obj = array.getJSONObject(i)
			list.add(
				VerseRef(
					bookId = obj.getInt("book"),
					chapter = obj.getInt("chapter"),
					startVerse = obj.getInt("start_verse"),
					endVerse = obj.getInt("end_verse")
				)
			)
		}
		cache = list
		return list
	}
}