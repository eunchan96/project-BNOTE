package com.chan.bnote.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 두 위젯 provider의 공통 동작. 위젯 갱신은 DB를 조회해야 하는데 BroadcastReceiver는
 * onReceive가 끝나면 프로세스가 곧바로 죽을 수 있어서, 알림의 AlarmReceiver와 같이
 * goAsync()로 시스템이 작업이 끝날 때까지 기다려주게 한다.
 */
abstract class AsyncWidgetProvider : AppWidgetProvider() {

	protected fun runAsync(block: suspend () -> Unit) {
		val pendingResult = goAsync()
		CoroutineScope(Dispatchers.IO).launch {
			try {
				block()
			} catch (e: Exception) {
				// 위젯 갱신 실패로 앱이 죽지 않게 한다.
			} finally {
				pendingResult.finish()
			}
		}
	}

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		// 재부팅 뒤에는 시스템이 위젯마다 onUpdate를 불러주므로, 사라진 자정 알람도 여기서 다시 잡는다.
		WidgetUpdater.scheduleNextMidnight(context)
		runAsync {
			for (id in appWidgetIds) WidgetUpdater.update(context, id)
		}
	}

	/** 위젯 크기를 바꾸면 그 크기에 맞는 글자 크기로 다시 그린다. */
	override fun onAppWidgetOptionsChanged(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetId: Int,
		newOptions: Bundle
	) {
		runAsync { WidgetUpdater.update(context, appWidgetId) }
	}

	override fun onDeleted(context: Context, appWidgetIds: IntArray) {
		WidgetSettings.clear(context, appWidgetIds)
	}

	override fun onEnabled(context: Context) {
		WidgetUpdater.scheduleNextMidnight(context)
	}

	/** 이 종류의 마지막 위젯이 지워졌을 때. 다른 종류의 위젯이 남아있으면 자정 알람도 남겨둔다. */
	override fun onDisabled(context: Context) {
		if (!WidgetUpdater.hasAnyWidget(context)) WidgetUpdater.cancelMidnight(context)
	}
}