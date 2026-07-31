package com.chan.bnote.notification

import android.content.Context
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.bible.BibleBooks
import java.util.Calendar

/**
 * 매일 말씀 알림 · 통독 리마인더의 실제 내용(DB 조회 + 알림 표시) 로직.
 *
 * 예전에는 [AlarmReceiver]가 이 작업을 WorkManager 1회성 작업으로 위임했는데, WorkManager는
 * 앱을 오래 안 켰을 때 "확장 작업(expedited) 여유"가 없으면 일반 백그라운드 작업으로 밀려나고,
 * 그러면 안드로이드가 실행 시점을 상당히 미룰 수 있다(그래서 알림이 안 오다가 앱을 열면 그제서야
 * 오는 문제가 있었다). 그래서 지금은 정확 알람이 울리는 즉시 이 함수를 직접 실행한다.
 */
object NotificationTasks {

	suspend fun showDailyVerse(context: Context) {
		val db = BibleDatabase.getInstance(context)
		val ref = CuratedVerses.load(context).random()
		val chapterVerses = db.bibleDao().getVerses("NKRV", ref.bookId, ref.chapter)
		val rangeVerses = chapterVerses.filter { it.verse in ref.startVerse..ref.endVerse }
		if (rangeVerses.isEmpty()) return

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
			context = context,
			notiId = NotificationHelper.NOTI_ID_DAILY_VERSE,
			title = "오늘의 말씀 · $label",
			content = verseText,
			bookId = verse.bookId,
			chapter = verse.chapter,
			verse = verse.verse
		)
	}

	suspend fun showReadingReminderIfNeeded(context: Context) {
		val db = BibleDatabase.getInstance(context)

		val startOfToday = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		val startOfTomorrow = Calendar.getInstance().apply {
			timeInMillis = startOfToday
			add(Calendar.DAY_OF_YEAR, 1)
		}.timeInMillis

		val readToday = db.readingProgressDao().countReadBetween(startOfToday, startOfTomorrow)
		if (readToday > 0) {
			// 오늘 이미 읽었으면 굳이 알릴 필요 없음
			return
		}

		NotificationHelper.show(
			context = context,
			notiId = NotificationHelper.NOTI_ID_READING_REMINDER,
			title = "오늘의 성경 읽기",
			content = "아직 오늘 성경을 읽지 않으셨어요. 지금 잠깐 읽어볼까요?"
		)
	}
}