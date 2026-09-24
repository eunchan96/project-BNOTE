package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.mypage.CopyFormatConfig
import com.chan.bnote.data.mypage.CopyFormatPreset
import com.chan.bnote.data.mypage.CopyFormatter
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import kotlinx.coroutines.launch

/** 저장된 복사 형식들을 보여주고, 항목을 누르면 그 아래로 예시 · 수정 · 선택이 펼쳐진다.
 * 추가/수정은 CopyFormatEditorActivity로 넘어간다. */
class CopyFormatPickerBottomSheet : FixedBottomSheetDialogFragment() {

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

	private val editorLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == android.app.Activity.RESULT_OK) loadPresets()
	}

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
				val toggle = row.findViewById<android.widget.ImageView>(R.id.img_preset_toggle)
				val expanded = row.findViewById<LinearLayout>(R.id.container_preset_expanded)
				val exampleText = row.findViewById<TextView>(R.id.text_preset_example)

				row.findViewById<TextView>(R.id.text_preset_name).text = preset.name
				row.findViewById<TextView>(R.id.text_preset_active_badge).visibility =
					if (preset.configJson == activeConfigJson) View.VISIBLE else View.GONE

				val isExpanded = expandedPresetId == preset.id
				expanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
				// 부록 · 업데이트 내역과 같은 방식: 접혀있으면 ∨(0도), 펼쳐지면 ∧(180도)로 회전한다.
				toggle.rotation = if (isExpanded) 180f else 0f
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
		editorLauncher.launch(
			CopyFormatEditorActivity.createIntent(requireContext(), existingPresetId)
		)
	}

	/** 기본으로 제공하는 형식 3개. 처음 한 번만 심고, 그 뒤로는 자유롭게 수정 · 삭제할 수 있는 그냥
	 * 평범한 프리셋이다(다시 자동으로 채워지지 않는다). */
	private suspend fun seedDefaultPresetsIfNeeded(db: BibleDatabase) {
		val prefs = requireContext().getSharedPreferences(
			"copy_format_prefs",
			android.content.Context.MODE_PRIVATE
		)

		if (!prefs.getBoolean("default_presets_seeded", false)) {
			for ((name, cfg) in defaultPresetConfigs()) {
				db.copyFormatPresetDao()
					.insert(CopyFormatPreset(name = name, configJson = cfg.toJson()))
			}
			prefs.edit()
				.putBoolean("default_presets_seeded", true)
				.putBoolean("default_presets_renamed_v2", true)
				.apply()
			return
		}

		// 이미 예전에 "기본1/기본2/기본3"으로 한 번 심어졌던 기기라면, 이번 개편에 맞춰 이름과
		// 설정을 딱 한 번만 새로 바꿔준다. 그 사이 사용자가 이름을 직접 바꿨거나 지운 프리셋은
		// (이름이 더 이상 "기본1" 등이 아니므로) 건드리지 않는다.
		if (prefs.getBoolean("default_presets_renamed_v2", false)) return
		val renameMap = mapOf(
			"기본1" to defaultPresetConfigs()[0],
			"기본2" to defaultPresetConfigs()[1],
			"기본3" to defaultPresetConfigs()[2]
		)
		for (preset in db.copyFormatPresetDao().getAll()) {
			val replacement = renameMap[preset.name] ?: continue
			db.copyFormatPresetDao().update(
				preset.copy(name = replacement.first, configJson = replacement.second.toJson())
			)
		}
		prefs.edit().putBoolean("default_presets_renamed_v2", true).apply()
	}

	/** "묵상용" / "짧게 (창 1:1)" / "길게 (창세기 1장 1절)" 세 기본 형식의 이름과 설정.
	 * - 묵상용: 참조를 괄호 없이 구절 위에 얹고, 절 번호를 붙여 여러 절을 죽 읽기 좋게 만든다.
	 * - 짧게: "(창 1:1) 본문"처럼 참조를 짧게 줄여 구절 앞에 붙인다.
	 * - 길게: 짧게와 같은 모양이되, 참조만 "창세기 1장 1절"처럼 풀어 쓴다. */
	private fun defaultPresetConfigs(): List<Pair<String, CopyFormatConfig>> = listOf(
		"묵상용" to CopyFormatConfig.meditationDefault(),
		"짧게 (창 1:1)" to CopyFormatConfig(
			refVerseSeparator = CopyFormatConfig.Separator.SPACE,
			multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
			refPosition = CopyFormatConfig.RefPosition.BEFORE,
			refLength = CopyFormatConfig.RefLength.SHORT,
			refSpacing = true,
			refBracket = CopyFormatConfig.RefBracket.PAREN,
			showVerseNumberWhenMulti = false
		),
		"길게 (창세기 1장 1절)" to CopyFormatConfig(
			refVerseSeparator = CopyFormatConfig.Separator.SPACE,
			multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
			refPosition = CopyFormatConfig.RefPosition.BEFORE,
			refLength = CopyFormatConfig.RefLength.LONG,
			refSpacing = true,
			refBracket = CopyFormatConfig.RefBracket.PAREN,
			showVerseNumberWhenMulti = false
		)
	)
}