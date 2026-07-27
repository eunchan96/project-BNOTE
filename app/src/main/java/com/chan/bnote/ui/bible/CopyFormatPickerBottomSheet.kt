package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.CopyFormatConfig
import com.chan.bnote.data.mypage.CopyFormatPreset
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 저장된 복사 형식들을 보여주고, 탭하면 바로 적용한다. 추가/수정은 CopyFormatBottomSheet로 넘어간다. */
class CopyFormatPickerBottomSheet : BottomSheetDialogFragment() {

	private lateinit var presetsContainer: LinearLayout
	private var isManageMode = false

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_copy_format_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		presetsContainer = view.findViewById(R.id.container_copy_format_presets)

		view.findViewById<TextView>(R.id.btn_manage_copy_formats).setOnClickListener {
			isManageMode = !isManageMode
			it as TextView
			it.text = if (isManageMode) "완료" else "관리"
			loadPresets()
		}

		view.findViewById<TextView>(R.id.btn_add_copy_format).setOnClickListener {
			openEditor(existingPresetId = null)
		}

		loadPresets()
	}

	private fun loadPresets() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			seedDefaultPresetsIfNeeded(db)
			val presets = db.copyFormatPresetDao().getAll()
			val activeConfigJson = AppSettings.getActiveCopyFormat(requireContext()).toJson()

			presetsContainer.removeAllViews()
			requireView().findViewById<TextView>(R.id.text_no_copy_format_presets).visibility =
				if (presets.isEmpty()) View.VISIBLE else View.GONE

			for (preset in presets) {
				val row = LayoutInflater.from(requireContext())
					.inflate(R.layout.item_copy_format_preset_row, presetsContainer, false)

				row.findViewById<TextView>(R.id.text_preset_name).text = preset.name
				row.findViewById<TextView>(R.id.text_preset_active_badge).visibility =
					if (preset.configJson == activeConfigJson) View.VISIBLE else View.GONE

				val editBtn = row.findViewById<ImageView>(R.id.btn_edit_preset)
				val deleteBtn = row.findViewById<ImageView>(R.id.btn_delete_preset)
				editBtn.visibility = if (isManageMode) View.VISIBLE else View.GONE
				deleteBtn.visibility = if (isManageMode) View.VISIBLE else View.GONE

				row.setOnClickListener {
					if (isManageMode) return@setOnClickListener
					AppSettings.setActiveCopyFormat(requireContext(), preset.toConfig())
					dismiss()
				}
				editBtn.setOnClickListener { openEditor(existingPresetId = preset.id) }
				deleteBtn.setOnClickListener { confirmDelete(db, preset) }

				presetsContainer.addView(row)
			}
		}
	}

	private fun confirmDelete(db: BibleDatabase, preset: CopyFormatPreset) {
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("형식 삭제")
			.setMessage("'${preset.name}' 형식을 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					db.copyFormatPresetDao().delete(preset)
					loadPresets()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun openEditor(existingPresetId: Long?) {
		val editor = CopyFormatBottomSheet()
		editor.existingPresetId = existingPresetId
		editor.onSaved = { loadPresets() }
		editor.show(parentFragmentManager, "copy_format_editor")
	}

	/** 기본으로 제공하는 형식 3개. 처음 한 번만 심고, 그 뒤로는 자유롭게 수정 · 삭제할 수 있는 그냥
	 * 평범한 프리셋이다(다시 자동으로 채워지지 않는다). */
	private suspend fun seedDefaultPresetsIfNeeded(db: BibleDatabase) {
		val prefs = requireContext().getSharedPreferences(
			"copy_format_prefs",
			android.content.Context.MODE_PRIVATE
		)
		if (prefs.getBoolean("default_presets_seeded", false)) return

		val defaults = listOf(
			"기본1" to CopyFormatConfig(
				refVerseSeparator = CopyFormatConfig.Separator.NEWLINE,
				multiVerseSeparator = CopyFormatConfig.Separator.NEWLINE,
				refLength = CopyFormatConfig.RefLength.LONG,
				refSpacing = true,
				verseNumberStyle = CopyFormatConfig.VerseNumberStyle.PLAIN,
				verseNumberSpacing = 2
			),
			"기본2" to CopyFormatConfig(
				refVerseSeparator = CopyFormatConfig.Separator.SPACE,
				multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
				refPosition = CopyFormatConfig.RefPosition.BEFORE,
				refLength = CopyFormatConfig.RefLength.SHORT,
				refSpacing = true,
				refBracket = CopyFormatConfig.RefBracket.PAREN,
				showVerseNumberWhenMulti = false
			),
			"기본3" to CopyFormatConfig(
				refVerseSeparator = CopyFormatConfig.Separator.SPACE,
				multiVerseSeparator = CopyFormatConfig.Separator.NEWLINE,
				refPosition = CopyFormatConfig.RefPosition.AFTER,
				refLength = CopyFormatConfig.RefLength.LONG,
				refSpacing = true,
				refBracket = CopyFormatConfig.RefBracket.PAREN,
				showVerseNumberWhenMulti = false
			)
		)

		for ((name, cfg) in defaults) {
			db.copyFormatPresetDao()
				.insert(CopyFormatPreset(name = name, configJson = cfg.toJson()))
		}
		prefs.edit().putBoolean("default_presets_seeded", true).apply()
	}
}