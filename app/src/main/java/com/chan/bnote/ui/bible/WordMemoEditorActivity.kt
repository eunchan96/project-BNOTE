package com.chan.bnote.ui.bible

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
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
import com.chan.bnote.data.memo.WordMemo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class WordMemoEditorActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TRANSLATION = "extra_translation"
		private const val EXTRA_BOOK_ID = "extra_book_id"
		private const val EXTRA_CHAPTER = "extra_chapter"
		private const val EXTRA_VERSE = "extra_verse"
		private const val EXTRA_START_OFFSET = "extra_start_offset"
		private const val EXTRA_END_OFFSET = "extra_end_offset"

		fun createIntent(
			context: Context,
			translation: String,
			bookId: Int,
			chapter: Int,
			verse: Int,
			startOffset: Int,
			endOffset: Int
		): Intent {
			return Intent(context, WordMemoEditorActivity::class.java).apply {
				putExtra(EXTRA_TRANSLATION, translation)
				putExtra(EXTRA_BOOK_ID, bookId)
				putExtra(EXTRA_CHAPTER, chapter)
				putExtra(EXTRA_VERSE, verse)
				putExtra(EXTRA_START_OFFSET, startOffset)
				putExtra(EXTRA_END_OFFSET, endOffset)
			}
		}
	}

	private data class MemoBox(
		val existing: WordMemo?,
		val root: LinearLayout,
		val editText: EditText
	)

	private lateinit var translation: String
	private var bookId = 0
	private var chapter = 0
	private var verse = 0
	private var startOffset = 0
	private var endOffset = 0
	private var wordText = ""

	private lateinit var container: LinearLayout
	private lateinit var checkboxPropagate: CheckBox
	private val boxes = mutableListOf<MemoBox>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_word_memo_editor)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.word_memo_editor_root)) { v, insets ->
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

		translation = intent.getStringExtra(EXTRA_TRANSLATION) ?: "NKRV"
		bookId = intent.getIntExtra(EXTRA_BOOK_ID, 1)
		chapter = intent.getIntExtra(EXTRA_CHAPTER, 1)
		verse = intent.getIntExtra(EXTRA_VERSE, 1)
		startOffset = intent.getIntExtra(EXTRA_START_OFFSET, 0)
		endOffset = intent.getIntExtra(EXTRA_END_OFFSET, 0)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_memo_boxes)
		checkboxPropagate = findViewById(R.id.chk_propagate)

		findViewById<TextView>(R.id.btn_add_memo_box).setOnClickListener {
			addBox(existing = null)
		}
		findViewById<TextView>(R.id.btn_save_word_memo).setOnClickListener { save() }

		loadExisting()
	}

	private fun loadExisting() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val verseText = db.bibleDao().getVerses(translation, bookId, chapter)
				.find { it.verse == verse }?.text ?: ""
			val safeStart = startOffset.coerceIn(0, verseText.length)
			val safeEnd = endOffset.coerceIn(safeStart, verseText.length)
			wordText = verseText.substring(safeStart, safeEnd)

			val unit = BibleBooks.chapterUnit(bookId)
			findViewById<TextView>(R.id.text_word_context).text =
				"${BibleBooks.nameOf(bookId)} ${chapter}${unit} ${verse}절 · \"$wordText\""

			val existingMemos = db.wordMemoDao()
				.getAtPosition(translation, bookId, chapter, verse, startOffset, endOffset)

			if (existingMemos.isEmpty()) {
				addBox(existing = null)
			} else {
				for (memo in existingMemos) addBox(existing = memo)
			}
		}
	}

	private fun addBox(existing: WordMemo?) {
		val boxView = LayoutInflater.from(this)
			.inflate(R.layout.item_word_memo_box, container, false) as LinearLayout

		val sourceView = boxView.findViewById<TextView>(R.id.text_box_source)
		if (existing?.sourceLabel != null) {
			sourceView.text = "출처: ${existing.sourceLabel}"
			sourceView.visibility = android.view.View.VISIBLE
		}

		val editText = boxView.findViewById<EditText>(R.id.edit_box_text)
		editText.setText(existing?.text ?: "")

		val box = MemoBox(existing, boxView, editText)
		boxes.add(box)
		container.addView(boxView)

		boxView.findViewById<ImageView>(R.id.btn_delete_box).setOnClickListener {
			removeBox(box)
		}
	}

	private fun removeBox(box: MemoBox) {
		boxes.remove(box)
		container.removeView(box.root)
		val existing = box.existing
		if (existing != null) {
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(applicationContext)
				db.wordMemoDao().delete(existing)
			}
		}
	}

	private fun save() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val savedTexts = mutableListOf<String>()

			for (box in boxes) {
				val text = box.editText.text.toString().trim()
				if (text.isEmpty()) continue
				savedTexts.add(text)

				if (box.existing != null) {
					if (box.existing.text != text) {
						db.wordMemoDao().update(
							box.existing.copy(
								text = text,
								updatedAt = System.currentTimeMillis()
							)
						)
					}
				} else {
					db.wordMemoDao().insert(
						WordMemo(
							translation = translation,
							bookId = bookId,
							chapter = chapter,
							verse = verse,
							startOffset = startOffset,
							endOffset = endOffset,
							text = text
						)
					)
				}
			}

			if (checkboxPropagate.isChecked && savedTexts.isNotEmpty()) {
				propagateToOtherVerses(db, savedTexts)
			} else {
				setResult(android.app.Activity.RESULT_OK)
				finish()
			}
		}
	}

	private suspend fun propagateToOtherVerses(db: BibleDatabase, texts: List<String>) {
		if (wordText.isBlank()) {
			setResult(android.app.Activity.RESULT_OK)
			finish()
			return
		}

		val matches = db.bibleDao().findVersesContainingExact(translation, wordText)
			.filter { !(it.bookId == bookId && it.chapter == chapter && it.verse == verse) }

		if (matches.isEmpty()) {
			Toast.makeText(this, "\"$wordText\"가 나오는 다른 구절을 찾지 못했어요", Toast.LENGTH_SHORT).show()
			setResult(android.app.Activity.RESULT_OK)
			finish()
			return
		}

		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("다른 구절에도 추가")
			.setMessage("\"$wordText\"가 나오는 ${matches.size}개 구절에 이 메모를 추가할까요?")
			.setPositiveButton("추가") { _, _ ->
				lifecycleScope.launch {
					val originLabel = "${BibleBooks.shortNameOf(bookId)} ${chapter}:${verse}"

					for (verseRow in matches) {
						val idx = verseRow.text.indexOf(wordText)
						if (idx == -1) continue
						val matchStart = idx
						val matchEnd = idx + wordText.length

						for (text in texts) {
							db.wordMemoDao().insert(
								WordMemo(
									translation = translation,
									bookId = verseRow.bookId,
									chapter = verseRow.chapter,
									verse = verseRow.verse,
									startOffset = matchStart,
									endOffset = matchEnd,
									text = text,
									sourceLabel = originLabel
								)
							)
						}
					}

					Toast.makeText(
						this@WordMemoEditorActivity,
						"${matches.size}개 구절에 추가됐어요",
						Toast.LENGTH_SHORT
					).show()
					setResult(android.app.Activity.RESULT_OK)
					finish()
				}
			}
			.setNegativeButton("취소") { _, _ ->
				setResult(android.app.Activity.RESULT_OK)
				finish()
			}
			.setOnCancelListener {
				setResult(android.app.Activity.RESULT_OK)
				finish()
			}
			.show()
	}
}