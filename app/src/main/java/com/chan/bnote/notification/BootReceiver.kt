package com.chan.bnote.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chan.bnote.data.AppSettings

/**
 * 기기가 재부팅되면 AlarmManager에 예약해둔 정확 알람이 전부 사라지므로,
 * 사용자가 켜둔 알림이 있으면 여기서 다시 예약해준다.
 */
class BootReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

		if (AppSettings.isDailyVerseNotiEnabled(context)) {
			val (h, m) = AppSettings.getDailyVerseNotiTime(context)
			NotificationScheduler.scheduleDailyVerse(context, h, m)
		}
		if (AppSettings.isReadingReminderEnabled(context)) {
			val (h, m) = AppSettings.getReadingReminderTime(context)
			NotificationScheduler.scheduleReadingReminder(context, h, m)
		}
	}
}