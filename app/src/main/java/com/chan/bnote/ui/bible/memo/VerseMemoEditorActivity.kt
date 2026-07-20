package com.chan.bnote.ui.bible.memo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.memo.VerseMemo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class VerseMemoEditorActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_BOOK_ID = "extra_book_id"
		private const val EXTRA_CHAPTER = "extra_chapter"
		private const val EXTRA_VERSE = "extra_verse"

		fun createIntent(context: Context, bookId: Int, chapter: Int, verse: Int): Intent {
			return Intent(context, VerseMemoEditorActivity::class.java).apply {
				putExtra(EXTRA_BOOK_ID, bookId)
				putExtra(EXTRA_CHAPTER, chapter)
				putExtra(EXTRA_VERSE, verse)
			}
		}
	}

	private class MemoBox(var existing: VerseMemo?, val root: View, val editText: EditText)

	private var bookId = 0
	private var chapter = 0
	private var verse = 0
	private var anyChangeMade = false

	private lateinit var container: LinearLayout
	private val boxes = mutableListOf<MemoBox>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_verse_memo_editor)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verse_memo_editor_root)) { v, insets ->
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

		bookId = intent.getIntExtra(EXTRA_BOOK_ID, 1)
		chapter = intent.getIntExtra(EXTRA_CHAPTER, 1)
		verse = intent.getIntExtra(EXTRA_VERSE, 1)

		val unit = BibleBooks.chapterUnit(bookId)
		findViewById<TextView>(R.id.text_top_bar_title).text =
			"${BibleBooks.nameOf(bookId)} ${chapter}${unit} ${verse}절 메모"

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finishWithResult() }
		container = findViewById(R.id.container_memo_boxes)
		findViewById<TextView>(R.id.btn_add_memo_box).setOnClickListener { addBox(existing = null) }

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
			val translation = AppSettings.getPrimaryTranslation(this@VerseMemoEditorActivity)
			val verseText = db.bibleDao().getVerses(translation, bookId, chapter)
				.find { it.verse == verse }?.text
			findViewById<TextView>(R.id.text_verse_preview).text = verseText ?: ""

			val existingMemos = db.verseMemoDao().getAtPosition(bookId, chapter, verse)
			if (existingMemos.isEmpty()) {
				addBox(existing = null)
			} else {
				for (memo in existingMemos) addBox(existing = memo)
			}
		}
	}

	private fun addBox(existing: VerseMemo?) {
		val boxView = LayoutInflater.from(this)
			.inflate(R.layout.item_verse_memo_box, container, false)

		val editText = boxView.findViewById<EditText>(R.id.edit_box_text)
		editText.setText(existing?.text ?: "")

		val box = MemoBox(existing, boxView, editText)
		boxes.add(box)
		container.addView(boxView)

		boxView.findViewById<ImageView>(R.id.btn_delete_box).setOnClickListener { removeBox(box) }
		boxView.findViewById<ImageView>(R.id.btn_save_box).setOnClickListener { saveBox(box) }
	}

	private fun removeBox(box: MemoBox) {
		val existing = box.existing
		if (existing == null) {
			boxes.remove(box)
			container.removeView(box.root)
			return
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("메모 삭제")
			.setMessage("이 메모를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				boxes.remove(box)
				container.removeView(box.root)
				anyChangeMade = true
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.verseMemoDao().delete(existing)
				}
			}
			.setNegativeButton("취소", null)
			.show()
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
					db.verseMemoDao().update(updated)
					box.existing = updated
				}
			} else {
				val newId = db.verseMemoDao().insert(
					VerseMemo(bookId = bookId, chapter = chapter, verse = verse, text = text)
				)
				box.existing = VerseMemo(
					id = newId,
					bookId = bookId,
					chapter = chapter,
					verse = verse,
					text = text
				)
			}
			anyChangeMade = true
			Toast.makeText(this@VerseMemoEditorActivity, "저장됐어요", Toast.LENGTH_SHORT).show()
		}
	}
}