package com.chan.bnote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.ui.VerseAdapter
import kotlinx.coroutines.launch

class VerseListActivity : AppCompatActivity() {

	private lateinit var adapter: VerseAdapter
	private var bookId = -1
	private var chapter = -1

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_verse_list)

		bookId = intent.getIntExtra("bookId", -1)
		val bookName = intent.getStringExtra("bookName") ?: ""
		chapter = intent.getIntExtra("chapter", -1)
		title = "$bookName ${chapter}장"

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			val verses = db.bibleDao().getVerses(bookId, chapter)
			val bookmarkList = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
			val bookmarkMap = bookmarkList.associateBy { it.verse }.toMutableMap()

			adapter = VerseAdapter(
				verses = verses,
				bookmarks = bookmarkMap,
				onToggleHighlight = { verseNum, current ->
					toggleField(verseNum, current, isHighlightToggle = true)
				},
				onToggleFavorite = { verseNum, current ->
					toggleField(verseNum, current, isHighlightToggle = false)
				}
			)
			recyclerView.adapter = adapter
		}
	}

	private fun toggleField(verseNum: Int, current: BibleBookmark?, isHighlightToggle: Boolean) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val updated = if (isHighlightToggle) {
				(current ?: BibleBookmark(bookId = bookId, chapter = chapter, verse = verseNum))
					.copy(
						isHighlighted = !(current?.isHighlighted ?: false),
						updatedAt = System.currentTimeMillis()
					)
			} else {
				(current ?: BibleBookmark(bookId = bookId, chapter = chapter, verse = verseNum))
					.copy(
						isFavorite = !(current?.isFavorite ?: false),
						updatedAt = System.currentTimeMillis()
					)
			}
			db.bookmarkDao().upsert(updated)

			val refreshed = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()
			adapter.updateBookmarks(refreshed)
		}
	}
}