package com.chan.bnote.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import kotlinx.coroutines.launch

/**
 * 위젯을 홈 화면에 올릴 때(그리고 위젯을 길게 눌러 "설정"을 고를 때) 뜨는 설정 화면.
 * 두 위젯이 같은 화면을 쓰고, 암송 위젯일 때만 "암송 그룹" 선택이 추가로 나온다.
 *
 * 위젯 설정 화면은 시작할 때 결과를 RESULT_CANCELED로 두었다가 "완료"를 눌렀을 때만 RESULT_OK로
 * 바꿔야 한다 — 그래야 뒤로가기로 나갔을 때 시스템이 그 위젯 추가를 취소해준다. 또 설정 화면이 있는
 * 위젯은 시스템이 처음 그려주지 않으므로, 완료할 때 직접 한 번 그려줘야 한다.
 */
class WidgetConfigActivity : AppCompatActivity() {

	private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
	private var isMemorizationWidget = false
	private var groups: List<MemorizationGroup> = emptyList()

	private lateinit var radioTheme: RadioGroup
	private lateinit var radioGroups: RadioGroup

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

		radioTheme = findViewById(R.id.radio_theme)
		radioGroups = findViewById(R.id.radio_groups)
		radioTheme.check(
			if (WidgetSettings.getTheme(this, appWidgetId) == WidgetTheme.DARK) {
				R.id.radio_theme_dark
			} else {
				R.id.radio_theme_light
			}
		)

		val providerName = AppWidgetManager.getInstance(this)
			.getAppWidgetInfo(appWidgetId)?.provider?.className
		isMemorizationWidget = providerName == MemorizationWidgetProvider::class.java.name
		if (isMemorizationWidget) {
			findViewById<View>(R.id.container_group_section).visibility = View.VISIBLE
			loadGroups()
		}

		findViewById<TextView>(R.id.btn_widget_config_done).setOnClickListener { saveAndFinish() }
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			groups = db.memorizationVerseDao().getAllGroups()
			showGroupOptions()
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
			val button = RadioButton(this).apply {
				id = View.generateViewId()
				text = group?.name ?: "전체 그룹"
				textSize = 16f
				setTextColor(ContextCompat.getColor(context, R.color.text_primary))
				// 어느 그룹인지 저장 시점에 알 수 있게 그룹 id를 달아둔다(전체 그룹은 -1).
				tag = group?.id ?: ALL_GROUPS
			}
			radioGroups.addView(button)
			if ((group?.id ?: ALL_GROUPS) == (selectedId ?: ALL_GROUPS)) button.isChecked = true
		}
	}

	private fun saveAndFinish() {
		val theme = if (radioTheme.checkedRadioButtonId == R.id.radio_theme_dark) {
			WidgetTheme.DARK
		} else {
			WidgetTheme.LIGHT
		}
		WidgetSettings.setTheme(this, appWidgetId, theme)

		if (isMemorizationWidget) {
			val checked = radioGroups.findViewById<RadioButton>(radioGroups.checkedRadioButtonId)
			val groupId = checked?.tag as? Long ?: ALL_GROUPS
			val group = groups.firstOrNull { it.id == groupId }

			// 다른 그룹을 골랐다면 그 그룹의 첫 구절부터 보이도록 "다음" 횟수를 되돌린다.
			val before = WidgetSettings.getGroup(this, appWidgetId)
			if (before?.id != group?.id) WidgetSettings.setOffset(this, appWidgetId, 0)
			WidgetSettings.setGroup(
				this, appWidgetId, group?.let { WidgetSettings.GroupChoice(it.id, it.name) }
			)
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

	private companion object {
		const val ALL_GROUPS = -1L
	}
}