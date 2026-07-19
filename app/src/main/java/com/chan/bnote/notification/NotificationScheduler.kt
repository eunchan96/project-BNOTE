package com.chan.bnote.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

	private const val WORK_DAILY_VERSE = "work_daily_verse"
	private const val WORK_READING_REMINDER = "work_reading_reminder"

	fun scheduleDailyVerse(context: Context, hour: Int, minute: Int) {
		val request = PeriodicWorkRequestBuilder<DailyVerseWorker>(1, TimeUnit.DAYS)
			.setInitialDelay(computeInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
			.build()
		WorkManager.getInstance(context).enqueueUniquePeriodicWork(
			WORK_DAILY_VERSE, ExistingPeriodicWorkPolicy.UPDATE, request
		)
	}

	fun cancelDailyVerse(context: Context) {
		WorkManager.getInstance(context).cancelUniqueWork(WORK_DAILY_VERSE)
	}

	fun scheduleReadingReminder(context: Context, hour: Int, minute: Int) {
		val request = PeriodicWorkRequestBuilder<ReadingReminderWorker>(1, TimeUnit.DAYS)
			.setInitialDelay(computeInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
			.build()
		WorkManager.getInstance(context).enqueueUniquePeriodicWork(
			WORK_READING_REMINDER, ExistingPeriodicWorkPolicy.UPDATE, request
		)
	}

	fun cancelReadingReminder(context: Context) {
		WorkManager.getInstance(context).cancelUniqueWork(WORK_READING_REMINDER)
	}

	/** 오늘 [hour]:[minute]가 이미 지났으면 내일 그 시각까지, 아니면 오늘 그 시각까지 남은 밀리초. */
	private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
		val now = Calendar.getInstance()
		val target = Calendar.getInstance().apply {
			set(Calendar.HOUR_OF_DAY, hour)
			set(Calendar.MINUTE, minute)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}
		if (target.before(now)) {
			target.add(Calendar.DAY_OF_YEAR, 1)
		}
		return target.timeInMillis - now.timeInMillis
	}
}