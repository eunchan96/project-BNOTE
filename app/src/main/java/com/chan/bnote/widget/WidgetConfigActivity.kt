package com.chan.bnote.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RemoteViews
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 위젯을 홈 화면에 올릴 때(그리고 위젯을 길게 눌러 "설정"을 고를 때) 뜨는 설정 화면.
 * 두 위젯이 같은 화면을 쓰고(테마 · 배경 투명도), 암송 위젯일 때만 "암송 그룹" 선택이 추가로 나온다.
 *
 * 화면 위쪽에 미리보기가 떠서, 테마·투명도·그룹을 바꾸는 즉시 위젯이 어떻게 보일지 확인할 수 있다.
 * 미리보기는 따로 만든 그림이 아니라 진짜 위젯과 같은 코드(buildViews)로 만든 RemoteViews를 이 화면에
 * 직접 펼쳐서(apply) 보여주기 때문에, 글자 크기·줄바꿈·색까지 홈 화면에서의 모습과 같다.
 *
 * 위젯 설정 화면은 시작할 때 결과를 RESULT_CANCELED로 두었다가 "완료"를 눌렀을 때만 RESULT_OK로
 * 바꿔야 한다 — 그래야 뒤로가기로 나갔을 때 시스템이 그 위젯 추가를 취소해준다. 또 설정 화면이 있는
 * 위젯은 시스템이 처음 그려주지 않으므로, 완료할 때 직접 한 번 그려줘야 한다.
 */
class WidgetConfigActivity : AppCompatActivity() {

	private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
	private var isMemorizationWidget = false
	private var groups: List<MemorizationGroup> = emptyList()
	private var groupsLoaded = false

	private lateinit var radioTheme: RadioGroup
	private lateinit var radioGroups: RadioGroup
	private lateinit var seekTransparency: SeekBar
	private lateinit var textTransparencyValue: TextView
	private lateinit var previewContainer: FrameLayout

