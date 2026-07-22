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
		val ref = CuratedVerses.load(applicationContext).random()
		val chapterVerses = db.bibleDao().getVerses("NKRV", ref.bookId, ref.chapter)
		val rangeVerses = chapterVerses.filter { it.verse in ref.startVerse..ref.endVerse }
		if (rangeVerses.isEmpty()) return Result.success()

		val verse = rangeVerses.first()
		val verseText = rangeVerses.joinToString("\n") { it.text }

		val unit = BibleBooks.chapterUnit(ref.bookId)
		val verseLabel = if (ref.startVerse == ref.endVerse) {
			"${ref.startVerse}절"
		} else {
			"${ref.startVerse}~${ref.endVerse}절"
		}
		val label = "${BibleBooks.nameOf(ref.bookId)} ${ref.chapter}${unit} $verseLabel"

		NotificationHelper.show(
			context = applicationContext,
			notiId = NotificationHelper.NOTI_ID_DAILY_VERSE,
			title = "오늘의 말씀 · $label",
			content = verseText,
			bookId = verse.bookId,
			chapter = verse.chapter
		)
		return Result.success()
	}
}