package com.chan.bnote.widget

import android.content.Context
import android.widget.RemoteViews
import com.chan.bnote.R
import com.chan.bnote.notification.DailyVerseProvider

/** "오늘의 말씀" 위젯. 동작은 전부 [AsyncWidgetProvider]에 있고, 그리는 내용은 [TodayVerseWidget]이 만든다. */
class TodayVerseWidgetProvider : AsyncWidgetProvider()

/**
 * 오늘의 말씀 위젯 화면. 구절은 매일 말씀 알림과 같은 [DailyVerseProvider]에서 받아오기 때문에,
 * 알림과 위젯, 그리고 다른 사용자의 위젯까지 같은 날에는 항상 같은 구절이 보인다.
 */
object TodayVerseWidget {

	// 위젯 안에서 본문 말고 세로로 차지하는 부분(제목·위치 표기가 한 줄 + 본문 위 간격)
	private const val RESERVED_HEIGHT_DP = 22

	/**
	 * [spec]을 안 주면 저장된 설정과 실제 크기로 그린다(홈 화면 위젯). 위젯 설정 화면의 미리보기는
	 * 지금 고르는 값이 담긴 spec을 넘겨서 같은 코드로 그린다.
	 */
	suspend fun buildViews(
		context: Context,
		appWidgetId: Int,
		spec: WidgetRenderSpec = WidgetViews.savedSpec(context, appWidgetId)
	): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_today_verse)
		WidgetViews.applyTheme(context, views, spec.style)

		val daily = DailyVerseProvider.getToday(context)
		if (daily == null) {
			// 성경 본문은 성경 탭을 처음 열 때 심어지기 때문에, 설치 직후 앱을 한 번도 안 열었으면
			// 아직 읽을 구절이 없다. 앱을 열면 시딩이 끝나고(앱이 백그라운드로 갈 때 위젯이 갱신된다).
			WidgetViews.showMessage(views, "앱을 한 번 열어 성경 데이터를 준비해 주세요")
			if (!spec.isPreview) {
				views.setOnClickPendingIntent(
					R.id.widget_root, WidgetViews.openAppIntent(context, appWidgetId)
				)
			}
			return views
		}

		WidgetViews.setBodyText(
			context, views, daily.text, spec.widthDp, spec.heightDp, RESERVED_HEIGHT_DP
		)
		views.setTextViewText(R.id.widget_label, daily.label)
		WidgetViews.showBody(views)

		if (!spec.isPreview) {
			views.setOnClickPendingIntent(
				R.id.widget_root,
				WidgetViews.openBibleIntent(
					context, appWidgetId, daily.bookId, daily.chapter, daily.startVerse
				)
			)
		}
		return views
	}
}