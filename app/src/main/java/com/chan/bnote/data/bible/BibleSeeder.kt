package com.chan.bnote.data.bible

import android.content.Context
import com.chan.bnote.data.BibleDatabase
import org.json.JSONArray
import java.io.FileNotFoundException

object BibleSeeder {

	private const val PREFS_NAME = "bible_seed_prefs"
	private const val KEY_SEED_VERSION = "seed_version"

	// 성경 본문 assets(JSON)를 고칠 때마다 이 숫자를 1씩 올린다.
	// 그러면 이미 앱을 쓰고 있던 사용자도 다음 실행 시 그 번역본만 자동으로 다시 심어진다.
	private const val SEED_VERSION = 1

	// 배포 전 오탈자 등을 계속 확인하는 동안엔 true로 두면 매번(앱 실행마다) 무조건 다시 심는다.
	// 실제 배포 전에는 반드시 false로 바꿔서, 위 SEED_VERSION 번호로만 재시딩되게 할 것.
	private const val FORCE_RESEED_EVERY_LAUNCH = true

	suspend fun seedIfEmpty(context: Context, db: BibleDatabase) {
		val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
		val appliedVersion = prefs.getInt(KEY_SEED_VERSION, -1)
		val needsReseed = FORCE_RESEED_EVERY_LAUNCH || appliedVersion != SEED_VERSION

		for (translation in Translation.values()) {
			if (needsReseed) {
				db.bibleDao().deleteTranslation(translation.code)
			}
			seedTranslationIfEmpty(context, db, translation)
		}

		if (needsReseed) {
			prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()
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