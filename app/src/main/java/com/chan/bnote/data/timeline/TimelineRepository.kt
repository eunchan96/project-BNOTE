package com.chan.bnote.data.timeline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object TimelineRepository {

	@Volatile
	private var cache: List<TimelineEvent>? = null

	// JSON은 이미 연대순으로 정리돼 있으므로 그대로 반환한다.
	suspend fun getAll(context: Context): List<TimelineEvent> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	private fun load(context: Context): List<TimelineEvent> {
		val json = context.assets.open("timeline/bible_timeline.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			TimelineEvent(
				id = obj.getString("id"),
				era = obj.getString("era"),
				period = obj.getString("period"),
				title = obj.getString("title"),
				description = obj.getString("description"),
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}