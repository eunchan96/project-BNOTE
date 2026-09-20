package com.chan.bnote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.chan.bnote.MainActivity
import com.chan.bnote.R

/**
 * 두 위젯이 함께 쓰는 RemoteViews 도구 모음(테마 적용, 빈 상태 표시, 글자 크기, 탭 동작).
 *
 * 두 위젯의 레이아웃은 widget_root / widget_bg / widget_title / widget_text / widget_label /
 * widget_message 여섯 개의 id를 똑같이 갖고 있어서, 여기 함수들은 어느 위젯이든 그대로 쓸 수 있다.
 */
object WidgetViews {

	// PendingIntent는 (요청 코드 + Intent의 목적지)가 같으면 같은 것으로 취급돼서, 위젯이 여러 개일 때
	// 코드를 위젯마다 다르게 주지 않으면 서로의 extras를 덮어써버린다. 알림(1001~)·알람(2001~)이 쓰는
	// 코드와 겹치지 않도록 큰 값에서 시작한다.
	private const val REQUEST_BASE_ACTIVITY = 100_000
	const val REQUEST_BASE_BROADCAST = 200_000

	private const val DEFAULT_WIDTH_DP = 250
	private const val DEFAULT_HEIGHT_DP = 180
	private const val MIN_TEXT_SP = 11f
	private const val MAX_TEXT_SP = 18f

	/**
	 * 위젯에 저장된 테마·배경 투명도를 적용하고, 적용한 테마를 돌려준다(위젯별로 더 칠할 게 있을 때 쓴다).
	 *
	 * 배경은 root의 background가 아니라 그 뒤에 깔아둔 ImageView(widget_bg)로 그린다. 뷰 전체의
	 * alpha를 낮추면 글자까지 같이 흐려지지만, 이미지뷰의 imageAlpha는 배경 그림에만 적용돼서
	 * 둥근 모서리·테두리는 그대로 두고 배경만 투명하게 만들 수 있다.
	 */
	fun applyTheme(context: Context, views: RemoteViews, appWidgetId: Int): WidgetTheme {
		val theme = WidgetSettings.getTheme(context, appWidgetId)
		val opacityPercent = 100 - WidgetSettings.getTransparency(context, appWidgetId)

		// 배경은 리소스 한정자가 아니라 여기서 골라 넣는다(WidgetTheme 설명 참고).
		views.setImageViewResource(R.id.widget_bg, theme.backgroundRes)
		views.setInt(R.id.widget_bg, "setImageAlpha", opacityPercent * 255 / 100)
		views.setTextColor(R.id.widget_title, ContextCompat.getColor(context, theme.accentRes))
		views.setTextColor(R.id.widget_text, ContextCompat.getColor(context, theme.primaryTextRes))
		views.setTextColor(
			R.id.widget_label,
			ContextCompat.getColor(context, theme.secondaryTextRes)
		)
		views.setTextColor(
			R.id.widget_message,
			ContextCompat.getColor(context, theme.secondaryTextRes)
		)
		return theme
	}

	/** 본문 대신 안내 문구를 보여준다(빈 상태·오류). */
	fun showMessage(views: RemoteViews, message: String) {
		views.setTextViewText(R.id.widget_message, message)
		views.setViewVisibility(R.id.widget_message, View.VISIBLE)
		views.setViewVisibility(R.id.widget_text, View.GONE)
		views.setViewVisibility(R.id.widget_label, View.GONE)
	}

	fun showBody(views: RemoteViews) {
		views.setViewVisibility(R.id.widget_message, View.GONE)
		views.setViewVisibility(R.id.widget_text, View.VISIBLE)
		views.setViewVisibility(R.id.widget_label, View.VISIBLE)
	}

	/** 데이터를 못 읽었을 때 쓰는 화면. 어느 위젯이든 같은 레이아웃 id를 가지므로 하나로 충분하다. */
	fun errorViews(context: Context, appWidgetId: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_today_verse)
		applyTheme(context, views, appWidgetId)
		showMessage(views, "위젯을 불러오지 못했어요.\n눌러서 앱을 열어주세요")
		views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, appWidgetId))
		return views
	}

	/** 지금 위젯의 (가로, 세로) 크기를 dp로. 아직 알려주지 않은 경우엔 기본 4x3 크기로 본다. */
	fun sizeDp(context: Context, appWidgetId: Int): Pair<Int, Int> {
		val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
		val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
			.takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
		val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
			.takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
		return width to height
	}

	/**
	 * 위젯 크기 안에 [text]가 들어가는 가장 큰 글자 크기(sp)를 어림해서 고른다. RemoteViews는 글자가
	 * 넘치는지 직접 재볼 수 없고 자동 크기 조절도 안드로이드 8 이상에서만 되기 때문에, 한글 한 글자가
	 * 대략 글자 크기만큼의 너비라고 보고 줄 수를 계산한다. 그래도 넘치면 XML의 maxLines/ellipsize가
	 * 뒤를 말줄임으로 자른다.
	 *
	 * [reserveDp]는 본문 말고 위젯이 차지하는 세로 공간(제목·위치 표기 줄)이다.
	 */
	fun pickTextSizeSp(text: String, widthDp: Int, heightDp: Int, reserveDp: Int): Float {
		val usableWidth = (widthDp - 24).coerceAtLeast(60) // 좌우 패딩 12dp씩
		val usableHeight = (heightDp - 24 - reserveDp).coerceAtLeast(24) // 상하 패딩 12dp씩

		var size = MAX_TEXT_SP
		while (size > MIN_TEXT_SP) {
			val charsPerLine = (usableWidth / (size * 1.05f)).toInt().coerceAtLeast(1)
			var lines = 0
			for (line in text.split('\n')) {
				lines += maxOf(1, (line.length + charsPerLine - 1) / charsPerLine)
			}
			if (lines * size * 1.45f <= usableHeight) return size
			size -= 1f
		}
		return MIN_TEXT_SP
	}

	/** 그냥 앱을 연다(성경 데이터 준비 전 안내 화면 등). */
	fun openAppIntent(context: Context, appWidgetId: Int): PendingIntent {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
		}
		return activityIntent(context, appWidgetId, intent)
	}

	/** 그 구절이 있는 성경 장(가능하면 그 절)으로 앱을 연다. 알림을 탭했을 때와 같은 방식이다. */
	fun openBibleIntent(
		context: Context,
		appWidgetId: Int,
		bookId: Int,
		chapter: Int,
		verse: Int
	): PendingIntent {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, verse)
		}
		return activityIntent(context, appWidgetId, intent)
	}

	/** 앱 안의 다른 화면(암송 연습 등)을 바로 연다. 위젯에서는 새 태스크로 시작해야 한다. */
	fun openScreenIntent(context: Context, appWidgetId: Int, intent: Intent): PendingIntent {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		return activityIntent(context, appWidgetId, intent)
	}

	private fun activityIntent(
		context: Context,
		appWidgetId: Int,
		intent: Intent
	): PendingIntent = PendingIntent.getActivity(
		context, REQUEST_BASE_ACTIVITY + appWidgetId, intent,
		PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
	)
}