package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object ParableRepository {

	@Volatile
	private var cache: List<ParableOrMiracle>? = null

	suspend fun getAll(context: Context): List<ParableOrMiracle> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): ParableOrMiracle? =
		getAll(context).find { it.id == id }

	private fun load(context: Context): List<ParableOrMiracle> {
		val json = context.assets.open("knowledge/parables_miracles.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			ParableOrMiracle(
				id = obj.getString("id"),
				title = obj.getString("title"),
				type = obj.getString("type"),
				summary = obj.getString("summary"),
				description = obj.getString("description"),
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}