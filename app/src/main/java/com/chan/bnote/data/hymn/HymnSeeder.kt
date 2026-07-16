package com.chan.bnote.data.hymn

import android.content.Context
import org.json.JSONObject

/**
 * assets/hymns/hymns.json 을 읽어서 DB가 비어있을 때만 채워 넣는다.
 * (성경 데이터처럼 앱 실행마다 다시 읽지 않고, 최초 1회만 시딩)
 */
object HymnSeeder {

	suspend fun seedIfNeeded(context: Context, dao: HymnDao) {
		if (dao.count() > 0) return

		val json = context.assets.open("hymns/hymns.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val root = JSONObject(json)

		val majorArray = root.getJSONArray("majorCategories")
		val majorCategories = (0 until majorArray.length()).map { i ->
			val obj = majorArray.getJSONObject(i)
			HymnCategory(
				id = obj.getLong("id"),
				name = obj.getString("name"),
				parentId = null,
				sortOrder = obj.getInt("sortOrder")
			)
		}

		val minorArray = root.getJSONArray("minorCategories")
		val minorCategories = (0 until minorArray.length()).map { i ->
			val obj = minorArray.getJSONObject(i)
			HymnCategory(
				id = obj.getLong("id"),
				name = obj.getString("name"),
				parentId = obj.getLong("majorId"),
				sortOrder = obj.getInt("sortOrder")
			)
		}

		dao.insertCategories(majorCategories + minorCategories)

		val hymnArray = root.getJSONArray("hymns")
		val hymns = (0 until hymnArray.length()).map { i ->
			val obj = hymnArray.getJSONObject(i)
			Hymn(
				number = obj.getInt("number"),
				title = obj.getString("title"),
				categoryId = obj.getLong("categoryId"),
				imageFileName = obj.getString("image"),
				youtubeSongUrl = obj.getString("youtubeSong"),
				youtubeMrUrl = obj.getString("youtubeMr")
			)
		}
		dao.insertHymns(hymns)
	}
}