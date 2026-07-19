package com.chan.bnote.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import java.util.Calendar

class ReadingReminderWorker(context: Context, params: WorkerParameters) :
	CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		val db = BibleDatabase.getInstance(applicationContext)

		val startOfToday = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		val startOfTomorrow = Calendar.getInstance().apply {
			timeInMillis = startOfToday
			add(Calendar.DAY_OF_YEAR, 1)
		}.timeInMillis

		val readToday = db.readingProgressDao().countReadBetween(startOfToday, startOfTomorrow)
		if (readToday > 0) {
			// 오늘 이미 읽었으면 굳이 알릴 필요 없음
			return Result.success()
		}

		NotificationHelper.show(
			context = applicationContext,
			notiId = NotificationHelper.NOTI_ID_READING_REMINDER,
			title = "오늘의 성경 읽기",
			content = "아직 오늘 성경을 읽지 않으셨어요. 지금 잠깐 읽어볼까요?"
		)
		return Result.success()
	}
}