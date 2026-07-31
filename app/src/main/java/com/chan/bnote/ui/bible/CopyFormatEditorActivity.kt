package com.chan.bnote.ui.bible

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.mypage.CopyFormatConfig
import com.chan.bnote.data.mypage.CopyFormatPreset
import com.chan.bnote.data.mypage.CopyFormatter
import com.chan.bnote.ui.common.UnsavedChangesDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 복사 형식 하나를 새로 만들거나 수정하는 화면. existingPresetId가 있으면 수정 모드. */
class CopyFormatEditorActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PRESET_ID = "extra_preset_id"

		fun createIntent(context: Context, existingPresetId: Long? = null): Intent {
			return Intent(context, CopyFormatEditorActivity::class.java).apply {
				if (existingPresetId != null) putExtra(EXTRA_PRESET_ID, existingPresetId)
			}
		}
	}

	private var existingPresetId: Long? = null
	private var existingPreset: CopyFormatPreset? = null
	private var config: CopyFormatConfig = CopyFormatConfig()
	private var originalName: String = ""
	private var originalConfigJson: String = ""

	private lateinit var groupsContainer: LinearLayout
	private lateinit var previewText: TextView
	private lateinit var titleView: TextView
	private lateinit var editName: EditText
	private lateinit var btnDeletePreset: TextView
	private lateinit var btnSavePreset: TextView

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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_copy_format_editor)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.copy_format_editor_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		existingPresetId =
			intent.getLongExtra(EXTRA_PRESET_ID, -1L).let { if (it == -1L) null else it }

		titleView = findViewById(R.id.text_copy_format_editor_title)
		editName = findViewById(R.id.edit_preset_name)
		groupsContainer = findViewById(R.id.container_option_groups)
		previewText = findViewById(R.id.text_copy_format_preview)
		btnDeletePreset = findViewById(R.id.btn_delete_preset_in_editor)
		btnSavePreset = findViewById(R.id.btn_save_preset)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPress()
			}
		})

		lifecycleScope.launch {
			val presetId = existingPresetId
			if (presetId != null) {
				val db = BibleDatabase.getInstance(applicationContext)
				existingPreset = db.copyFormatPresetDao().getById(presetId)
			}
			config = existingPreset?.toConfig()
				?: AppSettings.getActiveCopyFormat(this@CopyFormatEditorActivity)

			titleView.text = if (existingPreset != null) "형식 수정" else "형식 추가"
			editName.setText(existingPreset?.name ?: "")
			btnDeletePreset.visibility = if (existingPreset != null) View.VISIBLE else View.GONE

			originalName = editName.text.toString()
			originalConfigJson = config.toJson()

			buildOptionGroups()
			updatePreview()
		}

		btnSavePreset.setOnClickListener { savePreset() }
		btnDeletePreset.setOnClickListener { confirmDelete() }
	}

	private fun hasUnsavedChanges(): Boolean {
		return editName.text.toString() != originalName || config.toJson() != originalConfigJson
	}

	private fun handleBackPress() {
		if (!hasUnsavedChanges()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(
			context = this,
			onDiscard = { finish() }
		)
	}

	private fun savePreset() {
		val name = editName.text.toString().trim()
		if (name.isEmpty()) {
			editName.error = "이름을 입력해주세요"
			return
		}
		val preset = existingPreset
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			if (preset != null) {
				db.copyFormatPresetDao()
					.update(preset.copy(name = name, configJson = config.toJson()))
			} else {
				db.copyFormatPresetDao()
					.insert(CopyFormatPreset(name = name, configJson = config.toJson()))
			}
			AppSettings.setActiveCopyFormat(this@CopyFormatEditorActivity, config)
			setResult(Activity.RESULT_OK)
			finish()
		}
	}

	private fun confirmDelete() {
		val preset = existingPreset ?: return
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("형식 삭제")
			.setMessage("'${preset.name}' 형식을 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.copyFormatPresetDao().delete(preset)
					setResult(Activity.RESULT_OK)
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
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
				"중간 (창세기 1:1)" to (config.refLength == CopyFormatConfig.RefLength.MEDIUM) to {
					config = config.copy(refLength = CopyFormatConfig.RefLength.MEDIUM)
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

		addGroup("구절 앞뒤 큰따옴표 (선택한 구절 전체를 한 번에 묶어요)") {
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
		val labelView = TextView(this).apply {
			text = label
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@CopyFormatEditorActivity,
					R.color.text_secondary
				)
			)
			setPadding(dp(16), dp(10), dp(16), dp(4))
		}
		groupsContainer.addView(labelView)

		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding(dp(16), 0, dp(16), 0)
		}
		groupsContainer.addView(row)

		for ((textAndSelected, onClick) in optionsProvider()) {
			val (text, selected) = textAndSelected
			val chip = LayoutInflater.from(this)
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
				this,
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

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}