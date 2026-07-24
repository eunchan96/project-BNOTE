package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.mypage.CopyFormatConfig
import com.chan.bnote.data.mypage.CopyFormatPreset
import com.chan.bnote.data.mypage.CopyFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 복사할 때 쓸 형식을 자유롭게 조합해서 미리보고, 이름 붙여 저장해뒀다가 나중에 다시 불러올 수 있는 화면. */
class CopyFormatBottomSheet : BottomSheetDialogFragment() {

	private var config: CopyFormatConfig = CopyFormatConfig()
	private lateinit var groupsContainer: LinearLayout
	private lateinit var presetsContainer: LinearLayout
	private lateinit var previewText: TextView

	// 미리보기용 샘플 데이터: 창세기 1:1~2
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

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_copy_format, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		config = AppSettings.getActiveCopyFormat(requireContext())
		groupsContainer = view.findViewById(R.id.container_option_groups)
		presetsContainer = view.findViewById(R.id.container_saved_presets)
		previewText = view.findViewById(R.id.text_copy_format_preview)

		buildOptionGroups()
		updatePreview()
		loadPresets()

		view.findViewById<TextView>(R.id.btn_save_as_preset).setOnClickListener { showSaveDialog() }
		view.findViewById<TextView>(R.id.btn_apply_copy_format).setOnClickListener {
			AppSettings.setActiveCopyFormat(requireContext(), config)
			dismiss()
		}
	}

	private fun buildOptionGroups() {
		groupsContainer.removeAllViews()

		addGroup("본문과 구절 구분") {
			listOf(
				"줄바꿈" to (config.refVerseSeparator == CopyFormatConfig.Separator.NEWLINE) to {
					config = config.copy(refVerseSeparator = CopyFormatConfig.Separator.NEWLINE)
				},
				"한 줄" to (config.refVerseSeparator == CopyFormatConfig.Separator.SPACE) to {
					config = config.copy(refVerseSeparator = CopyFormatConfig.Separator.SPACE)
				}
			)
		}

		addGroup("구절 여러 개일 때 구분") {
			listOf(
				"줄바꿈" to (config.multiVerseSeparator == CopyFormatConfig.Separator.NEWLINE) to {
					config = config.copy(multiVerseSeparator = CopyFormatConfig.Separator.NEWLINE)
				},
				"한 줄" to (config.multiVerseSeparator == CopyFormatConfig.Separator.SPACE) to {
					config = config.copy(multiVerseSeparator = CopyFormatConfig.Separator.SPACE)
				}
			)
		}

		addGroup("본문 위치 (한 줄일 때만 적용)") {
			listOf(
				"구절 앞" to (config.refPosition == CopyFormatConfig.RefPosition.BEFORE) to {
					config = config.copy(refPosition = CopyFormatConfig.RefPosition.BEFORE)
				},
				"구절 뒤" to (config.refPosition == CopyFormatConfig.RefPosition.AFTER) to {
					config = config.copy(refPosition = CopyFormatConfig.RefPosition.AFTER)
				}
			)
		}

		addGroup("본문 길이") {
			listOf(
				"짧게 (창 1:1)" to (config.refLength == CopyFormatConfig.RefLength.SHORT) to {
					config = config.copy(refLength = CopyFormatConfig.RefLength.SHORT)
				},
				"길게 (창세기 1장 1절)" to (config.refLength == CopyFormatConfig.RefLength.LONG) to {
					config = config.copy(refLength = CopyFormatConfig.RefLength.LONG)
				}
			)
		}

		addGroup("본문 띄어쓰기") {
			listOf(
				"띄어쓰기" to config.refSpacing to { config = config.copy(refSpacing = true) },
				"붙여쓰기" to !config.refSpacing to { config = config.copy(refSpacing = false) }
			)
		}

		addGroup("본문 괄호") {
			listOf(
				"없음" to (config.refBracket == CopyFormatConfig.RefBracket.NONE) to {
					config = config.copy(refBracket = CopyFormatConfig.RefBracket.NONE)
				},
				"(소괄호)" to (config.refBracket == CopyFormatConfig.RefBracket.PAREN) to {
					config = config.copy(refBracket = CopyFormatConfig.RefBracket.PAREN)
				},
				"[대괄호]" to (config.refBracket == CopyFormatConfig.RefBracket.SQUARE) to {
					config = config.copy(refBracket = CopyFormatConfig.RefBracket.SQUARE)
				}
			)
		}

		addGroup("구절 앞뒤 큰따옴표") {
			listOf(
				"안 붙임" to !config.quoteVerse to { config = config.copy(quoteVerse = false) },
				"붙임" to config.quoteVerse to { config = config.copy(quoteVerse = true) }
			)
		}

		addGroup("구절 여러 개일 때 절 번호 표시") {
			listOf(
				"표시 안 함" to !config.showVerseNumberWhenMulti to {
					config = config.copy(showVerseNumberWhenMulti = false)
				},
				"표시함" to config.showVerseNumberWhenMulti to {
					config = config.copy(showVerseNumberWhenMulti = true)
				}
			)
		}

		addGroup("절 번호 표시 유형") {
			listOf(
				"1" to (config.verseNumberStyle == CopyFormatConfig.VerseNumberStyle.PLAIN) to {
					config = config.copy(verseNumberStyle = CopyFormatConfig.VerseNumberStyle.PLAIN)
				},
				"1." to (config.verseNumberStyle == CopyFormatConfig.VerseNumberStyle.DOT) to {
					config = config.copy(verseNumberStyle = CopyFormatConfig.VerseNumberStyle.DOT)
				},
				"[1]" to (config.verseNumberStyle == CopyFormatConfig.VerseNumberStyle.BRACKET) to {
					config =
						config.copy(verseNumberStyle = CopyFormatConfig.VerseNumberStyle.BRACKET)
				}
			)
		}

		addGroup("절 번호 뒤 띄어쓰기") {
			listOf(
				"1칸" to (config.verseNumberSpacing == 1) to {
					config = config.copy(verseNumberSpacing = 1)
				},
				"2칸" to (config.verseNumberSpacing == 2) to {
					config = config.copy(verseNumberSpacing = 2)
				},
				"3칸" to (config.verseNumberSpacing == 3) to {
					config = config.copy(verseNumberSpacing = 3)
				}
			)
		}
	}

	/** (표시 텍스트, 선택 여부) to 선택했을 때 실행할 동작, 의 리스트를 받아 칩 한 줄을 만든다. */
	private fun addGroup(
		label: String,
		optionsProvider: () -> List<Pair<Pair<String, Boolean>, () -> Unit>>
	) {
		val labelView = TextView(requireContext()).apply {
			text = label
			textSize = 13f
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
			setPadding(dp(16), dp(10), dp(16), dp(4))
		}
		groupsContainer.addView(labelView)

		val row = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding(dp(16), 0, dp(16), 0)
		}
		groupsContainer.addView(row)

		for ((textAndSelected, onClick) in optionsProvider()) {
			val (text, selected) = textAndSelected
			val chip = LayoutInflater.from(requireContext())
				.inflate(R.layout.item_copy_format_chip, row, false) as TextView
			chip.text = text
			applyChipStyle(chip, selected)
			chip.setOnClickListener {
				onClick()
				buildOptionGroups()
				updatePreview()
			}
			row.addView(chip)
		}
	}

	private fun applyChipStyle(chip: TextView, selected: Boolean) {
		chip.setBackgroundResource(if (selected) R.drawable.bg_book_button_selected else R.drawable.bg_book_button)
		chip.setTextColor(
			ContextCompat.getColor(
				requireContext(),
				if (selected) R.color.white else R.color.text_primary
			)
		)
	}

	private fun updatePreview() {
		val preview = CopyFormatter.format(
			bookId = 1,
			chapter = 1,
			verses = sampleVerses,
			selectedVerseNumbers = setOf(1, 2),
			secondaryMap = null,
			includeSecondary = false,
			config = config
		)
		previewText.text = preview
	}

	private fun loadPresets() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			seedDefaultPresetsIfNeeded(db)
			val presets = db.copyFormatPresetDao().getAll()

			presetsContainer.removeAllViews()
			requireView().findViewById<TextView>(R.id.text_no_presets).visibility =
				if (presets.isEmpty()) View.VISIBLE else View.GONE

			for (preset in presets) {
				val row = LinearLayout(requireContext()).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = android.view.Gravity.CENTER_VERTICAL
					setPadding(dp(16), dp(10), dp(16), dp(10))
					isClickable = true
					isFocusable = true
					background = ContextCompat.getDrawable(
						requireContext(),
						android.R.drawable.list_selector_background
					)
				}
				val nameView = TextView(requireContext()).apply {
					text = preset.name
					textSize = 15f
					setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
				}
				val deleteView = TextView(requireContext()).apply {
					text = "삭제"
					textSize = 13f
					setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
					setPadding(dp(8), dp(8), dp(8), dp(8))
					isClickable = true
					isFocusable = true
					setOnClickListener {
						lifecycleScope.launch {
							db.copyFormatPresetDao().delete(preset)
							loadPresets()
						}
					}
				}
				row.addView(nameView)
				row.addView(deleteView)
				row.setOnClickListener {
					config = preset.toConfig()
					buildOptionGroups()
					updatePreview()
				}
				presetsContainer.addView(row)
			}
		}
	}

	/** 기본으로 제공하는 형식 3개. 사용자가 처음 이 화면을 열 때 한 번만 심고, 그 뒤로는 자유롭게
	 * 수정 · 삭제할 수 있는 그냥 평범한 프리셋이다(다시 자동으로 채워지지 않는다). */
	private suspend fun seedDefaultPresetsIfNeeded(db: BibleDatabase) {
		val prefs = requireContext().getSharedPreferences(
			"copy_format_prefs",
			android.content.Context.MODE_PRIVATE
		)
		if (prefs.getBoolean("default_presets_seeded", false)) return

		val defaults = listOf(
			"창세기 1장 (줄바꿈형)" to CopyFormatConfig(
				refVerseSeparator = CopyFormatConfig.Separator.NEWLINE,
				multiVerseSeparator = CopyFormatConfig.Separator.NEWLINE,
				refLength = CopyFormatConfig.RefLength.LONG,
				refSpacing = true,
				verseNumberStyle = CopyFormatConfig.VerseNumberStyle.PLAIN,
				verseNumberSpacing = 2
			),
			"(창 1:1~2) 짧은형" to CopyFormatConfig(
				refVerseSeparator = CopyFormatConfig.Separator.SPACE,
				multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
				refPosition = CopyFormatConfig.RefPosition.BEFORE,
				refLength = CopyFormatConfig.RefLength.SHORT,
				refSpacing = true,
				refBracket = CopyFormatConfig.RefBracket.PAREN,
				showVerseNumberWhenMulti = false
			),
			"~ (창세기 1장 1~2절)" to CopyFormatConfig(
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

	private fun showSaveDialog() {
		val editText = EditText(requireContext()).apply {
			hint = "형식 이름 (예: 카톡용, 노션용)"
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_book_button)
		}
		val container = FrameLayout(requireContext()).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}

		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("이 형식 저장")
			.setView(container)
			.setPositiveButton("저장") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isEmpty()) return@setPositiveButton
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					db.copyFormatPresetDao().insert(
						CopyFormatPreset(name = name, configJson = config.toJson())
					)
					loadPresets()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}