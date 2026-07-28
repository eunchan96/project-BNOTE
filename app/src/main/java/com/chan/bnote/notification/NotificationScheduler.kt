package com.chan.bnote.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * 매일 정해진 시각에 오는 알림(매일 말씀, 통독 리마인더) 예약.
 *
 * WorkManager의 PeriodicWorkRequest는 "정확한 시각"을 보장하지 않는다 — Doze 모드 등에 걸리면
 * 지연되고, 한 번 밀리면 다음 주기도 밀린 시점 기준으로 다시 잡혀서 시간이 갈수록 알림 시각이
 * 계속 뒤로 드리프트되는 문제가 있었다. 그래서 여기서는 AlarmManager의 정확 알람
 * (setExactAndAllowWhileIdle)으로 "이번 1회"만 예약하고, 실제 알림을 띄운 뒤 [AlarmReceiver]가
 * 다음 날 같은 시각으로 스스로 재예약하는 방식을 쓴다. 기기가 재부팅되면 정확 알람이 전부
 * 취소되므로 [BootReceiver]가 재부팅 시 다시 예약해준다.
 */
object NotificationScheduler {

	const val ACTION_DAILY_VERSE = "com.chan.bnote.action.DAILY_VERSE_ALARM"
	const val ACTION_READING_REMINDER = "com.chan.bnote.action.READING_REMINDER_ALARM"

	private const val REQUEST_CODE_DAILY_VERSE = 2001
	private const val REQUEST_CODE_READING_REMINDER = 2002

	fun scheduleDailyVerse(context: Context, hour: Int, minute: Int) {
		schedule(context, ACTION_DAILY_VERSE, REQUEST_CODE_DAILY_VERSE, hour, minute)
	}

	fun cancelDailyVerse(context: Context) {
		cancel(context, ACTION_DAILY_VERSE, REQUEST_CODE_DAILY_VERSE)
	}

	fun scheduleReadingReminder(context: Context, hour: Int, minute: Int) {
		schedule(context, ACTION_READING_REMINDER, REQUEST_CODE_READING_REMINDER, hour, minute)
	}

	fun cancelReadingReminder(context: Context) {
		cancel(context, ACTION_READING_REMINDER, REQUEST_CODE_READING_REMINDER)
	}

	/** 이 기기/OS 버전에서 지금 정확 알람을 예약할 수 있는 상태인지(Android 12 미만은 항상 가능). */
	fun canScheduleExactAlarms(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
		val alarmManager = context.getSystemService(AlarmManager::class.java)
		return alarmManager.canScheduleExactAlarms()
	}

	private fun schedule(
		context: Context,
		action: String,
		requestCode: Int,
		hour: Int,
		minute: Int
	) {
		if (!canScheduleExactAlarms(context)) {
			// 권한이 없으면 예약할 수 없다 — 호출부(SettingsActivity)가 미리 권한을 확인/요청해야
			// 한다. 여기서는 크래시 방지를 위한 방어 코드로만 남겨둔다.
			return
		}
		val alarmManager = context.getSystemService(AlarmManager::class.java)
		alarmManager.setExactAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			computeNextTriggerMillis(hour, minute),
			pendingIntentFor(context, action, requestCode)
		)
	}

	private fun cancel(context: Context, action: String, requestCode: Int) {
		val alarmManager = context.getSystemService(AlarmManager::class.java)
		alarmManager.cancel(pendingIntentFor(context, action, requestCode))
	}

	private fun pendingIntentFor(
		context: Context,
		action: String,
		requestCode: Int
	): PendingIntent {
		val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
		return PendingIntent.getBroadcast(
			context, requestCode, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}

	/** 오늘 [hour]:[minute]가 이미 지났으면 내일, 아니면 오늘 그 시각의 epoch millis. */
	fun computeNextTriggerMillis(hour: Int, minute: Int): Long {
		val now = Calendar.getInstance()
		val target = Calendar.getInstance().apply {
			set(Calendar.HOUR_OF_DAY, hour)
			set(Calendar.MINUTE, minute)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}
		if (!target.after(now)) {
			target.add(Calendar.DAY_OF_YEAR, 1)
		}
		return target.timeInMillis
	}
}