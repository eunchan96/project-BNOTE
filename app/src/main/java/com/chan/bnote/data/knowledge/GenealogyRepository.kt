package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object GenealogyRepository {

	@Volatile
	private var cache: List<GenealogyChart>? = null

	suspend fun getAll(context: Context): List<GenealogyChart> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): GenealogyChart? =
		getAll(context).find { it.id == id }

	private fun load(context: Context): List<GenealogyChart> {
		val json = context.assets.open("knowledge/genealogy_charts.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			val entriesArray = obj.getJSONArray("entries")
			val entries = (0 until entriesArray.length()).map { j ->
				val e = entriesArray.getJSONObject(j)
				GenealogyEntry(
					name = e.getString("name"),
					relation = e.getString("relation"),
					note = e.optString("note", "")
				)
			}
			GenealogyChart(
				id = obj.getString("id"),
				title = obj.getString("title"),
				description = obj.getString("description"),
				entries = entries,
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}