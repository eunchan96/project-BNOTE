package com.chan.bnote.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chan.bnote.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [NotificationScheduler]가 예약한 정확 알람(AlarmManager)이 울렸을 때 호출된다.
 *
 * 알람 자체는 "이번 1회"만 울리는 방식이라, 여기서 실제 알림을 띄운 뒤 반드시 다음 날 같은
 * 시각으로 스스로 재예약해야 한다(재부팅 시에는 [BootReceiver]가 대신 예약해준다).
 *
 * 실제 DB 조회 + 알림 표시([NotificationTasks])는 WorkManager를 거치지 않고 여기서 바로
 * 실행한다 — WorkManager로 위임했을 때는, 앱을 오래 안 켰을 때 "확장 작업(expedited) 여유"가
 * 없으면 일반 백그라운드 작업으로 밀려나면서 실행 시점이 크게 늦춰질 수 있었다(알림이 안 오다가
 * 앱을 열어야 그제서야 오는 문제의 원인). `goAsync()`로 브로드캐스트 처리 시간을 늘려서,
 * 짧은 DB 조회 + 알림 표시 정도는 안전하게 끝낼 수 있게 한다.
 */
class AlarmReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action ?: return
		if (action != NotificationScheduler.ACTION_DAILY_VERSE &&
			action != NotificationScheduler.ACTION_READING_REMINDER
		) {
			return
		}

		// BroadcastReceiver는 onReceive가 끝나면 프로세스가 곧바로 죽을 수 있는데, goAsync()로
		// 짧은 비동기 작업(DB 조회 + 알림 표시)이 끝날 때까지 시스템이 기다려주게 한다.
		val pendingResult = goAsync()
		val appContext = context.applicationContext

		CoroutineScope(Dispatchers.IO).launch {
			try {
				when (action) {
					NotificationScheduler.ACTION_DAILY_VERSE -> {
						NotificationTasks.showDailyVerse(appContext)
						if (AppSettings.isDailyVerseNotiEnabled(appContext)) {
							val (h, m) = AppSettings.getDailyVerseNotiTime(appContext)
							NotificationScheduler.scheduleDailyVerse(appContext, h, m)
						}
					}

					NotificationScheduler.ACTION_READING_REMINDER -> {
						NotificationTasks.showReadingReminderIfNeeded(appContext)
						if (AppSettings.isReadingReminderEnabled(appContext)) {
							val (h, m) = AppSettings.getReadingReminderTime(appContext)
							NotificationScheduler.scheduleReadingReminder(appContext, h, m)
						}
					}
				}
			} finally {
				pendingResult.finish()
			}
		}
	}
}