package com.chan.bnote.data.bible.hymn

import android.content.Context
import org.json.JSONObject

object HymnSeeder {

	suspend fun seedIfNeeded(context: Context, dao: HymnDao) {
		if (dao.count() > 0) return

		val json = context.assets.open("hymns/hymns.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val root = JSONObject(json)

		// 1) 대분류 삽입, JSON id -> 실제 DB id 매핑 생성
		val majorIdMap = HashMap<Long, Long>()
		val majorArray = root.getJSONArray("majorCategories")
		for (i in 0 until majorArray.length()) {
			val obj = majorArray.getJSONObject(i)
			val jsonId = obj.getLong("id")
			val realId = dao.insertCategory(
				HymnCategory(
					name = obj.getString("name"),
					parentId = null,
					sortOrder = obj.getInt("sortOrder")
				)
			)
			majorIdMap[jsonId] = realId
		}

		// 2) 소분류 삽입 (majorId는 위에서 만든 매핑으로 실제 id로 치환), JSON id -> 실제 DB id 매핑 생성
		val minorIdMap = HashMap<Long, Long>()
		val minorArray = root.getJSONArray("minorCategories")
		for (i in 0 until minorArray.length()) {
			val obj = minorArray.getJSONObject(i)
			val jsonId = obj.getLong("id")
			val jsonMajorId = obj.getLong("majorId")
			val realMajorId = majorIdMap[jsonMajorId]
				?: error("알 수 없는 대분류 id 참조: $jsonMajorId")
			val realId = dao.insertCategory(
				HymnCategory(
					name = obj.getString("name"),
					parentId = realMajorId,
					sortOrder = obj.getInt("sortOrder")
				)
			)
			minorIdMap[jsonId] = realId
		}

		// 3) 찬송 삽입 (categoryId도 실제 소분류 id로 치환)
		val hymnArray = root.getJSONArray("hymns")
		val hymns = (0 until hymnArray.length()).map { i ->
			val obj = hymnArray.getJSONObject(i)
			val jsonCategoryId = obj.getLong("categoryId")
			val realCategoryId = minorIdMap[jsonCategoryId]
				?: error("알 수 없는 소분류 id 참조: $jsonCategoryId")
			Hymn(
				number = obj.getInt("number"),
				title = obj.getString("title"),
				categoryId = realCategoryId,
				imageFileName = obj.getString("image"),
				youtubeSongUrl = obj.getString("youtubeSong"),
				youtubeMrUrl = obj.getString("youtubeMr")
			)
		}
		dao.insertHymns(hymns)
	}
}