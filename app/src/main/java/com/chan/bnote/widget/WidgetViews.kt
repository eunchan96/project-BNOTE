package com.chan.bnote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.widget.WidgetViews.MIN_TEXT_SP

/**
 * 위젯 화면을 만들 때의 조건. 홈 화면의 실제 위젯은 저장된 설정과 실제 크기로 그리고(savedSpec),
 * 위젯 설정 화면의 미리보기는 지금 고르고 있는 값과 미리보기 크기로 같은 코드를 태워서 그린다.
 *
 * [group]은 암송 위젯만 쓴다(null이면 전체 그룹). [isPreview]가 true면 탭했을 때 동작(PendingIntent)을
 * 달지 않고, 저장된 설정도 건드리지 않는다.
 */
data class WidgetRenderSpec(
	val style: WidgetStyle,
	val widthDp: Int,
	val heightDp: Int,
	val group: WidgetSettings.GroupChoice? = null,
	val isPreview: Boolean = false
)

/**
 * 두 위젯이 함께 쓰는 RemoteViews 도구 모음(테마 적용, 빈 상태 표시, 글자 크기, 탭 동작).
 *
 * 두 위젯의 레이아웃은 widget_root / widget_bg / widget_icon / widget_title / widget_text /
 * widget_label / widget_message 일곱 개의 id를 똑같이 갖고 있어서, 여기 함수들은 어느 위젯이든 그대로 쓸 수 있다.
 */
object WidgetViews {

	// PendingIntent는 (요청 코드 + Intent의 목적지)가 같으면 같은 것으로 취급돼서, 위젯이 여러 개일 때
	// 코드를 위젯마다 다르게 주지 않으면 서로의 extras를 덮어써버린다. 알림(1001~)·알람(2001~)이 쓰는
	// 코드와 겹치지 않도록 큰 값에서 시작한다.
	private const val REQUEST_BASE_ACTIVITY = 100_000
	const val REQUEST_BASE_BROADCAST = 200_000

	// 위젯 크기를 아직 알려주지 않았을 때 쓰는 값(기본 크기 3x2)
	private const val DEFAULT_WIDTH_DP = 180
	private const val DEFAULT_HEIGHT_DP = 110

	// 본문 글자 크기 범위. 이보다 작으면 읽기 힘들어서, 다 안 들어가더라도 MIN에서 멈추고 뒤를 말줄임으로 자른다.
	private const val MIN_TEXT_SP = 12f
	private const val MAX_TEXT_SP = 18f
	private const val TEXT_STEP_SP = 0.5f

	// 레이아웃(widget_content)의 안쪽 여백(가로 14dp, 세로 12dp), 그리고 런처가 그리는 실제 크기와
	// 어긋날 수 있어서 남겨두는 여유
	private const val CONTENT_PADDING_H_DP = 14
	private const val CONTENT_PADDING_V_DP = 12
	private const val SAFETY_WIDTH_DP = 4
	private const val SAFETY_HEIGHT_DP = 2

	// 레이아웃 XML의 lineSpacingMultiplier와 같은 값이어야 계산이 맞는다.
	private const val LINE_SPACING_MULTIPLIER = 1.15f

	/** 본문에 쓸 글자 크기와, 그 크기에서 화면에 들어가는 최대 줄 수. */
	data class TextFit(val sizeSp: Float, val maxLines: Int)

	/** 실제 홈 화면 위젯을 그릴 때의 조건: 저장된 설정 + 런처가 알려준 실제 크기. */
	fun savedSpec(context: Context, appWidgetId: Int): WidgetRenderSpec {
		val (widthDp, heightDp) = sizeDp(context, appWidgetId)
		return WidgetRenderSpec(
			style = WidgetSettings.getStyle(context, appWidgetId),
			widthDp = widthDp,
			heightDp = heightDp,
			group = WidgetSettings.getGroup(context, appWidgetId),
			isPreview = false
		)
	}

