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
import com.chan.bnote.data.Translation
import com.chan.bnote.ui.VerseAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	private lateinit var adapter: VerseAdapter
	private lateinit var textCurrentLocation: TextView
	private lateinit var btnTranslation: TextView

	private var currentBookId = 1
	private var currentChapter = 1
	private var primaryTranslation: Translation = Translation.GAEYEOK
	private var secondaryTranslation: Translation? = null

	private var currentFontSize: Int = 16

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val darkMode = com.chan.bnote.data.AppSettings.isDarkMode(this)
		androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
			if (darkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
			else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
		)

		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		textCurrentLocation = findViewById(R.id.text_current_location)
		btnTranslation = findViewById(R.id.btn_translation)
		btnTranslation.text = primaryTranslation.displayName

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.clipToPadding = false

		val bottomSpace = (resources.displayMetrics.heightPixels * 0.3f).toInt()
		recyclerView.setPadding(
			recyclerView.paddingLeft,
			recyclerView.paddingTop,
			recyclerView.paddingRight,
			bottomSpace
		)

		setupTopBarActions()
		setupBottomNavActions()

		currentFontSize = com.chan.bnote.data.AppSettings.getFontSize(applicationContext)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			BibleSeeder.seedIfEmpty(applicationContext, db)
			loadChapter(currentBookId, currentChapter)
		}
	}

	private fun setupTopBarActions() {
		textCurrentLocation.setOnClickListener {
			val sheet = com.chan.bnote.ui.BookChapterPickerBottomSheet(primaryTranslation.code)
			sheet.onVerseSelected = { bookId, chapter, verse ->
				loadChapter(bookId, chapter, scrollToVerse = verse)
			}
			sheet.show(supportFragmentManager, "book_chapter_picker")
		}

		btnTranslation.setOnClickListener {
			val sheet = com.chan.bnote.ui.TranslationPickerBottomSheet(primaryTranslation)
			sheet.onTranslationsSelected = { primary, secondary ->
				primaryTranslation = primary
				secondaryTranslation = secondary
				btnTranslation.text = primary.displayName
				loadChapter(currentBookId, currentChapter)
			}
			sheet.show(supportFragmentManager, "translation_picker")
		}

		findViewById<android.widget.ImageView>(R.id.btn_search).setOnClickListener {
			val sheet = com.chan.bnote.ui.SearchBottomSheet(primaryTranslation.code)
			sheet.onResultSelected = { bookId, chapter, verse ->
				loadChapter(bookId, chapter, scrollToVerse = verse)
			}
			sheet.show(supportFragmentManager, "search")
		}
		findViewById<TextView>(R.id.btn_favorites).setOnClickListener {
			val sheet = com.chan.bnote.ui.FavoritesBottomSheet()
			sheet.onVerseSelected = { bookId, chapter -> loadChapter(bookId, chapter) }
			sheet.show(supportFragmentManager, "favorites_list")
		}
		findViewById<android.widget.ImageView>(R.id.btn_menu).setOnClickListener {
			val sheet = com.chan.bnote.ui.MenuBottomSheet()
			sheet.onFontSizeChanged = { newSize ->
				currentFontSize = newSize
				adapter.updateFontSize(newSize)
			}
			sheet.onDarkModeChanged = {
				recreate() // 다크모드는 테마 전체가 바뀌니 액티비티 재시작
			}
			sheet.show(supportFragmentManager, "menu")
		}
	}

	private fun setupBottomNavActions() {
		findViewById<android.widget.ImageView>(R.id.btn_prev_chapter).setOnClickListener { goToPreviousChapter() }
		findViewById<android.widget.ImageView>(R.id.btn_next_chapter).setOnClickListener { goToNextChapter() }
		findViewById<android.widget.ImageView>(R.id.nav_sermon).setOnClickListener {
			Toast.makeText(this, "설교 탭 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		findViewById<android.widget.ImageView>(R.id.nav_mypage).setOnClickListener {
			Toast.makeText(this, "마이페이지 탭 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
	}

	private fun goToPreviousChapter() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			when {
				currentChapter > 1 -> loadChapter(currentBookId, currentChapter - 1)
				currentBookId > 1 -> {
					val prevBook = currentBookId - 1
					val maxChapter = db.bibleDao().getMaxChapter(primaryTranslation.code, prevBook)
					loadChapter(prevBook, maxChapter)
				}

				else -> Toast.makeText(this@MainActivity, "첫 장입니다", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun goToNextChapter() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter(primaryTranslation.code, currentBookId)
			when {
				currentChapter < maxChapter -> loadChapter(currentBookId, currentChapter + 1)
				currentBookId < 66 -> loadChapter(currentBookId + 1, 1)
				else -> Toast.makeText(this@MainActivity, "마지막 장입니다", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun loadChapter(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		currentBookId = bookId
		currentChapter = chapter
		textCurrentLocation.text = "${BibleBooks.nameOf(bookId)} ${chapter}장"

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val verses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
			val secondaryMap = secondaryTranslation?.let { sec ->
				db.bibleDao().getVerses(sec.code, bookId, chapter).associate { it.verse to it.text }
			}
			val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()

			adapter = VerseAdapter(
				verses = verses,
				secondaryTextByVerse = secondaryMap,
				bookmarks = bookmarkMap,
				fontSize = currentFontSize
			) { verseNum, current ->
				val verseText = verses.firstOrNull { it.verse == verseNum }?.text ?: ""
				val sheet = com.chan.bnote.ui.VerseActionBottomSheet(
					verseText = verseText,
					isHighlighted = current?.isHighlighted ?: false,
					isFavorite = current?.isFavorite ?: false,
					onToggleHighlight = {
						toggleField(
							verseNum,
							current,
							isHighlightToggle = true
						)
					},
					onToggleFavorite = { toggleField(verseNum, current, isHighlightToggle = false) }
				)
				sheet.show(supportFragmentManager, "verse_action")
			}
			findViewById<RecyclerView>(R.id.recycler_verses).adapter = adapter

			scrollToVerse?.let { verseNum ->
				val index = verses.indexOfFirst { it.verse == verseNum }
				if (index >= 0) findViewById<RecyclerView>(R.id.recycler_verses).scrollToPosition(
					index
				)
			}
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