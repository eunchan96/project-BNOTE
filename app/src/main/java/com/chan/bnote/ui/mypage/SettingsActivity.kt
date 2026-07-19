package com.chan.bnote.ui.mypage

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.backup.BackupManager
import com.chan.bnote.notification.NotificationScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

	private lateinit var fontSizeText: TextView
	private lateinit var scrollSpeedText: TextView
	private var currentFontSize = 16
	private var currentScrollSpeed = 3

	private lateinit var switchDailyVerseNoti: Switch
	private lateinit var switchReadingReminder: Switch
	private lateinit var btnDailyVerseTime: TextView
	private lateinit var btnReadingReminderTime: TextView

	// 알림 권한 요청 중 어느 스위치가 요청했는지 구분하기 위한 값 ("daily_verse" | "reading_reminder")
	private var pendingNotiTarget: String? = null

	private val notiPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		val target = pendingNotiTarget
		pendingNotiTarget = null
		if (target == null) return@registerForActivityResult

		if (granted) {
			when (target) {
				"daily_verse" -> {
					val (h, m) = AppSettings.getDailyVerseNotiTime(this)
					NotificationScheduler.scheduleDailyVerse(this, h, m)
				}

				"reading_reminder" -> {
					val (h, m) = AppSettings.getReadingReminderTime(this)
					NotificationScheduler.scheduleReadingReminder(this, h, m)
				}
			}
		} else {
			// 권한을 거부하면 토글을 다시 꺼서 실제 상태와 화면을 일치시킨다.
			Toast.makeText(this, "알림 권한이 없으면 알림을 보낼 수 없어요", Toast.LENGTH_SHORT).show()
			when (target) {
				"daily_verse" -> {
					AppSettings.setDailyVerseNotiEnabled(this, false)
					switchDailyVerseNoti.isChecked = false
				}

				"reading_reminder" -> {
					AppSettings.setReadingReminderEnabled(this, false)
					switchReadingReminder.isChecked = false
				}
			}
		}
	}

	private val exportDataLauncher = registerForActivityResult(
		ActivityResultContracts.CreateDocument("application/zip")
	) { uri ->
		if (uri == null) return@registerForActivityResult
		lifecycleScope.launch {
			try {
				BackupManager.export(this@SettingsActivity, uri)
				Toast.makeText(this@SettingsActivity, "내보내기가 완료됐어요", Toast.LENGTH_SHORT).show()
			} catch (e: Exception) {
				Toast.makeText(
					this@SettingsActivity,
					"내보내기에 실패했어요: ${e.message}",
					Toast.LENGTH_LONG
				).show()
			}
		}
	}

	private val importDataLauncher = registerForActivityResult(
		ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) confirmImport(uri)
	}

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

		findViewById<TextView>(R.id.btn_export_data).setOnClickListener {
			val fileName = "bnote_backup_${System.currentTimeMillis()}.zip"
			exportDataLauncher.launch(fileName)
		}
		findViewById<TextView>(R.id.btn_import_data).setOnClickListener {
			importDataLauncher.launch(arrayOf("application/zip"))
		}
		findViewById<TextView>(R.id.menu_user_guide).setOnClickListener {
			startActivity(Intent(this, UserGuideActivity::class.java))
		}
		findViewById<TextView>(R.id.menu_app_info).setOnClickListener {
			startActivity(Intent(this, AppInfoActivity::class.java))
		}

		setupNotificationSettings()
	}

	private fun confirmImport(uri: android.net.Uri) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("데이터 불러오기")
			.setMessage("지금 앱에 있는 데이터가 전부 이 백업 파일의 내용으로 교체돼요. 계속할까요?")
			.setPositiveButton("불러오기") { _, _ ->
				lifecycleScope.launch {
					try {
						BackupManager.import(this@SettingsActivity, uri)
						Toast.makeText(this@SettingsActivity, "불러오기가 완료됐어요", Toast.LENGTH_SHORT)
							.show()
					} catch (e: Exception) {
						Toast.makeText(
							this@SettingsActivity,
							"불러오기에 실패했어요: ${e.message}",
							Toast.LENGTH_LONG
						).show()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun setupNotificationSettings() {
		switchDailyVerseNoti = findViewById(R.id.switch_daily_verse_noti)
		switchReadingReminder = findViewById(R.id.switch_reading_reminder)
		btnDailyVerseTime = findViewById(R.id.btn_daily_verse_time)
		btnReadingReminderTime = findViewById(R.id.btn_reading_reminder_time)

		updateDailyVerseTimeLabel()
		updateReadingReminderTimeLabel()

		switchDailyVerseNoti.isChecked = AppSettings.isDailyVerseNotiEnabled(this)
		switchDailyVerseNoti.setOnCheckedChangeListener { _, checked ->
			AppSettings.setDailyVerseNotiEnabled(this, checked)
			if (checked) {
				enableNotification("daily_verse")
			} else {
				NotificationScheduler.cancelDailyVerse(this)
			}
		}
		btnDailyVerseTime.setOnClickListener {
			val (hour, minute) = AppSettings.getDailyVerseNotiTime(this)
			showTimePicker(hour, minute) { h, m ->
				AppSettings.setDailyVerseNotiTime(this, h, m)
				updateDailyVerseTimeLabel()
				if (switchDailyVerseNoti.isChecked) {
					NotificationScheduler.scheduleDailyVerse(this, h, m)
				}
			}
		}

		switchReadingReminder.isChecked = AppSettings.isReadingReminderEnabled(this)
		switchReadingReminder.setOnCheckedChangeListener { _, checked ->
			AppSettings.setReadingReminderEnabled(this, checked)
			if (checked) {
				enableNotification("reading_reminder")
			} else {
				NotificationScheduler.cancelReadingReminder(this)
			}
		}
		btnReadingReminderTime.setOnClickListener {
			val (hour, minute) = AppSettings.getReadingReminderTime(this)
			showTimePicker(hour, minute) { h, m ->
				AppSettings.setReadingReminderTime(this, h, m)
				updateReadingReminderTimeLabel()
				if (switchReadingReminder.isChecked) {
					NotificationScheduler.scheduleReadingReminder(this, h, m)
				}
			}
		}
	}

	/** [target]은 "daily_verse" 또는 "reading_reminder". 권한이 있으면 바로 예약하고, 없으면 요청한다. */
	private fun enableNotification(target: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
			pendingNotiTarget = target
			notiPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
			return
		}
		when (target) {
			"daily_verse" -> {
				val (h, m) = AppSettings.getDailyVerseNotiTime(this)
				NotificationScheduler.scheduleDailyVerse(this, h, m)
			}

			"reading_reminder" -> {
				val (h, m) = AppSettings.getReadingReminderTime(this)
				NotificationScheduler.scheduleReadingReminder(this, h, m)
			}
		}
	}

	private fun hasNotificationPermission(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
		return ContextCompat.checkSelfPermission(
			this, Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED
	}

	private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
		TimePickerDialog(this, { _, h, m -> onSet(h, m) }, hour, minute, false).show()
	}

	private fun updateDailyVerseTimeLabel() {
		val (h, m) = AppSettings.getDailyVerseNotiTime(this)
		btnDailyVerseTime.text = formatTime(h, m)
	}

	private fun updateReadingReminderTimeLabel() {
		val (h, m) = AppSettings.getReadingReminderTime(this)
		btnReadingReminderTime.text = formatTime(h, m)
	}

	private fun formatTime(hour: Int, minute: Int): String {
		val period = if (hour < 12) "오전" else "오후"
		val hour12 = when {
			hour == 0 -> 12
			hour > 12 -> hour - 12
			else -> hour
		}
		return String.format(Locale.KOREA, "%s %d:%02d", period, hour12, minute)
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