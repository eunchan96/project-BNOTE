package com.chan.bnote.data.knowledge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** 인물사전 데이터. JSON 에셋에서 읽어와 메모리에 캐시한다 (수정 없는 참고 자료라 Room을 쓰지 않음). */
object BibleFigureRepository {

	@Volatile
	private var cache: List<BibleFigure>? = null

	suspend fun getAll(context: Context): List<BibleFigure> {
		cache?.let { return it }
		return withContext(Dispatchers.IO) {
			cache ?: load(context).also { cache = it }
		}
	}

	suspend fun getById(context: Context, id: String): BibleFigure? =
		getAll(context).find { it.id == id }

	suspend fun search(context: Context, query: String): List<BibleFigure> {
		val all = getAll(context)
		val trimmed = query.trim()
		if (trimmed.isEmpty()) return all
		return all.filter {
			it.name.contains(trimmed) || it.otherNames.contains(trimmed) || it.summary.contains(
				trimmed
			)
		}
	}

	private fun load(context: Context): List<BibleFigure> {
		val json = context.assets.open("knowledge/bible_figures.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
		val array = JSONArray(json)
		return (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			BibleFigure(
				id = obj.getString("id"),
				name = obj.getString("name"),
				otherNames = obj.optString("otherNames", ""),
				category = obj.getString("category"),
				era = obj.getString("era"),
				summary = obj.getString("summary"),
				description = obj.getString("description"),
				keyBookId = obj.getInt("keyBookId"),
				keyChapter = obj.getInt("keyChapter"),
				keyVerseLabel = obj.getString("keyVerseLabel")
			)
		}
	}
}