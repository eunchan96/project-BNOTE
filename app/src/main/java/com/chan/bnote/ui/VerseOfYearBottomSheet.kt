package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.VerseOfYear
import kotlinx.coroutines.launch
import java.util.Calendar

class VerseOfYearBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.75f

	private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
	private var selectedBookId: Int? = null
	private var selectedChapter: Int? = null
	private var selectedVerse: Int? = null
	private var selectedVerseText: String = ""

	private lateinit var verseContentText: TextView
	private lateinit var verseRefText: TextView
	private lateinit var noteEdit: EditText

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_verse_of_year, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_year_label).text = "${currentYear}년의 말씀"
		verseContentText = view.findViewById(R.id.text_verse_content)
		verseRefText = view.findViewById(R.id.text_verse_ref)
		noteEdit = view.findViewById(R.id.edit_verse_note)

		view.findViewById<LinearLayout>(R.id.container_current_verse).setOnClickListener {
			val picker = BookChapterPickerBottomSheet("GAEYEOK")
			picker.onVerseSelected = { bookId, chapter, verse ->
				selectedBookId = bookId
				selectedChapter = chapter
				selectedVerse = verse
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					val verseData = db.bibleDao().getVerses("GAEYEOK", bookId, chapter)
						.firstOrNull { it.verse == verse }
					selectedVerseText = verseData?.text ?: ""
					verseContentText.text = selectedVerseText
					verseRefText.text = "${BibleBooks.nameOf(bookId)} $chapter:$verse"
				}
			}
			picker.show(parentFragmentManager, "verse_of_year_picker")
		}

		view.findViewById<TextView>(R.id.btn_save_verse_of_year).setOnClickListener {
			save()
		}

		loadCurrentYear()
		loadPastYears(view)
	}

	private fun loadCurrentYear() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val existing = db.verseOfYearDao().getByYear(currentYear) ?: return@launch

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

	private fun loadPastYears(rootView: View) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val allEntries = db.verseOfYearDao().getAll().filter { it.year != currentYear }

			val container = rootView.findViewById<LinearLayout>(R.id.container_past_years)
			val emptyText = rootView.findViewById<TextView>(R.id.text_no_past_years)

			if (allEntries.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				return@launch
			}
			emptyText.visibility = View.GONE

			for (entry in allEntries) {
				val row = LayoutInflater.from(requireContext())
					.inflate(R.layout.item_verse_of_year_row, container, false)
				row.findViewById<TextView>(R.id.text_row_year).text = "${entry.year}년"
				row.findViewById<TextView>(R.id.text_row_verse).text =
					"${BibleBooks.nameOf(entry.bookId)} ${entry.chapter}:${entry.verse}  ${entry.verseText}"
				container.addView(row)
			}
		}
	}

	private fun save() {
		val bookId = selectedBookId
		val chapter = selectedChapter
		val verse = selectedVerse

		if (bookId == null || chapter == null || verse == null) {
			Toast.makeText(requireContext(), "말씀을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			db.verseOfYearDao().upsert(
				VerseOfYear(
					year = currentYear,
					bookId = bookId,
					chapter = chapter,
					verse = verse,
					verseText = selectedVerseText,
					note = noteEdit.text.toString()
				)
			)
			Toast.makeText(requireContext(), "저장됐어요", Toast.LENGTH_SHORT).show()
			dismiss()
		}
	}
}