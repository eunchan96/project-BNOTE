package com.chan.bnote.data

import android.content.Context
import org.json.JSONArray

object BibleSeeder {

	suspend fun seedIfEmpty(context: Context, db: BibleDatabase) {
		if (db.bibleDao().count() > 0) return

		val jsonText = context.assets.open("bible.json")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }

		val array = JSONArray(jsonText)
		val verses = ArrayList<BibleVerse>(array.length())

		for (i in 0 until array.length()) {
			val obj = array.getJSONObject(i)
			verses.add(
				BibleVerse(
					bookId = obj.getInt("book"),
					chapter = obj.getInt("chapter"),
					verse = obj.getInt("verse"),
					text = obj.getString("text"),
					title = if (obj.has("title") && !obj.isNull("title")) obj.getString("title") else null
				)
			)
		}

		db.bibleDao().insertAll(verses)
	}
}