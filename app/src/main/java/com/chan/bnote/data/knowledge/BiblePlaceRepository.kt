package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** 성경 지명사전 데이터. JSON 에셋에서 읽어와 메모리에 캐시한다. */
object BiblePlaceRepository {

	@Volatile
	private var cache: List<BiblePlace>? = null

	suspend fun getAll(context: Context): List<BiblePlace> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): BiblePlace? =
		getAll(context).find { it.id == id }

	private fun load(context: Context): List<BiblePlace> {
		val json = context.assets.open("knowledge/bible_places.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			BiblePlace(
				id = obj.getString("id"),
				name = obj.getString("name"),
				otherNames = obj.optString("otherNames", ""),
				category = obj.getString("category"),
				region = obj.getString("region"),
				summary = obj.getString("summary"),
				description = obj.getString("description"),
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}