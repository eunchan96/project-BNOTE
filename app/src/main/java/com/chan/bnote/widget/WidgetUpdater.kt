package com.chan.bnote.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.chan.bnote.widget.WidgetUpdater.ACTION_MIDNIGHT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 홈 화면 위젯을 다시 그리는 곳 + 날짜가 바뀌는 자정에 위젯을 갱신하도록 예약하는 곳.
 *
 * 위젯은 하루에 한 번(날짜가 바뀔 때)만 내용이 달라지면 되는데, 시스템이 주기적으로 갱신해주는
 * updatePeriodMillis는 최소 30분 단위라 배터리를 쓸데없이 쓰고 정확한 자정도 아니다. 그래서
 * 그 값은 0으로 꺼두고, 다음 날 0시 1분에 한 번 울리는 알람([ACTION_MIDNIGHT])으로 갱신하며,
 * 울릴 때마다 다음 날 것을 다시 예약한다(알림의 정확 알람과 같은 방식).
 *
 * 자정 알람은 정확할 필요가 없어서 정확 알람 권한이 필요 없는 setAndAllowWhileIdle을 쓴다.
 * 그래서 조금 늦게 울릴 수 있는데, 그래도 화면은 갱신 시점에 "그때의 날짜"로 그려지므로 늦게
 * 울려도 내용이 틀리지는 않는다.
 */
object WidgetUpdater {

	const val ACTION_MIDNIGHT = "com.chan.bnote.action.WIDGET_MIDNIGHT_UPDATE"
	private const val REQUEST_CODE_MIDNIGHT = 3001

	private val providerClasses = listOf(
		TodayVerseWidgetProvider::class.java,
		MemorizationWidgetProvider::class.java
	)

	/** 홈 화면에 이 앱의 위젯이 하나라도 올라가 있는지. */
	fun hasAnyWidget(context: Context): Boolean {
		val manager = AppWidgetManager.getInstance(context)
		return providerClasses.any { providerClass ->
			manager.getAppWidgetIds(ComponentName(context, providerClass)).isNotEmpty()
		}
	}

	/** 위젯 하나를 다시 그린다. 어떤 위젯인지는 그 id를 등록한 provider로 알아낸다. */
	suspend fun update(context: Context, appWidgetId: Int) {
		val appContext = context.applicationContext
		val manager = AppWidgetManager.getInstance(appContext)
		val providerName = manager.getAppWidgetInfo(appWidgetId)?.provider?.className ?: return

		val views = try {
			when (providerName) {
				TodayVerseWidgetProvider::class.java.name ->
					TodayVerseWidget.buildViews(appContext, appWidgetId)

				MemorizationWidgetProvider::class.java.name ->
					MemorizationWidget.buildViews(appContext, appWidgetId)

				else -> return
			}
		} catch (e: Exception) {
			// DB 조회 등에서 예외가 나도 위젯이 "로드 실패"로 죽지 않고 안내 문구를 보여주게 한다.
			WidgetViews.errorViews(appContext, appWidgetId)
		}
		manager.updateAppWidget(appWidgetId, views)
	}

	suspend fun updateAll(context: Context) {
		val appContext = context.applicationContext
		val manager = AppWidgetManager.getInstance(appContext)
		for (providerClass in providerClasses) {
			for (id in manager.getAppWidgetIds(ComponentName(appContext, providerClass))) {
				update(appContext, id)
			}
		}
	}

	/** 화면이 없는 곳(앱이 백그라운드로 갈 때 등)에서 결과를 기다리지 않고 갱신만 요청한다. */
	fun requestUpdateAll(context: Context) {
		val appContext = context.applicationContext
		if (!hasAnyWidget(appContext)) return
		CoroutineScope(Dispatchers.IO).launch {
			try {
				updateAll(appContext)
			} catch (e: Exception) {
				// 위젯 갱신 실패가 앱에 영향을 주지 않게 조용히 넘긴다.
			}
		}
	}

	/** 다음 날 0시 1분에 위젯을 갱신하도록 예약한다. 같은 예약이 있으면 덮어쓴다. */
	fun scheduleNextMidnight(context: Context) {
		val alarmManager = context.getSystemService(AlarmManager::class.java)
		val next = Calendar.getInstance().apply {
			add(Calendar.DAY_OF_YEAR, 1)
			set(Calendar.HOUR_OF_DAY, 0)
			set(Calendar.MINUTE, 1)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis
		alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, next, midnightIntent(context))
	}

	fun cancelMidnight(context: Context) {
		context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
	}

	private fun midnightIntent(context: Context): PendingIntent {
		val intent = Intent(context, WidgetUpdateReceiver::class.java).setAction(ACTION_MIDNIGHT)
		return PendingIntent.getBroadcast(
			context, REQUEST_CODE_MIDNIGHT, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}
}