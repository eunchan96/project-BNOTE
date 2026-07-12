package com.chan.bnote.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import kotlinx.coroutines.launch

class SettingsBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.75f

	private lateinit var fontSizeText: TextView
	private lateinit var scrollSpeedText: TextView
	private var currentFontSize = 16
	private var currentScrollSpeed = 3

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_settings, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		currentFontSize = AppSettings.getFontSize(requireContext())
		currentScrollSpeed = AppSettings.getScrollSpeed(requireContext())

		fontSizeText = view.findViewById(R.id.text_font_size)
		scrollSpeedText = view.findViewById(R.id.text_scroll_speed)
		fontSizeText.text = "${currentFontSize}sp"
		scrollSpeedText.text = "$currentScrollSpeed"

		view.findViewById<TextView>(R.id.btn_font_minus).setOnClickListener {
			currentFontSize = (currentFontSize - 2).coerceAtLeast(AppSettings.MIN_FONT_SIZE)
			fontSizeText.text = "${currentFontSize}sp"
			AppSettings.setFontSize(requireContext(), currentFontSize)
		}
		view.findViewById<TextView>(R.id.btn_font_plus).setOnClickListener {
			currentFontSize = (currentFontSize + 2).coerceAtMost(AppSettings.MAX_FONT_SIZE)
			fontSizeText.text = "${currentFontSize}sp"
			AppSettings.setFontSize(requireContext(), currentFontSize)
		}

		view.findViewById<TextView>(R.id.btn_speed_minus).setOnClickListener {
			currentScrollSpeed = (currentScrollSpeed - 1).coerceAtLeast(1)
			scrollSpeedText.text = "$currentScrollSpeed"
			AppSettings.setScrollSpeed(requireContext(), currentScrollSpeed)
		}
		view.findViewById<TextView>(R.id.btn_speed_plus).setOnClickListener {
			currentScrollSpeed = (currentScrollSpeed + 1).coerceAtMost(5)
			scrollSpeedText.text = "$currentScrollSpeed"
			AppSettings.setScrollSpeed(requireContext(), currentScrollSpeed)
		}

		view.findViewById<Switch>(R.id.switch_dark_mode).apply {
			isChecked = AppSettings.isDarkMode(requireContext())
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setDarkMode(requireContext(), checked)
				requireActivity().recreate()
			}
		}

		view.findViewById<Switch>(R.id.switch_keep_screen_on).apply {
			isChecked = AppSettings.isKeepScreenOn(requireContext())
			setOnCheckedChangeListener { _, checked ->
				AppSettings.setKeepScreenOn(requireContext(), checked)
				applyKeepScreenOn(checked)
			}
		}

		view.findViewById<TextView>(R.id.menu_download_translation).setOnClickListener {
			Toast.makeText(requireContext(), "대역본 다운로드 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		view.findViewById<TextView>(R.id.menu_theme_color).setOnClickListener {
			Toast.makeText(requireContext(), "테마 및 글자 색상 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		view.findViewById<TextView>(R.id.menu_cloud_sync).setOnClickListener {
			Toast.makeText(
				requireContext(),
				"클라우드 동기화는 로컬 전용 앱 특성상 추후 검토 예정이에요",
				Toast.LENGTH_SHORT
			).show()
		}

		view.findViewById<TextView>(R.id.btn_reset_reading_progress).setOnClickListener {
			AlertDialog.Builder(requireContext())
				.setTitle("성경읽기표 기록 초기화")
				.setMessage("지금까지 읽음 표시한 모든 기록이 사라져요. 계속할까요?")
				.setPositiveButton("초기화") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						db.readingProgressDao().resetAll()
						Toast.makeText(requireContext(), "초기화됐어요", Toast.LENGTH_SHORT).show()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}
	}

	private fun applyKeepScreenOn(enabled: Boolean) {
		if (enabled) {
			requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}
}