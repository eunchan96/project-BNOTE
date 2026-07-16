package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

	private lateinit var fontSizeText: TextView
	private lateinit var scrollSpeedText: TextView
	private var currentFontSize = 16
	private var currentScrollSpeed = 3

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_settings)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "설정"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		currentFontSize = AppSettings.getFontSize(this)
		currentScrollSpeed = AppSettings.getScrollSpeed(this)

		fontSizeText = findViewById(R.id.text_font_size)
		scrollSpeedText = findViewById(R.id.text_scroll_speed)
		fontSizeText.text = "${currentFontSize}sp"
		scrollSpeedText.text = "$currentScrollSpeed"

		findViewById<TextView>(R.id.btn_font_minus).setOnClickListener {
			currentFontSize = (currentFontSize - 2).coerceAtLeast(AppSettings.MIN_FONT_SIZE)
			fontSizeText.text = "${currentFontSize}sp"
			AppSettings.setFontSize(this, currentFontSize)
		}
		findViewById<TextView>(R.id.btn_font_plus).setOnClickListener {
			currentFontSize = (currentFontSize + 2).coerceAtMost(AppSettings.MAX_FONT_SIZE)
			fontSizeText.text = "${currentFontSize}sp"
			AppSettings.setFontSize(this, currentFontSize)
		}

		findViewById<TextView>(R.id.btn_speed_minus).setOnClickListener {
			currentScrollSpeed = (currentScrollSpeed - 1).coerceAtLeast(1)
			scrollSpeedText.text = "$currentScrollSpeed"
			AppSettings.setScrollSpeed(this, currentScrollSpeed)
		}
		findViewById<TextView>(R.id.btn_speed_plus).setOnClickListener {
			currentScrollSpeed = (currentScrollSpeed + 1).coerceAtMost(5)
			scrollSpeedText.text = "$currentScrollSpeed"
			AppSettings.setScrollSpeed(this, currentScrollSpeed)
		}

		findViewById<Switch>(R.id.switch_dark_mode).apply {
			isChecked = AppSettings.isDarkMode(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setDarkMode(this@SettingsActivity, checked)
				// 앱 전체(백스택의 다른 액티비티 포함)에 즉시 반영되도록 여기서 직접 적용한다.
				AppCompatDelegate.setDefaultNightMode(
					if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
				)
			}
		}

		findViewById<Switch>(R.id.switch_keep_screen_on).apply {
			isChecked = AppSettings.isKeepScreenOn(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setKeepScreenOn(this@SettingsActivity, checked)
				applyKeepScreenOn(checked)
			}
		}

		findViewById<Switch>(R.id.switch_copy_secondary).apply {
			isChecked = AppSettings.isCopyIncludeSecondary(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setCopyIncludeSecondary(this@SettingsActivity, checked)
			}
		}

		val btnReferenceStyle = findViewById<TextView>(R.id.btn_copy_reference_style)
		updateReferenceStyleLabel(btnReferenceStyle)
		btnReferenceStyle.setOnClickListener {
			val popup = PopupMenu(this, btnReferenceStyle)
			popup.menu.add(0, 0, 0, "표기 안 함")
			popup.menu.add(0, 1, 1, "짧게 (창 1:1)")
			popup.menu.add(0, 2, 2, "길게 (창세기 1장 1절)")
			popup.setOnMenuItemClickListener { item ->
				val style = when (item.itemId) {
					1 -> "SHORT"
					2 -> "LONG"
					else -> "NONE"
				}
				AppSettings.setCopyReferenceStyle(this, style)
				updateReferenceStyleLabel(btnReferenceStyle)
				true
			}
			popup.show()
		}

		findViewById<TextView>(R.id.menu_download_translation).setOnClickListener {
			Toast.makeText(this, "대역본 다운로드 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		findViewById<TextView>(R.id.menu_theme_color).setOnClickListener {
			Toast.makeText(this, "테마 및 글자 색상 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		findViewById<TextView>(R.id.menu_cloud_sync).setOnClickListener {
			Toast.makeText(
				this,
				"클라우드 동기화는 로컬 전용 앱 특성상 추후 검토 예정이에요",
				Toast.LENGTH_SHORT
			).show()
		}

		findViewById<TextView>(R.id.btn_reset_reading_progress).setOnClickListener {
			MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("성경읽기표 기록 초기화")
				.setMessage("지금까지 읽음 표시한 모든 기록이 사라져요. 계속할까요?")
				.setPositiveButton("초기화") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.readingProgressDao().resetAll()
						Toast.makeText(this@SettingsActivity, "초기화됐어요", Toast.LENGTH_SHORT).show()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}
	}

	private fun applyKeepScreenOn(enabled: Boolean) {
		if (enabled) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	private fun updateReferenceStyleLabel(button: TextView) {
		button.text = when (AppSettings.getCopyReferenceStyle(this)) {
			"SHORT" -> "짧게 (창 1:1) ▾"
			"LONG" -> "길게 (창세기 1장 1절) ▾"
			else -> "표기 안 함 ▾"
		}
	}
}