	/**
	 * [style]의 테마·배경 투명도를 적용하고, 적용한 테마를 돌려준다(위젯별로 더 칠할 게 있을 때 쓴다).
	 *
	 * 배경은 root의 background가 아니라 그 뒤에 깔아둔 ImageView(widget_bg)로 그린다. 뷰 전체의
	 * alpha를 낮추면 글자까지 같이 흐려지지만, 이미지뷰의 imageAlpha는 배경 그림에만 적용돼서
	 * 둥근 모서리·테두리는 그대로 두고 배경만 투명하게 만들 수 있다.
	 */
	fun applyTheme(context: Context, views: RemoteViews, style: WidgetStyle): WidgetTheme {
		val theme = style.theme
		val opacityPercent = 100 - style.transparencyPercent
		val accent = ContextCompat.getColor(context, theme.accentRes)

		// 배경은 리소스 한정자가 아니라 여기서 골라 넣는다(WidgetTheme 설명 참고).
		views.setImageViewResource(R.id.widget_bg, theme.backgroundRes)
		views.setInt(R.id.widget_bg, "setImageAlpha", opacityPercent * 255 / 100)
		views.setInt(R.id.widget_icon, "setColorFilter", accent)
		views.setTextColor(R.id.widget_title, accent)
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
		applyTheme(context, views, WidgetSettings.getStyle(context, appWidgetId))
		showMessage(views, "위젯을 불러오지 못했어요.\n눌러서 앱을 열어주세요")
		views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, appWidgetId))
		return views
	}

	/**
	 * 지금 위젯의 (가로, 세로) 크기를 dp로. 런처가 알려주는 값은 세로로 든 화면 기준과 가로로 든 화면
	 * 기준이 따로 있다: 세로 화면이면 (MIN_WIDTH, MAX_HEIGHT), 가로 화면이면 (MAX_WIDTH, MIN_HEIGHT)
	 * 가 실제 크기다. 예전에는 MIN_HEIGHT(가로 화면일 때의 낮은 높이)를 항상 써서, 세로 화면에서는
	 * 위젯이 실제보다 낮다고 보고 글자를 필요 이상으로 줄였다. 아직 알려주지 않은 경우엔 기본 3x2 크기로 본다.
	 */
	fun sizeDp(context: Context, appWidgetId: Int): Pair<Int, Int> {
		val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
		val portrait =
			context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
		val width = options.getInt(
			if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
			else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0
		).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
		val height = options.getInt(
			if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
			else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0
		).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
		return width to height
	}

	/** 본문 텍스트와, 위젯 크기에 맞춘 글자 크기·최대 줄 수를 한꺼번에 넣는다. */
	fun setBodyText(
		context: Context,
		views: RemoteViews,
		text: String,
		widthDp: Int,
		heightDp: Int,
		reserveDp: Int
	) {
		val fit = fitText(context, text, widthDp, heightDp, reserveDp)
		views.setTextViewText(R.id.widget_text, text)
		views.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, fit.sizeSp)
		views.setInt(R.id.widget_text, "setMaxLines", fit.maxLines)
	}

	/**
	 * 위젯의 본문 영역 안에 [text]가 다 들어가는 가장 큰 글자 크기를 찾는다. RemoteViews는 글자가
	 * 넘치는지 직접 재볼 수 없어서, 같은 글꼴·너비·줄 간격으로 StaticLayout에 미리 배치해보고 그 실제
	 * 높이로 판단한다(글자 수로 줄 수를 어림하던 예전 방식은 한글 폭·줄 높이 오차가 커서 여백이 남는데도
	 * 글자를 너무 줄였다).
	 *
	 * [MIN_TEXT_SP]에서도 다 안 들어가면 그 크기로 고정하고, 실제로 보이는 줄 수만큼만 maxLines로 줘서
	 * 마지막 줄이 말줄임(…)으로 끝나게 한다(글자가 줄 중간에서 뚝 잘리지 않도록).
	 *
	 * [reserveDp]는 본문 말고 위젯이 세로로 차지하는 공간(제목·위치 표기 줄)이다.
	 */
	private fun fitText(
		context: Context,
		text: String,
		widthDp: Int,
		heightDp: Int,
		reserveDp: Int
	): TextFit {
		val metrics = context.resources.displayMetrics
		val widthPx = ((widthDp - 2 * CONTENT_PADDING_H_DP - SAFETY_WIDTH_DP) * metrics.density)
			.toInt().coerceAtLeast(1)
		val heightPx =
			((heightDp - 2 * CONTENT_PADDING_V_DP - reserveDp - SAFETY_HEIGHT_DP) * metrics.density)
				.coerceAtLeast(1f)

		// 본문은 레이아웃에서 fontFamily="serif"(명조 계열)로 그려지므로, 재는 쪽도 같은 서체로 잰다.
		val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SERIF }
		var size = MAX_TEXT_SP
		while (true) {
			// sp → px 변환은 시스템 글꼴 크기 설정까지 반영하는 방식으로 한다(런처가 그릴 때와 같다).
			paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics)
			val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
				.setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
				.setIncludePad(true)
				.build()

			// 다 들어가면 그 크기로. 실제 줄바꿈이 예상과 한 줄 어긋나도 마지막 줄이 잘리지 않게 한 줄 여유를 둔다.
			if (layout.height <= heightPx) return TextFit(size, layout.lineCount + 1)

			if (size <= MIN_TEXT_SP) {
				var visibleLines = 0
				for (i in 0 until layout.lineCount) {
					if (layout.getLineBottom(i) <= heightPx) visibleLines = i + 1
				}
				return TextFit(size, maxOf(1, visibleLines))
			}
			size = maxOf(MIN_TEXT_SP, size - TEXT_STEP_SP)
		}
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