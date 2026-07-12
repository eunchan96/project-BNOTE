package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuBottomSheet : BottomSheetDialogFragment() {

	// 글자 크기 변경 시 즉시 반영하기 위한 콜백
	var onFontSizeChanged: ((Int) -> Unit)? = null

	// 다크모드 변경 시 액티비티 재시작하기 위한 콜백
	var onDarkModeChanged: ((Boolean) -> Unit)? = null

	private lateinit var fontSizeText: TextView
	private var currentFontSize = AppSettings.MIN_FONT_SIZE

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_menu, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		currentFontSize = AppSettings.getFontSize(requireContext())
		fontSizeText = view.findViewById(R.id.text_font_size)
		fontSizeText.text = "${currentFontSize}sp"

		view.findViewById<TextView>(R.id.btn_font_minus).setOnClickListener {
			currentFontSize = (currentFontSize - 2).coerceAtLeast(AppSettings.MIN_FONT_SIZE)
			applyFontSize()
		}
		view.findViewById<TextView>(R.id.btn_font_plus).setOnClickListener {
			currentFontSize = (currentFontSize + 2).coerceAtMost(AppSettings.MAX_FONT_SIZE)
			applyFontSize()
		}

		val darkSwitch = view.findViewById<Switch>(R.id.switch_dark_mode)
		darkSwitch.isChecked = AppSettings.isDarkMode(requireContext())
		darkSwitch.setOnCheckedChangeListener { _, isChecked ->
			AppSettings.setDarkMode(requireContext(), isChecked)
			onDarkModeChanged?.invoke(isChecked)
		}

		view.findViewById<TextView>(R.id.menu_reading_plan).setOnClickListener {
			Toast.makeText(requireContext(), "통독 관리 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		view.findViewById<TextView>(R.id.menu_sermon_note).setOnClickListener {
			Toast.makeText(requireContext(), "설교 노트 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
	}

	private fun applyFontSize() {
		fontSizeText.text = "${currentFontSize}sp"
		AppSettings.setFontSize(requireContext(), currentFontSize)
		onFontSizeChanged?.invoke(currentFontSize)
	}
}