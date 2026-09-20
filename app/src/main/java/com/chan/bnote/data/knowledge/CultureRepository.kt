package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object CultureRepository {

	@Volatile
	private var cache: List<CultureTopic>? = null

	suspend fun getAll(context: Context): List<CultureTopic> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): CultureTopic? =
		getAll(context).find { it.id == id }

	private fun load(context: Context): List<CultureTopic> {
		val json = context.assets.open("knowledge/bible_culture.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			CultureTopic(
				id = obj.getString("id"),
				title = obj.getString("title"),
				category = obj.getString("category"),
				summary = obj.getString("summary"),
				description = obj.getString("description"),
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}