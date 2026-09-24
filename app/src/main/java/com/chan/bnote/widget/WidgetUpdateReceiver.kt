package com.chan.bnote.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [WidgetUpdater.scheduleNextMidnight]가 예약한 자정 알람이 울렸을 때 위젯을 새로 그린다.
 * 알람은 1회용이라 울릴 때마다 다음 날 것을 다시 예약해야 한다(알림의 [AlarmReceiver][com.chan.bnote.notification.AlarmReceiver]와 같은 방식).
 */
class WidgetUpdateReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != WidgetUpdater.ACTION_MIDNIGHT) return

		val appContext = context.applicationContext
		// 그 사이에 위젯을 전부 지웠다면 더 예약하지 않는다.
		if (!WidgetUpdater.hasAnyWidget(appContext)) return

		// 갱신이 실패하더라도 내일 것은 예약돼 있도록, 갱신보다 예약을 먼저 한다.
		WidgetUpdater.scheduleNextMidnight(appContext)

		val pendingResult = goAsync()
		CoroutineScope(Dispatchers.IO).launch {
			try {
				WidgetUpdater.updateAll(appContext)
			} catch (e: Exception) {
				// 위젯 갱신 실패로 앱이 죽지 않게 한다.
			} finally {
				pendingResult.finish()
			}
		}
	}
}