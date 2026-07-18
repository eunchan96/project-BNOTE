package com.chan.bnote.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.MemorizationVerse
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.sermon.BibleRangePickerBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MemorizationVerseListActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyStateText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		recyclerView = findViewById(R.id.recycler_memorization_verses)
		emptyStateText = findViewById(R.id.text_empty_state)
		recyclerView.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_practice).setOnClickListener {
			startActivity(Intent(this, MemorizationPracticeActivity::class.java))
		}
		findViewById<TextView>(R.id.btn_add_verse).setOnClickListener { showAddPicker() }
	}

	override fun onResume() {
		super.onResume()
		loadItems()
	}

	private fun loadItems() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val items = db.memorizationVerseDao().getAll()

			emptyStateText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.adapter = MemorizationVerseAdapter(
				items = items,
				onClick = { item -> navigateToBible(item.startBookId, item.startChapter) },
				onDelete = { item -> confirmDelete(item) }
			)
		}
	}

	private fun showAddPicker() {
		val rangePicker = BibleRangePickerBottomSheet()
		rangePicker.onRangeSelected = { ref -> addVerse(ref) }
		rangePicker.show(supportFragmentManager, "memorization_verse_picker")
	}

	private fun addVerse(ref: SermonBibleRef) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val alreadyExists = db.memorizationVerseDao().existsCount(
				ref.startBookId, ref.startChapter, ref.startVerse,
				ref.endBookId, ref.endChapter, ref.endVerse
			) > 0
			if (alreadyExists) {
				Toast.makeText(
					this@MemorizationVerseListActivity,
					"이미 등록된 구절이에요",
					Toast.LENGTH_SHORT
				).show()
				return@launch
			}

			val verseText = buildVerseText(ref)
			db.memorizationVerseDao().insert(
				MemorizationVerse(
					startBookId = ref.startBookId,
					startChapter = ref.startChapter,
					startVerse = ref.startVerse,
					endBookId = ref.endBookId,
					endChapter = ref.endChapter,
					endVerse = ref.endVerse,
					verseText = verseText
				)
			)
			loadItems()
		}
	}

	private suspend fun buildVerseText(ref: SermonBibleRef): String {
		val db = BibleDatabase.getInstance(applicationContext)
		val parts = mutableListOf<String>()
		for (chapter in ref.startChapter..ref.endChapter) {
			val verses = db.bibleDao().getVerses("NKRV", ref.startBookId, chapter)
			val filtered = verses.filter { v ->
				when {
					ref.startChapter == ref.endChapter -> v.verse in ref.startVerse..ref.endVerse
					chapter == ref.startChapter -> v.verse >= ref.startVerse
					chapter == ref.endChapter -> v.verse <= ref.endVerse
					else -> true
				}
			}
			parts.addAll(filtered.map { it.text })
		}
		return parts.joinToString(" ")
	}

	private fun confirmDelete(item: MemorizationVerse) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("암송 구절 삭제")
			.setMessage("'${item.toDisplayLabel()}'를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.memorizationVerseDao().delete(item)
					loadItems()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun navigateToBible(bookId: Int, chapter: Int) {
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		startActivity(intent)
	}
}