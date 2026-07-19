package com.chan.bnote.ui.bible

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
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

	/** 박스 하나의 상태. existing은 저장되면서 새로 생긴 id로 갱신될 수 있어 var로 둔다. */
	private class MemoBox(
		var existing: WordMemo?,
		val root: View,
		val editText: EditText,
		val checkbox: CheckBox
	)

	private lateinit var translation: String
	private var bookId = 0
	private var chapter = 0
	private var verse = 0
	private var startOffset = 0
	private var endOffset = 0
	private var wordText = ""
	private var anyChangeMade = false

	private lateinit var container: LinearLayout
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

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finishWithResult() }

		container = findViewById(R.id.container_memo_boxes)

		findViewById<TextView>(R.id.btn_add_memo_box).setOnClickListener {
			addBox(existing = null)
		}

		onBackPressedDispatcher.addCallback(this) { finishWithResult() }

		loadExisting()
	}

	private fun finishWithResult() {
		setResult(if (anyChangeMade) android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED)
		finish()
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
			findViewById<TextView>(R.id.text_top_bar_title).text =
				"${BibleBooks.nameOf(bookId)} ${chapter}${unit} ${verse}절 [$wordText] 메모"

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
			.inflate(R.layout.item_word_memo_box, container, false)

		val sourceView = boxView.findViewById<TextView>(R.id.text_box_source)
		if (existing?.sourceLabel != null) {
			sourceView.text = "출처: ${existing.sourceLabel}"
			sourceView.visibility = View.VISIBLE
		}

		val editText = boxView.findViewById<EditText>(R.id.edit_box_text)
		editText.setText(existing?.text ?: "")
		val checkbox = boxView.findViewById<CheckBox>(R.id.chk_box_propagate)

		val box = MemoBox(existing, boxView, editText, checkbox)
		boxes.add(box)
		container.addView(boxView)

		boxView.findViewById<ImageView>(R.id.btn_delete_box).setOnClickListener { removeBox(box) }
		boxView.findViewById<ImageView>(R.id.btn_save_box).setOnClickListener { saveBox(box) }
	}

	private fun removeBox(box: MemoBox) {
		boxes.remove(box)
		container.removeView(box.root)
		val existing = box.existing
		if (existing != null) {
			anyChangeMade = true
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(applicationContext)
				db.wordMemoDao().delete(existing)
			}
		}
	}

	private fun saveBox(box: MemoBox) {
		val text = box.editText.text.toString().trim()
		if (text.isEmpty()) {
			Toast.makeText(this, "메모 내용을 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val existing = box.existing
			if (existing != null) {
				if (existing.text != text) {
					val updated = existing.copy(text = text, updatedAt = System.currentTimeMillis())
					db.wordMemoDao().update(updated)
					box.existing = updated
				}
			} else {
				val newId = db.wordMemoDao().insert(
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
				box.existing = db.wordMemoDao().getById(newId)
			}
			anyChangeMade = true

			if (box.checkbox.isChecked) {
				box.checkbox.isChecked = false
				propagateToOtherVerses(db, text)
			} else {
				Toast.makeText(this@WordMemoEditorActivity, "저장됐어요", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private suspend fun propagateToOtherVerses(db: BibleDatabase, text: String) {
		if (wordText.isBlank()) return

		val matches = db.bibleDao().findVersesContainingExact(translation, wordText)
			.filter { !(it.bookId == bookId && it.chapter == chapter && it.verse == verse) }

		if (matches.isEmpty()) {
			Toast.makeText(this, "저장됐어요 (\"$wordText\"가 나오는 다른 구절은 못 찾았어요)", Toast.LENGTH_SHORT)
				.show()
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
						db.wordMemoDao().insert(
							WordMemo(
								translation = translation,
								bookId = verseRow.bookId,
								chapter = verseRow.chapter,
								verse = verseRow.verse,
								startOffset = idx,
								endOffset = idx + wordText.length,
								text = text,
								sourceLabel = originLabel
							)
						)
					}

					Toast.makeText(
						this@WordMemoEditorActivity,
						"저장됐고, ${matches.size}개 구절에도 추가됐어요",
						Toast.LENGTH_SHORT
					).show()
				}
			}
			.setNegativeButton("추가 안 함") { _, _ ->
				Toast.makeText(this@WordMemoEditorActivity, "저장됐어요", Toast.LENGTH_SHORT).show()
			}
			.show()
	}
}