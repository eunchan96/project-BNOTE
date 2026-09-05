package com.chan.bnote.ui.mypage.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
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
import com.chan.bnote.data.backup.AutoBackupManager
import com.chan.bnote.data.backup.BackupManager
import com.chan.bnote.notification.NotificationScheduler
import com.chan.bnote.ui.mypage.guide.UserGuideActivity
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
			if (!hasExactAlarmPermission()) {
				requestExactAlarmPermission()
			} else {
				scheduleFor(target)
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

	/** 자동 내보내기가 저장될 폴더를 한 번 골라두면, 그 다음부턴 매번 위치를 고르지 않고 그 폴더에
	 * 바로 저장한다(AutoBackupManager 참고). */
	private val autoBackupFolderLauncher = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree()
	) { uri ->
		if (uri != null) {
			AutoBackupManager.saveFolderUri(this, uri)
			refreshAutoBackupFolderLabel()
		}
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

		val switchDarkMode = findViewById<Switch>(R.id.switch_dark_mode).apply {
			isChecked = AppSettings.isDarkMode(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setDarkMode(this@SettingsActivity, checked)
				// 앱 전체(백스택의 다른 액티비티 포함)에 즉시 반영되도록 여기서 직접 적용한다.
				AppCompatDelegate.setDefaultNightMode(
					if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
				)
			}
		}
		findViewById<View>(R.id.row_dark_mode_toggle).setOnClickListener {
			switchDarkMode.isChecked = !switchDarkMode.isChecked
		}

		val switchKeepScreenOn = findViewById<Switch>(R.id.switch_keep_screen_on).apply {
			isChecked = AppSettings.isKeepScreenOn(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setKeepScreenOn(this@SettingsActivity, checked)
				applyKeepScreenOn(checked)
			}
		}
		findViewById<View>(R.id.row_keep_screen_on_toggle).setOnClickListener {
			switchKeepScreenOn.isChecked = !switchKeepScreenOn.isChecked
		}

		val switchChapterSwipe = findViewById<Switch>(R.id.switch_chapter_swipe).apply {
			isChecked = AppSettings.isChapterSwipeEnabled(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setChapterSwipeEnabled(this@SettingsActivity, checked)
			}
		}
		findViewById<View>(R.id.row_chapter_swipe_toggle).setOnClickListener {
			switchChapterSwipe.isChecked = !switchChapterSwipe.isChecked
		}

		val switchBibleScrollbar = findViewById<Switch>(R.id.switch_bible_scrollbar).apply {
			isChecked = AppSettings.isBibleScrollbarVisible(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setBibleScrollbarVisible(this@SettingsActivity, checked)
			}
		}
		findViewById<View>(R.id.row_bible_scrollbar_toggle).setOnClickListener {
			switchBibleScrollbar.isChecked = !switchBibleScrollbar.isChecked
		}

		val switchReadingCheckPosition =
			findViewById<Switch>(R.id.switch_reading_check_position).apply {
				isChecked = AppSettings.isReadingCheckBottomButtonMode(this@SettingsActivity)
				setOnCheckedChangeListener { _, checked ->
					AppSettings.setReadingCheckBottomButtonMode(this@SettingsActivity, checked)
				}
			}
		findViewById<View>(R.id.row_reading_check_position_toggle).setOnClickListener {
			switchReadingCheckPosition.isChecked = !switchReadingCheckPosition.isChecked
		}

		val switchCopySecondary = findViewById<Switch>(R.id.switch_copy_secondary).apply {
			isChecked = AppSettings.isCopyIncludeSecondary(this@SettingsActivity)
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setCopyIncludeSecondary(this@SettingsActivity, checked)
			}
		}
		findViewById<View>(R.id.row_copy_secondary_toggle).setOnClickListener {
			switchCopySecondary.isChecked = !switchCopySecondary.isChecked
		}

		findViewById<TextView>(R.id.btn_copy_reference_style).setOnClickListener {
			com.chan.bnote.ui.bible.CopyFormatPickerBottomSheet()
				.show(supportFragmentManager, "copy_format_picker")
		}

		findViewById<TextView>(R.id.btn_export_data).setOnClickListener {
			val fileName = "bnote_backup_${System.currentTimeMillis()}.zip"
			exportDataLauncher.launch(fileName)
		}
		findViewById<TextView>(R.id.btn_import_data).setOnClickListener {
			importDataLauncher.launch(arrayOf("application/zip"))
		}

		setupAutoBackupUi()

		findViewById<TextView>(R.id.menu_user_guide).setOnClickListener {
			startActivity(Intent(this, UserGuideActivity::class.java))
		}
		findViewById<TextView>(R.id.menu_app_info).setOnClickListener {
			startActivity(Intent(this, AppInfoActivity::class.java))
		}

		setupNotificationSettings()
	}

	/** (일수, 버튼/목록에 보여줄 짧은 이름) 쌍. AppSettings에는 일수로 저장한다. */
	private val autoBackupIntervalOptions = listOf(
		7 to "1주",
		14 to "2주",
		30 to "1개월",
		90 to "3개월",
		180 to "6개월",
		365 to "1년"
	)

	private fun setupAutoBackupUi() {
		val switchAutoBackup = findViewById<Switch>(R.id.switch_auto_backup)
		val btnInterval = findViewById<TextView>(R.id.btn_auto_backup_interval)
		val rowFolder = findViewById<View>(R.id.row_auto_backup_folder)

		fun updateIntervalLabel() {
			val days = AppSettings.getAutoBackupIntervalDays(this)
			val label = autoBackupIntervalOptions.firstOrNull { it.first == days }?.second ?: "1개월"
			// 매일 말씀 알림·통독 리마인더의 시간 버튼("오전 8:00 ▾")과 같은 형식으로 통일.
			btnInterval.text = "$label ▾"
		}

		fun refreshFolderRowVisibility(enabled: Boolean) {
			rowFolder.visibility = if (enabled) View.VISIBLE else View.GONE
		}

		switchAutoBackup.isChecked = AppSettings.isAutoBackupEnabled(this)
		updateIntervalLabel()
		refreshAutoBackupFolderLabel()
		refreshFolderRowVisibility(switchAutoBackup.isChecked)

		switchAutoBackup.setOnCheckedChangeListener { _, checked ->
			AppSettings.setAutoBackupEnabled(this, checked)
			refreshFolderRowVisibility(checked)
			// 처음 켤 때 폴더를 아직 안 골라뒀으면, 그 자리에서 바로 고르게 한다.
			if (checked && AutoBackupManager.getFolderUri(this) == null) {
				autoBackupFolderLauncher.launch(null)
			}
		}
		findViewById<View>(R.id.row_auto_backup_toggle).setOnClickListener {
			switchAutoBackup.isChecked = !switchAutoBackup.isChecked
		}

		rowFolder.setOnClickListener {
			autoBackupFolderLauncher.launch(null)
		}

		btnInterval.setOnClickListener {
			val labels = autoBackupIntervalOptions.map { it.second }.toTypedArray()
			val currentDays = AppSettings.getAutoBackupIntervalDays(this)
			val currentIndex = autoBackupIntervalOptions.indexOfFirst { it.first == currentDays }
				.coerceAtLeast(0)

			MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("자동 내보내기 주기")
				.setSingleChoiceItems(labels, currentIndex) { dialog, which ->
					val chosenDays = autoBackupIntervalOptions[which].first
					AppSettings.setAutoBackupIntervalDays(this, chosenDays)
					updateIntervalLabel()
					dialog.dismiss()
				}
				.setNegativeButton("취소", null)
				.show()
		}
	}

	private fun refreshAutoBackupFolderLabel() {
		val textFolder = findViewById<TextView>(R.id.text_auto_backup_folder)
		val folderUri = AutoBackupManager.getFolderUri(this)
		textFolder.text = if (folderUri != null) {
			AutoBackupManager.displayNameFor(folderUri)
		} else {
			"선택 안 함"
		}
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

		findViewById<View>(R.id.row_daily_verse_toggle).setOnClickListener {
			switchDailyVerseNoti.isChecked = !switchDailyVerseNoti.isChecked
		}
		findViewById<View>(R.id.row_reading_reminder_toggle).setOnClickListener {
			switchReadingReminder.isChecked = !switchReadingReminder.isChecked
		}
	}

	/** [target]은 "daily_verse" 또는 "reading_reminder". 필요한 권한이 다 있으면 바로 예약하고,
	 * 없으면 순서대로(알림 권한 → 정확 알람 권한) 요청한다. */
	private fun enableNotification(target: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
			pendingNotiTarget = target
			notiPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
			return
		}
		if (!hasExactAlarmPermission()) {
			// 정확 알람 권한 설정 화면은 결과 콜백이 따로 없는 시스템 설정 화면이라, 여기서 막고
			// 사용자가 허용하고 돌아왔을 때는 onResume()에서 다시 확인해 예약한다.
			requestExactAlarmPermission()
			return
		}
		scheduleFor(target)
	}

	private fun scheduleFor(target: String) {
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

	/** 정확한 시각에 알림을 보내기 위한 권한. Android 12 미만은 항상 있는 것으로 취급한다. */
	private fun hasExactAlarmPermission(): Boolean {
		return NotificationScheduler.canScheduleExactAlarms(this)
	}

	/** Android 12+에서 정확 알람 권한을 요청하는 시스템 설정 화면으로 이동시킨다. */
	private fun requestExactAlarmPermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
		Toast.makeText(
			this,
			"정확한 시각에 알림을 보내려면 '정확한 알람' 권한이 필요해요. 다음 화면에서 BNOTE를 허용해주세요.",
			Toast.LENGTH_LONG
		).show()
		try {
			startActivity(
				Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
					data = android.net.Uri.parse("package:$packageName")
				}
			)
		} catch (e: Exception) {
			// 일부 기기/OS 버전엔 이 설정 화면 자체가 없을 수 있음 — 조용히 무시.
		}
	}

	override fun onResume() {
		super.onResume()
		// 알림 권한 · 정확 알람 권한 설정 화면에 다녀왔을 수 있으니, 스위치가 켜져 있는데 아직
		// 실제로 예약이 안 됐을 수 있는 알림은 여기서 다시 확인해 예약해준다(이미 예약돼 있어도
		// 같은 시각으로 다시 예약하는 것뿐이라 안전하다).
		if (::switchDailyVerseNoti.isInitialized && switchDailyVerseNoti.isChecked &&
			hasNotificationPermission() && hasExactAlarmPermission()
		) {
			scheduleFor("daily_verse")
		}
		if (::switchReadingReminder.isInitialized && switchReadingReminder.isChecked &&
			hasNotificationPermission() && hasExactAlarmPermission()
		) {
			scheduleFor("reading_reminder")
		}
	}

	private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
		TimePickerDialog(this, { _, h, m -> onSet(h, m) }, hour, minute, false).show()
	}

	private fun updateDailyVerseTimeLabel() {
		val (h, m) = AppSettings.getDailyVerseNotiTime(this)
		btnDailyVerseTime.text = "${formatTime(h, m)} ▾"
	}

	private fun updateReadingReminderTimeLabel() {
		val (h, m) = AppSettings.getReadingReminderTime(this)
		btnReadingReminderTime.text = "${formatTime(h, m)} ▾"
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
}