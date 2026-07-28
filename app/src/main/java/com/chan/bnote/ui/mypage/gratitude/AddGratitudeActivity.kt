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
		row.findViewById<EditText>(R.id.edit_gratitude_entry).setText(initialText)
		containerEntries.addView(row)
	}

	private fun currentEntryTexts(): List<String> {
		val texts = mutableListOf<String>()
		for (i in 0 until containerEntries.childCount) {
			val row = containerEntries.getChildAt(i)
			val text = row.findViewById<EditText>(R.id.edit_gratitude_entry).text.toString().trim()
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
			onSaveAndExit = { save() },
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