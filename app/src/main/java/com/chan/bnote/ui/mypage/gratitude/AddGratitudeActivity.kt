package com.chan.bnote.ui.mypage.gratitude

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.gratitude.GratitudeEntry
import com.chan.bnote.data.mypage.gratitude.GratitudeNote
import com.chan.bnote.ui.common.UnsavedChangesDialog
import kotlinx.coroutines.launch
import java.util.Calendar

class AddGratitudeActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NOTE_ID = "extra_note_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"
		private const val DEFAULT_ENTRY_COUNT = 5

		fun createIntent(
			context: Context,
			initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		): Intent {
			return Intent(context, AddGratitudeActivity::class.java).apply {
				putExtra(EXTRA_INITIAL_DATE_MILLIS, initialDateMillis)
			}
		}

		fun editIntent(context: Context, noteId: Long): Intent {
			return Intent(context, AddGratitudeActivity::class.java).apply {
				putExtra(EXTRA_NOTE_ID, noteId)
			}
		}
	}

	private var isEditMode = false
	private var existingNote: GratitudeNote? = null
	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var originalTexts: List<String> = emptyList()
	private var originalDateMillis: Long = 0L

	private lateinit var btnPickDate: TextView
	private lateinit var containerEntries: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_add_gratitude)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_gratitude_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			v.setPadding(
				systemBars.left,
				systemBars.top,
				systemBars.right,
				maxOf(systemBars.bottom, ime.bottom)
			)
			insets
		}

		val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
		isEditMode = noteId != -1L
		selectedDateMillis = intent.getLongExtra(
			EXTRA_INITIAL_DATE_MILLIS, DateUtils.normalizeToDayStart(System.currentTimeMillis())
		)

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "감사 노트 수정" else "감사 노트 작성"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }

		btnPickDate = findViewById(R.id.btn_pick_date)
		containerEntries = findViewById(R.id.container_gratitude_entries)

		updateDateText()
		btnPickDate.setOnClickListener { showDatePicker() }

		findViewById<TextView>(R.id.btn_add_gratitude_entry).setOnClickListener { addEntryRow("") }
		findViewById<TextView>(R.id.btn_save_gratitude).setOnClickListener { save() }

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPress()
			}
		})

		if (isEditMode) {
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(applicationContext)
				val note = db.gratitudeNoteDao().getById(noteId)
				existingNote = note
				if (note != null) {
					selectedDateMillis = note.date
					originalDateMillis = note.date
					updateDateText()
					val entries = db.gratitudeEntryDao().getByNote(note.id)
					if (entries.isEmpty()) {
						repeat(DEFAULT_ENTRY_COUNT) { addEntryRow("") }
					} else {
						entries.forEach { addEntryRow(it.text) }
					}
					originalTexts = currentEntryTexts()
				}
			}
		} else {
			repeat(DEFAULT_ENTRY_COUNT) { addEntryRow("") }
		}
	}

	private fun updateDateText() {
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = SpannableString(label)
		spannable.setSpan(UnderlineSpan(), 0, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		btnPickDate.text = spannable
	}

	private fun showDatePicker() {
		val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
		android.app.DatePickerDialog(
			this,
			{ _, year, month, day ->
				val picked = Calendar.getInstance()
				picked.set(year, month, day, 0, 0, 0)
				selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
				updateDateText()
			},
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
		).show()
	}

	private fun addEntryRow(initialText: String) {
		val row = layoutInflater.inflate(R.layout.item_gratitude_entry_row, containerEntries, false)
		val editText = row.findViewById<EditText>(R.id.edit_gratitude_entry)
		editText.setText(initialText)

		// textMultiLine이라 길게 쓰면 화면 폭에서 자연스럽게 줄바꿈(래핑)되지만, 실제로 개행 문자
		// ("\n")가 들어가는 건 막는다 — 각 칸은 어디까지나 한 줄짜리 항목이어야 한다. 키보드마다
		// 엔터를 처리하는 방식이 조금씩 달라서(어떤 키보드는 그냥 "\n"을 넣어버림), 입력 필터로
		// 한 번 더 확실히 걸러낸다.
		editText.filters = arrayOf(android.text.InputFilter { source, _, _, _, _, _ ->
			if (source.contains("\n")) source.toString().replace("\n", "") else source
		})

		editText.setOnEditorActionListener { view, actionId, event ->
			val isEnterKeyDown = event != null &&
					event.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
					event.action == android.view.KeyEvent.ACTION_DOWN
			if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT || isEnterKeyDown) {
				focusNextRowAfter(view as EditText)
				true
			} else if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
				view.clearFocus()
				true
			} else {
				false
			}
		}

		containerEntries.addView(row)
		updateImeActionsForRows()
	}

	/** 지금 칸 다음에 있는 행의 입력칸으로 포커스를 옮긴다(그 행이 맨 마지막이면 아무 일도 안 함). */
	private fun focusNextRowAfter(current: EditText) {
		for (i in 0 until containerEntries.childCount) {
			val row = containerEntries.getChildAt(i)
			val editText = row.findViewById<EditText>(R.id.edit_gratitude_entry)
			if (editText === current) {
				val nextRow = containerEntries.getChildAt(i + 1) ?: return
				nextRow.findViewById<EditText>(R.id.edit_gratitude_entry).requestFocus()
				return
			}
		}
	}

	/** 키보드의 엔터 자리에 "다음" 버튼이 뜨게 해서, 다음 칸을 직접 안 눌러도 그대로 넘어갈 수
	 * 있게 한다(각 칸이 한 줄짜리라 원래 줄바꿈이 필요 없다). 실제로 다음 칸이 있는 행만 "다음"으로
	 * 보여주고, 맨 마지막 행은 "완료"로 보여준다 — "+ 항목 추가"로 새 행이 생기면 그 직전까지
	 * "완료"였던 행도 다시 "다음"으로 바뀌어야 하므로 매번 전체를 다시 맞춘다. */
	private fun updateImeActionsForRows() {
		val count = containerEntries.childCount
		for (i in 0 until count) {
			val editText =
				containerEntries.getChildAt(i).findViewById<EditText>(R.id.edit_gratitude_entry)
			val isLast = i == count - 1
			editText.imeOptions = if (isLast) {
				android.view.inputmethod.EditorInfo.IME_ACTION_DONE
			} else {
				android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
			}
		}
	}

	private fun currentEntryTexts(): List<String> {
		val texts = mutableListOf<String>()
		for (i in 0 until containerEntries.childCount) {
			val row = containerEntries.getChildAt(i)
			// 입력 필터로 개행을 걸러내고 있지만, 혹시 다른 경로(자동완성 등)로 섞여 들어왔을 수도
			// 있으니 저장 직전에도 한 번 더 확실히 없앤다 — 개행이 남아있으면 캘린더 미리보기의
			// 들여쓰기 계산이 어긋난다.
			val text = row.findViewById<EditText>(R.id.edit_gratitude_entry).text.toString()
				.replace("\n", " ").trim()
			texts.add(text)
		}
		return texts
	}

	private fun hasUnsavedContent(): Boolean {
		val currentNonBlank = currentEntryTexts().filter { it.isNotBlank() }
		if (!isEditMode) {
			return currentNonBlank.isNotEmpty()
		}
		val originalNonBlank = originalTexts.filter { it.isNotBlank() }
		return currentNonBlank != originalNonBlank || selectedDateMillis != originalDateMillis
	}

	private fun handleBackPress() {
		if (!hasUnsavedContent()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(
			context = this,
			onDiscard = { finish() }
		)
	}

	private fun save() {
		val texts = currentEntryTexts().filter { it.isNotBlank() }

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val current = existingNote

			val noteId: Long = if (current == null) {
				db.gratitudeNoteDao().insert(GratitudeNote(date = selectedDateMillis))
			} else {
				db.gratitudeNoteDao().update(current.copy(date = selectedDateMillis))
				db.gratitudeEntryDao().deleteByNote(current.id)
				current.id
			}

			if (texts.isNotEmpty()) {
				db.gratitudeEntryDao().insertAll(
					texts.mapIndexed { index, text ->
						GratitudeEntry(noteId = noteId, text = text, sortOrder = index)
					}
				)
			}

			setResult(RESULT_OK)
			finish()
		}
	}
}