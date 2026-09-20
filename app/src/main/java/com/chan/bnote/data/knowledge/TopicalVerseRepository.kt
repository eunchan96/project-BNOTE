package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object TopicalVerseRepository {

	@Volatile
	private var cache: List<TopicalVerseGroup>? = null

	suspend fun getAll(context: Context): List<TopicalVerseGroup> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): TopicalVerseGroup? =
		getAll(context).find { it.id == id }

	private fun load(context: Context): List<TopicalVerseGroup> {
		val json = context.assets.open("knowledge/topical_verses.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			val versesArray = obj.getJSONArray("verses")
			val verses = (0 until versesArray.length()).map { j ->
				val v = versesArray.getJSONObject(j)
				VerseRef(
					bookId = v.getInt("bookId"),
					chapter = v.getInt("chapter"),
					verseStart = v.getInt("verseStart"),
					verseEnd = v.getInt("verseEnd")
				)
			}
			TopicalVerseGroup(
				id = obj.getString("id"),
				title = obj.getString("title"),
				verses = verses
			)
		}
	}
}