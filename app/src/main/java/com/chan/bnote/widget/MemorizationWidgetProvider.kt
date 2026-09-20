package com.chan.bnote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.notification.DailyVerseProvider
import com.chan.bnote.ui.mypage.memorization.MemorizationPracticeActivity
import com.chan.bnote.ui.mypage.memorization.MemorizationVerseListActivity

/** "암송 구절" 위젯. 공통 동작은 [AsyncWidgetProvider]에 있고, 여기서는 "다음" 버튼만 추가로 받는다. */
class MemorizationWidgetProvider : AsyncWidgetProvider() {

	companion object {
		const val ACTION_NEXT = "com.chan.bnote.action.MEMORIZATION_WIDGET_NEXT"
	}

	override fun onReceive(context: Context, intent: Intent) {
		super.onReceive(context, intent)
		if (intent.action != ACTION_NEXT) return

		val appWidgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
		)
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

		WidgetSettings.setOffset(
			context,
			appWidgetId,
			WidgetSettings.getOffset(context, appWidgetId) + 1
		)
		runAsync { WidgetUpdater.update(context, appWidgetId) }
	}
}

/**
 * 암송 구절 위젯 화면. 고른 그룹(없으면 전체)의 구절 중 하나를 보여주고, 날짜가 바뀔 때마다
 * 다음 구절로 넘어간다. "다음" 버튼으로 그 자리에서 다음 구절로 넘길 수도 있고, 위젯을 탭하면
 * 그 구절의 암송 연습 화면이 열린다.
 */
object MemorizationWidget {

	// 위젯 안에서 본문 말고 세로로 차지하는 부분(제목 줄 + 구절 위치 줄 + 간격)
	private const val RESERVED_HEIGHT_DP = 44

	suspend fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_memorization)
		val theme = WidgetViews.applyTheme(context, views, appWidgetId)
		views.setTextColor(
			R.id.widget_group,
			ContextCompat.getColor(context, theme.secondaryTextRes)
		)
		views.setTextColor(R.id.widget_next, ContextCompat.getColor(context, theme.accentRes))
		// 이 위젯에서는 위치 표기가 제목 바로 아래 있어서 본문색으로 또렷하게 보인다.
		views.setTextColor(R.id.widget_label, ContextCompat.getColor(context, theme.primaryTextRes))

		val dao = BibleDatabase.getInstance(context.applicationContext).memorizationVerseDao()

		// 위젯에 저장해둔 그룹을 찾는다. 삭제됐거나 데이터 불러오기로 id가 새로 매겨졌으면 이름으로
		// 다시 찾고, 그래도 없으면 "전체 그룹"으로 대신한다. 찾은 그룹의 최신 id·이름은 다시 저장해둔다.
		val choice = WidgetSettings.getGroup(context, appWidgetId)
		val group = choice?.let { saved ->
			val groups = dao.getAllGroups()
			groups.firstOrNull { it.id == saved.id } ?: groups.firstOrNull { it.name == saved.name }
		}
		if (choice != null && group != null &&
			(group.id != choice.id || group.name != choice.name)
		) {
			WidgetSettings.setGroup(
				context, appWidgetId, WidgetSettings.GroupChoice(group.id, group.name)
			)
		}

		// 구절을 추가한 순서대로 고정해서, 넘길 때마다 순서가 뒤섞이지 않게 한다.
		val verses = (if (group != null) dao.getByGroup(group.id) else dao.getAll())
			.sortedWith(compareBy({ it.createdAt }, { it.id }))
		val groupName = group?.name ?: "전체"

		if (verses.isEmpty()) {
			views.setTextViewText(R.id.widget_group, groupName)
			views.setViewVisibility(R.id.widget_next, View.GONE)
			WidgetViews.showMessage(
				views,
				if (group != null) "이 그룹에 구절이 없어요.\n눌러서 추가해 보세요"
				else "암송 구절이 없어요.\n눌러서 추가해 보세요"
			)
			views.setOnClickPendingIntent(
				R.id.widget_root,
				WidgetViews.openScreenIntent(
					context, appWidgetId,
					Intent(context, MemorizationVerseListActivity::class.java)
				)
			)
			return views
		}

		val index = (DailyVerseProvider.epochDay() + WidgetSettings.getOffset(context, appWidgetId))
			.mod(verses.size.toLong()).toInt()
		val verse = verses[index]

		val (widthDp, heightDp) = WidgetViews.sizeDp(context, appWidgetId)
		views.setTextViewText(R.id.widget_group, "$groupName · ${index + 1}/${verses.size}")
		views.setTextViewText(R.id.widget_label, verse.toDisplayLabel())
		WidgetViews.setBodyText(
			context,
			views,
			verse.verseText,
			widthDp,
			heightDp,
			RESERVED_HEIGHT_DP
		)
		WidgetViews.showBody(views)

		// 구절이 하나뿐이면 넘길 곳이 없으니 버튼을 숨긴다.
		views.setViewVisibility(R.id.widget_next, if (verses.size > 1) View.VISIBLE else View.GONE)
		views.setOnClickPendingIntent(R.id.widget_next, nextIntent(context, appWidgetId))
		views.setOnClickPendingIntent(
			R.id.widget_root,
			WidgetViews.openScreenIntent(
				context, appWidgetId,
				MemorizationPracticeActivity.singleVerseIntent(context, verse.id)
			)
		)
		return views
	}

	private fun nextIntent(context: Context, appWidgetId: Int): PendingIntent {
		val intent = Intent(context, MemorizationWidgetProvider::class.java)
			.setAction(MemorizationWidgetProvider.ACTION_NEXT)
			.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
		return PendingIntent.getBroadcast(
			context, WidgetViews.REQUEST_BASE_BROADCAST + appWidgetId, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}
}