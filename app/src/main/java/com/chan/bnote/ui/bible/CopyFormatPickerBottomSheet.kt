package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.mypage.CopyFormatConfig
import com.chan.bnote.data.mypage.CopyFormatPreset
import com.chan.bnote.data.mypage.CopyFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/** 저장된 복사 형식들을 보여주고, 항목을 누르면 그 아래로 예시 · 수정 · 선택이 펼쳐진다.
 * 추가/수정은 CopyFormatBottomSheet로 넘어간다. */
class CopyFormatPickerBottomSheet : BottomSheetDialogFragment() {

	private lateinit var presetsContainer: LinearLayout

	// 예시 미리보기용 샘플 데이터: 창세기 1:1~2 (편집 화면과 동일)
	private val sampleVerses = listOf(
		BibleVerse(
			translation = "NKRV",
			bookId = 1,
			chapter = 1,
			verse = 1,
			text = "태초에 하나님이 천지를 창조하시니라"
		),
		BibleVerse(
			translation = "NKRV",
			bookId = 1,
			chapter = 1,
			verse = 2,
			text = "땅이 혼돈하고 공허하며 흑암이 깊음 위에 있고 하나님의 영은 수면 위에 운행하시니라"
		)
	)

	// 지금 펼쳐져 있는 프리셋 id(하나만 펼쳐둔다). 목록을 다시 그려도 유지되도록 필드로 둔다.
	private var expandedPresetId: Long? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_copy_format_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		presetsContainer = view.findViewById(R.id.container_copy_format_presets)

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

				val header = row.findViewById<LinearLayout>(R.id.row_preset_header)
				val toggle = row.findViewById<TextView>(R.id.text_preset_toggle)
				val expanded = row.findViewById<LinearLayout>(R.id.container_preset_expanded)
				val exampleText = row.findViewById<TextView>(R.id.text_preset_example)

				row.findViewById<TextView>(R.id.text_preset_name).text = preset.name
				row.findViewById<TextView>(R.id.text_preset_active_badge).visibility =
					if (preset.configJson == activeConfigJson) View.VISIBLE else View.GONE

				val isExpanded = expandedPresetId == preset.id
				expanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
				toggle.text = if (isExpanded) "▴" else "▾"
				if (isExpanded) exampleText.text = buildExample(preset)

				header.setOnClickListener {
					expandedPresetId = if (isExpanded) null else preset.id
					loadPresets()
				}

				row.findViewById<TextView>(R.id.btn_preset_edit).setOnClickListener {
					openEditor(existingPresetId = preset.id)
				}
				row.findViewById<TextView>(R.id.btn_preset_select).setOnClickListener {
					AppSettings.setActiveCopyFormat(requireContext(), preset.toConfig())
					dismiss()
				}

				presetsContainer.addView(row)
			}
		}
	}

	private fun buildExample(preset: CopyFormatPreset): String {
		return CopyFormatter.format(
			bookId = 1,
			chapter = 1,
			verses = sampleVerses,
			selectedVerseNumbers = setOf(1, 2),
			secondaryMap = null,
			includeSecondary = false,
			config = preset.toConfig()
		)
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