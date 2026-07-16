package com.chan.bnote.ui.mypage

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.VerseOfYear
import com.chan.bnote.ui.bible.BookChapterPickerBottomSheet
import kotlinx.coroutines.launch
import java.util.Calendar

class VerseOfYearEditActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_YEAR = "extra_year"

		/** 신규 추가용 Intent. */
		fun addIntent(context: Context): Intent =
			Intent(context, VerseOfYearEditActivity::class.java)

		/** 기존 연도 수정용 Intent. */
		fun editIntent(context: Context, year: Int): Intent {
			return Intent(context, VerseOfYearEditActivity::class.java).apply {
				putExtra(EXTRA_YEAR, year)
			}
		}
	}

	private var isEditMode = false
	private var editingYear: Int = 0

	private var selectedBookId: Int? = null
	private var selectedChapter: Int? = null
	private var selectedVerse: Int? = null
	private var selectedVerseText: String = ""

	private lateinit var yearEdit: EditText
	private lateinit var yearFixedText: TextView
	private lateinit var verseContentText: TextView
	private lateinit var verseRefText: TextView
	private lateinit var noteEdit: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_verse_of_year_edit)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verse_of_year_edit_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		isEditMode = intent.hasExtra(EXTRA_YEAR)
		editingYear = intent.getIntExtra(EXTRA_YEAR, Calendar.getInstance().get(Calendar.YEAR))

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "${editingYear}년 말씀 수정" else "말씀 추가"

		yearEdit = findViewById(R.id.edit_year)
		yearFixedText = findViewById(R.id.text_year_fixed)
		verseContentText = findViewById(R.id.text_verse_content)
		verseRefText = findViewById(R.id.text_verse_ref)
		noteEdit = findViewById(R.id.edit_verse_note)

		if (isEditMode) {
			yearEdit.visibility = android.view.View.GONE
			yearFixedText.visibility = android.view.View.VISIBLE
			yearFixedText.text = "${editingYear}년"
		} else {
			yearEdit.setText(Calendar.getInstance().get(Calendar.YEAR).toString())
		}

		findViewById<LinearLayout>(R.id.container_verse_picker).setOnClickListener {
			val picker = BookChapterPickerBottomSheet("GAEYEOK")
			picker.onVerseSelected = { bookId, chapter, verse ->
				selectedBookId = bookId
				selectedChapter = chapter
				selectedVerse = verse
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					val verseData = db.bibleDao().getVerses("GAEYEOK", bookId, chapter)
						.firstOrNull { it.verse == verse }
					selectedVerseText = verseData?.text ?: ""
					verseContentText.text = selectedVerseText
					verseRefText.text = "${BibleBooks.nameOf(bookId)} $chapter:$verse"
				}
			}
			picker.show(supportFragmentManager, "verse_of_year_picker")
		}

		val deleteBtn = findViewById<ImageView>(R.id.btn_delete_entry)
		if (isEditMode) {
			deleteBtn.visibility = android.view.View.VISIBLE
			deleteBtn.setOnClickListener { confirmDelete() }
		}

		findViewById<TextView>(R.id.btn_save_verse_of_year).setOnClickListener { save() }

		if (isEditMode) {
			loadExisting()
		}
	}

	private fun loadExisting() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val existing = db.verseOfYearDao().getByYear(editingYear) ?: return@launch

			selectedBookId = existing.bookId
			selectedChapter = existing.chapter
			selectedVerse = existing.verse
			selectedVerseText = existing.verseText

			verseContentText.text = existing.verseText
			verseRefText.text =
				"${BibleBooks.nameOf(existing.bookId)} ${existing.chapter}:${existing.verse}"
			noteEdit.setText(existing.note)
		}
	}

	private fun save() {
		val bookId = selectedBookId
		val chapter = selectedChapter
		val verse = selectedVerse

		if (bookId == null || chapter == null || verse == null) {
			Toast.makeText(this, "말씀을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		val year: Int
		if (isEditMode) {
			year = editingYear
		} else {
			val yearInput = yearEdit.text.toString().trim().toIntOrNull()
			if (yearInput == null || yearInput < 1900 || yearInput > 2200) {
				Toast.makeText(this, "연도를 올바르게 입력해주세요", Toast.LENGTH_SHORT).show()
				return
			}
			year = yearInput
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (!isEditMode) {
				val existing = db.verseOfYearDao().getByYear(year)
				if (existing != null) {
					Toast.makeText(
						this@VerseOfYearEditActivity,
						"이미 ${year}년 말씀이 있어요. 목록에서 수정해주세요",
						Toast.LENGTH_SHORT
					).show()
					return@launch
				}
			}

			db.verseOfYearDao().upsert(
				VerseOfYear(
					year = year,
					bookId = bookId,
					chapter = chapter,
					verse = verse,
					verseText = selectedVerseText,
					note = noteEdit.text.toString()
				)
			)
			Toast.makeText(this@VerseOfYearEditActivity, "저장됐어요", Toast.LENGTH_SHORT).show()
			finish()
		}
	}

	private fun confirmDelete() {
		AlertDialog.Builder(this)
			.setTitle("${editingYear}년 말씀 삭제")
			.setMessage("삭제하면 되돌릴 수 없어요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.verseOfYearDao().delete(editingYear)
					Toast.makeText(this@VerseOfYearEditActivity, "삭제됐어요", Toast.LENGTH_SHORT).show()
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}