	private var previewWidthDp = 0
	private var previewHeightDp = 0
	private var previewJob: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_widget_config)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.widget_config_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		appWidgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
		)
		setResult(
			RESULT_CANCELED,
			Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
		)
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish()
			return
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "위젯 설정"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val providerName = AppWidgetManager.getInstance(this)
			.getAppWidgetInfo(appWidgetId)?.provider?.className
		isMemorizationWidget = providerName == MemorizationWidgetProvider::class.java.name

		setupPreviewSize()

		radioTheme = findViewById(R.id.radio_theme)
		radioGroups = findViewById(R.id.radio_groups)
		radioTheme.check(
			if (WidgetSettings.getTheme(this, appWidgetId) == WidgetTheme.DARK) {
				R.id.radio_theme_dark
			} else {
				R.id.radio_theme_light
			}
		)
		radioTheme.setOnCheckedChangeListener { _, _ -> refreshPreview() }

		seekTransparency = findViewById(R.id.seek_transparency)
		textTransparencyValue = findViewById(R.id.text_transparency_value)
		seekTransparency.progress =
			WidgetSettings.getTransparency(this, appWidgetId) / TRANSPARENCY_STEP
		updateTransparencyLabel()
		seekTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				updateTransparencyLabel()
				refreshPreview()
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		if (isMemorizationWidget) {
			findViewById<View>(R.id.container_group_section).visibility = View.VISIBLE
			radioGroups.setOnCheckedChangeListener { _, _ -> refreshPreview() }
			loadGroups()
		}

		findViewById<TextView>(R.id.btn_widget_config_done).setOnClickListener { saveAndFinish() }

		refreshPreview()
	}

	/**
	 * 미리보기 영역의 크기를 실제 위젯 크기에 맞춘다. 위젯이 아주 큰 경우엔 설정 화면을 다 차지하지
	 * 않도록 상한을 둔다(이때는 정확한 크기가 아니라 대표적인 모습을 보여주는 셈이다).
	 */
	private fun setupPreviewSize() {
		val (widthDp, heightDp) = WidgetViews.sizeDp(this, appWidgetId)
		previewWidthDp = minOf(widthDp, MAX_PREVIEW_WIDTH_DP)
		previewHeightDp = minOf(heightDp, MAX_PREVIEW_HEIGHT_DP)

		previewContainer = findViewById(R.id.container_widget_preview)
		val params = previewContainer.layoutParams
		params.width = dp(previewWidthDp)
		params.height = dp(previewHeightDp)
		previewContainer.layoutParams = params
	}

	/** 지금 화면에서 고른 값(저장 전)으로 위젯을 만들어 미리보기에 펼친다. */
	private fun refreshPreview() {
		// 슬라이더를 빠르게 움직이면 요청이 여러 번 겹치니, 이전 것은 취소하고 마지막 것만 그린다.
		previewJob?.cancel()
		previewJob = lifecycleScope.launch {
			val spec = WidgetRenderSpec(
				style = currentStyle(),
				widthDp = previewWidthDp,
				heightDp = previewHeightDp,
				group = currentGroupChoice(),
				isPreview = true
			)
			val context = applicationContext
			val views: RemoteViews = try {
				if (isMemorizationWidget) {
					MemorizationWidget.buildViews(context, appWidgetId, spec)
				} else {
					TodayVerseWidget.buildViews(context, appWidgetId, spec)
				}
			} catch (e: CancellationException) {
				// 뒤이어 새 요청이 들어와서 취소된 것이니, 오류 화면을 그리지 말고 그대로 끝낸다.
				throw e
			} catch (e: Exception) {
				WidgetViews.errorViews(context, appWidgetId)
			}

			// 미리보기가 실패해도 설정 화면 전체가 죽으면 안 되니, 여기서 잡아서 안내 문구로 대신한다.
			try {
				// 반드시 액티비티(this)가 아니라 applicationContext로 펼친다. 액티비티의 LayoutInflater에는
				// AppCompat이 붙어 있어서 <TextView>·<ImageView>가 AppCompatTextView·AppCompatImageView로
				// 바뀌는데, 이 뷰들이 다시 정의한 메서드(setBackgroundResource, setImageResource 등)는
				// RemoteViews가 요구하는 @RemotableViewMethod 표시가 없어서 "can't use method with
				// RemoteViews" 오류로 앱이 죽는다. 홈 화면 런처에서는 AppCompat이 없어서 문제가 없다.
				val previewView = views.apply(context, previewContainer)
				previewContainer.removeAllViews()
				previewContainer.addView(previewView)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.e(TAG, "위젯 미리보기를 그리지 못했어요", e)
				showPreviewError(e)
			}
		}
	}

	private fun showPreviewError(e: Exception) {
		previewContainer.removeAllViews()
		previewContainer.addView(TextView(this).apply {
			text = "미리보기를 표시하지 못했어요\n(${e.javaClass.simpleName})"
			textSize = 12f
			gravity = Gravity.CENTER
			setTextColor(0xFFFFFFFF.toInt())
		})
	}

	private fun currentStyle() = WidgetStyle(
		theme = if (radioTheme.checkedRadioButtonId == R.id.radio_theme_dark) {
			WidgetTheme.DARK
		} else {
			WidgetTheme.LIGHT
		},
		transparencyPercent = seekTransparency.progress * TRANSPARENCY_STEP
	)

	/** 지금 고른 암송 그룹. 그룹 목록을 아직 못 불러왔으면 저장돼 있던 값을 쓴다. 전체 그룹이면 null. */
	private fun currentGroupChoice(): WidgetSettings.GroupChoice? {
		if (!isMemorizationWidget) return null
		if (!groupsLoaded) return WidgetSettings.getGroup(this, appWidgetId)
		val checked = radioGroups.findViewById<RadioButton>(radioGroups.checkedRadioButtonId)
		val groupId = checked?.tag as? Long ?: ALL_GROUPS
		return groups.firstOrNull { it.id == groupId }
			?.let { WidgetSettings.GroupChoice(it.id, it.name) }
	}

	private fun updateTransparencyLabel() {
		textTransparencyValue.text = "${seekTransparency.progress * TRANSPARENCY_STEP}%"
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			groups = db.memorizationVerseDao().getAllGroups()
			showGroupOptions()
			groupsLoaded = true
			refreshPreview()
		}
	}

	/** "전체 그룹" + 각 암송 그룹을 라디오 버튼으로 나열하고, 지금 저장된 선택을 체크해둔다. */
	private fun showGroupOptions() {
		radioGroups.removeAllViews()

		// 저장된 그룹이 삭제됐거나 id가 바뀌었을 수 있으니, 위젯 화면과 같은 방식으로(id → 이름) 찾는다.
		val saved = WidgetSettings.getGroup(this, appWidgetId)
		val selectedId = saved?.let { s ->
			(groups.firstOrNull { it.id == s.id } ?: groups.firstOrNull { it.name == s.name })?.id
		}

		val options = listOf<MemorizationGroup?>(null) + groups
		for (group in options) {
			// 위젯 테마 라디오 버튼과 똑같은 모양(높이·글자)이 되도록 같은 레이아웃으로 만든다.
			val button = layoutInflater.inflate(R.layout.item_widget_radio, radioGroups, false)
					as RadioButton
			button.id = View.generateViewId()
			button.text = group?.name ?: "전체 그룹"
			// 어느 그룹인지 저장 시점에 알 수 있게 그룹 id를 달아둔다(전체 그룹은 -1).
			button.tag = group?.id ?: ALL_GROUPS
			radioGroups.addView(button)
			if ((group?.id ?: ALL_GROUPS) == (selectedId ?: ALL_GROUPS)) button.isChecked = true
		}
	}

	private fun saveAndFinish() {
		val style = currentStyle()
		WidgetSettings.setTheme(this, appWidgetId, style.theme)
		WidgetSettings.setTransparency(this, appWidgetId, style.transparencyPercent)

		if (isMemorizationWidget) {
			val group = currentGroupChoice()

			// 다른 그룹을 골랐다면 그 그룹의 첫 구절부터 보이도록 "다음" 횟수를 되돌린다.
			val before = WidgetSettings.getGroup(this, appWidgetId)
			if (before?.id != group?.id) WidgetSettings.setOffset(this, appWidgetId, 0)
			WidgetSettings.setGroup(this, appWidgetId, group)
		}

		lifecycleScope.launch {
			WidgetUpdater.update(applicationContext, appWidgetId)
			WidgetUpdater.scheduleNextMidnight(applicationContext)
			setResult(
				RESULT_OK,
				Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
			)
			finish()
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private companion object {
		const val TAG = "WidgetConfig"
		const val ALL_GROUPS = -1L

		// 투명도 슬라이더는 10% 단위(0~10칸)로 움직인다.
		const val TRANSPARENCY_STEP = 10

		// 미리보기 영역의 최대 크기. 이보다 큰 위젯은 이 크기로 줄여서 보여준다.
		const val MAX_PREVIEW_WIDTH_DP = 340
		const val MAX_PREVIEW_HEIGHT_DP = 240
	}
}