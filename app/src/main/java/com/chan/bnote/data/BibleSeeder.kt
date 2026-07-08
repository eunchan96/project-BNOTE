package com.chan.bnote.data

import android.content.Context
import org.json.JSONArray
import java.io.FileNotFoundException

object BibleSeeder {

	suspend fun seedIfEmpty(context: Context, db: BibleDatabase) {
		for (translation in Translation.values()) {
			seedTranslationIfEmpty(context, db, translation)
		}
	}

	private suspend fun seedTranslationIfEmpty(
		context: Context,
		db: BibleDatabase,
		translation: Translation
	) {
		if (db.bibleDao().countForTranslation(translation.code) > 0) return

		val jsonText = try {
			context.assets.open(translation.assetFileName)
				.bufferedReader(Charsets.UTF_8)
				.use { it.readText() }
		} catch (e: FileNotFoundException) {
			return // 해당 번역본 파일이 assets에 없으면 조용히 스킵
		}

		val array = JSONArray(jsonText)
		val verses = ArrayList<BibleVerse>(array.length())

		for (i in 0 until array.length()) {
			val obj = array.getJSONObject(i)
			verses.add(
				BibleVerse(
					translation = translation.code,
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