package com.chan.bnote.ui.mypage

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
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.MemorizationVerse
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.sermon.BibleRangePickerBottomSheet
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
			startActivity(MemorizationPracticeActivity.allVersesIntent(this))
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
				onClick = { item ->
					startActivity(
						MemorizationVerseDetailActivity.createIntent(
							this@MemorizationVerseListActivity,
							item.id
						)
					)
				}
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
		// 구절이 여러 개면 구절 단위로 줄바꿈해서 저장한다 (단일 구절이면 그냥 한 줄).
		return parts.joinToString("\n")
	}

}