package com.chan.bnote.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.chan.bnote.data.AppSettings

/**
 * [NotificationScheduler]가 예약한 정확 알람(AlarmManager)이 울렸을 때 호출된다.
 *
 * 알람 자체는 "이번 1회"만 울리는 방식이라, 여기서 실제 알림을 띄운 뒤 반드시 다음 날 같은
 * 시각으로 스스로 재예약해야 한다(재부팅 시에는 [BootReceiver]가 대신 예약해준다).
 *
 * 실제 DB 조회 + 알림 표시는 기존 [DailyVerseWorker]/[ReadingReminderWorker]에 그대로 맡긴다 —
 * 알람은 "정확한 시각에 깨우는" 역할만 하고, 실행 자체(+ 재시도 보장)는 WorkManager 1회성
 * 작업으로 위임한다.
 */
class AlarmReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		when (intent.action) {
			NotificationScheduler.ACTION_DAILY_VERSE -> {
				enqueueWork(context, DailyVerseWorker::class.java, "one_time_daily_verse")
				if (AppSettings.isDailyVerseNotiEnabled(context)) {
					val (h, m) = AppSettings.getDailyVerseNotiTime(context)
					NotificationScheduler.scheduleDailyVerse(context, h, m)
				}
			}

			NotificationScheduler.ACTION_READING_REMINDER -> {
				enqueueWork(context, ReadingReminderWorker::class.java, "one_time_reading_reminder")
				if (AppSettings.isReadingReminderEnabled(context)) {
					val (h, m) = AppSettings.getReadingReminderTime(context)
					NotificationScheduler.scheduleReadingReminder(context, h, m)
				}
			}
		}
	}

	private fun enqueueWork(
		context: Context,
		workerClass: Class<out ListenableWorker>,
		uniqueName: String
	) {
		val request = OneTimeWorkRequest.Builder(workerClass)
			.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
			.build()
		WorkManager.getInstance(context)
			.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
	}
}