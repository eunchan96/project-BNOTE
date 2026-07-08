package com.chan.bnote

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.BibleSeeder
import com.chan.bnote.ui.VerseAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	private lateinit var adapter: VerseAdapter
	private lateinit var textCurrentLocation: TextView

	// 현재 보고 있는 위치 (기본값: 창세기 1장)
	private var currentBookId = 1
	private var currentChapter = 1

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		textCurrentLocation = findViewById(R.id.text_current_location)

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(this)

		setupTopBarActions()
		setupBottomNavActions()

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			BibleSeeder.seedIfEmpty(applicationContext, db)
			loadChapter(currentBookId, currentChapter)
		}
	}

	private fun setupTopBarActions() {
		findViewById<TextView>(R.id.text_current_location).setOnClickListener {
			val sheet = com.chan.bnote.ui.BookChapterPickerBottomSheet()
			sheet.onChapterSelected = { bookId, chapter ->
				loadChapter(bookId, chapter)
			}
			sheet.show(supportFragmentManager, "book_chapter_picker")
		}

		// 대역본 선택 BottomSheet
		findViewById<TextView>(R.id.btn_translation).setOnClickListener {
			Toast.makeText(this, "대역본 선택 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		// 검색 화면/BottomSheet
		findViewById<TextView>(R.id.btn_search).setOnClickListener {
			Toast.makeText(this, "검색 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		// TODO 3단계: 즐겨찾기 목록 BottomSheet
		findViewById<TextView>(R.id.btn_favorites).setOnClickListener {
			Toast.makeText(this, "즐겨찾기 목록 (3단계에서 구현)", Toast.LENGTH_SHORT).show()
		}
		// 햄버거 메뉴
		findViewById<TextView>(R.id.btn_menu).setOnClickListener {
			Toast.makeText(this, "메뉴 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
	}

	private fun setupBottomNavActions() {
		// TODO: 설교/마이페이지 탭 (추후 구현). 지금은 "성경" 탭만 동작.
		findViewById<TextView>(R.id.nav_sermon).setOnClickListener {
			Toast.makeText(this, "설교 탭 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		findViewById<TextView>(R.id.nav_mypage).setOnClickListener {
			Toast.makeText(this, "마이페이지 탭 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
	}

	private fun loadChapter(bookId: Int, chapter: Int) {
		currentBookId = bookId
		currentChapter = chapter
		textCurrentLocation.text = "${BibleBooks.nameOf(bookId)} ${chapter}장"

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val verses = db.bibleDao().getVerses(bookId, chapter)
			val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()

			adapter = VerseAdapter(
				verses = verses,
				bookmarks = bookmarkMap,
				onToggleHighlight = { verseNum, current ->
					toggleField(
						verseNum,
						current,
						isHighlightToggle = true
					)
				},
				onToggleFavorite = { verseNum, current ->
					toggleField(
						verseNum,
						current,
						isHighlightToggle = false
					)
				}
			)
			findViewById<RecyclerView>(R.id.recycler_verses).adapter = adapter
		}
	}

	private fun toggleField(verseNum: Int, current: BibleBookmark?, isHighlightToggle: Boolean) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val updated = if (isHighlightToggle) {
				(current ?: BibleBookmark(
					bookId = currentBookId,
					chapter = currentChapter,
					verse = verseNum
				))
					.copy(
						isHighlighted = !(current?.isHighlighted ?: false),
						updatedAt = System.currentTimeMillis()
					)
			} else {
				(current ?: BibleBookmark(
					bookId = currentBookId,
					chapter = currentChapter,
					verse = verseNum
				))
					.copy(
						isFavorite = !(current?.isFavorite ?: false),
						updatedAt = System.currentTimeMillis()
					)
			}
			db.bookmarkDao().upsert(updated)

			val refreshed = db.bookmarkDao().getBookmarksForChapter(currentBookId, currentChapter)
				.associateBy { it.verse }.toMutableMap()
			adapter.updateBookmarks(refreshed)
		}
	}
}