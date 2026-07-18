package com.chan.bnote.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks

class DailyVerseWorker(context: Context, params: WorkerParameters) :
	CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		val db = BibleDatabase.getInstance(applicationContext)
		val verse = db.bibleDao().getRandomVerse("NKRV") ?: return Result.success()

		val unit = BibleBooks.chapterUnit(verse.bookId)
		val label = "${BibleBooks.nameOf(verse.bookId)} ${verse.chapter}${unit} ${verse.verse}절"

		NotificationHelper.show(
			context = applicationContext,
			notiId = NotificationHelper.NOTI_ID_DAILY_VERSE,
			title = "오늘의 말씀 · $label",
			content = verse.text,
			bookId = verse.bookId,
			chapter = verse.chapter
		)
		return Result.success()
	}
}