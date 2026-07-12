package com.chan.bnote.ui

import TopBarActionHandler
import TopBarConfig
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.BibleSeeder
import com.chan.bnote.data.Translation
import kotlinx.coroutines.launch

class BibleFragment : Fragment(), TopBarActionHandler {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: VerseAdapter

	private var currentBookId = 1
	private var currentChapter = 1
	private var primaryTranslation: Translation = Translation.GAEYEOK
	private var secondaryTranslation: Translation? = null
	private var currentFontSize: Int = 16
	private var scrollSpeed = 3

	private var isReadingPlanEnabled = false
	private var isChapterRead = false
	private var isAutoScrollEnabled = false
	private var isAutoScrolling = false
	private var hasSermonForChapter = false

	private val autoScrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
	private val autoScrollRunnable = object : Runnable {
		override fun run() {
			recyclerView.smoothScrollBy(0, 2 + scrollSpeed) // 속도 1~5 -> 3~7px씩
			autoScrollHandler.postDelayed(this, 60L - (scrollSpeed * 8)) // 속도 1~5 -> 52~20ms 간격
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_bible, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		currentFontSize = AppSettings.getFontSize(requireContext())
		isReadingPlanEnabled = AppSettings.isReadingPlanEnabled(requireContext())
		isAutoScrollEnabled = AppSettings.isAutoScrollEnabled(requireContext())
		scrollSpeed = AppSettings.getScrollSpeed(requireContext())

		recyclerView = view.findViewById(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		recyclerView.clipToPadding = false

		val bottomSpace = (resources.displayMetrics.heightPixels * 0.3f).toInt()
		recyclerView.setPadding(
			recyclerView.paddingLeft,
			recyclerView.paddingTop,
			recyclerView.paddingRight,
			bottomSpace
		)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			BibleSeeder.seedIfEmpty(requireContext().applicationContext, db)
			loadChapter(currentBookId, currentChapter)
		}
	}

	override fun getTopBarConfig() = TopBarConfig(
		title = "${BibleBooks.nameOf(currentBookId)} ${currentChapter}장",
		showTranslationButton = true,
		showSearch = true,
		showFavorites = true,
		showMenu = true,
		showChapterNav = true,
		showReadingPlanCheck = isReadingPlanEnabled,
		isChapterRead = isChapterRead,
		showAutoScrollButton = isAutoScrollEnabled,
		isAutoScrolling = isAutoScrolling,
		showSermonIcon = hasSermonForChapter
	)

	override fun onLocationClicked() {
		val sheet = BookChapterPickerBottomSheet(primaryTranslation.code)
		sheet.onVerseSelected = { bookId, chapter, verse ->
			loadChapter(bookId, chapter, scrollToVerse = verse)
		}
		sheet.show(parentFragmentManager, "book_chapter_picker")
	}

	override fun onTranslationClicked() {
		val sheet = TranslationPickerBottomSheet(primaryTranslation)
		sheet.onTranslationsSelected = { primary, secondary ->
			primaryTranslation = primary
			secondaryTranslation = secondary
			loadChapter(currentBookId, currentChapter)
		}
		sheet.show(parentFragmentManager, "translation_picker")
	}

	override fun onSearchClicked() {
		val sheet = SearchBottomSheet(primaryTranslation.code)
		sheet.onResultSelected = { bookId, chapter, verse ->
			loadChapter(bookId, chapter, scrollToVerse = verse)
		}
		sheet.show(parentFragmentManager, "search")
	}

	override fun onFavoritesClicked() {
		val sheet = FavoritesBottomSheet()
		sheet.onVerseSelected = { bookId, chapter -> loadChapter(bookId, chapter) }
		sheet.show(parentFragmentManager, "favorites_list")
	}

	override fun onMenuClicked() {
		val dialog = BibleMenuDialogFragment(
			isReadingPlanEnabled = isReadingPlanEnabled,
			isAutoScrollEnabled = isAutoScrollEnabled
		)
		dialog.onAppendixItemSelected = { itemName ->
			// TODO: 번역본/버전 정해지면 실제 본문 화면 연결
			Toast.makeText(requireContext(), "$itemName (본문 준비 중)", Toast.LENGTH_SHORT).show()
		}
		dialog.onReadingPlanToggled = { enabled ->
			isReadingPlanEnabled = enabled
			AppSettings.setReadingPlanEnabled(requireContext(), enabled)
			notifyTopBarChanged()
		}
		dialog.onAutoScrollToggled = { enabled ->
			isAutoScrollEnabled = enabled
			AppSettings.setAutoScrollEnabled(requireContext(), enabled)
			if (!enabled) {
				isAutoScrolling = false
				stopAutoScroll()
			}
			notifyTopBarChanged()
		}
		dialog.show(parentFragmentManager, "bible_menu")
	}

	override fun onPrevChapterClicked() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			when {
				currentChapter > 1 -> loadChapter(currentBookId, currentChapter - 1)
				currentBookId > 1 -> {
					val prevBook = currentBookId - 1
					val maxChapter = db.bibleDao().getMaxChapter(primaryTranslation.code, prevBook)
					loadChapter(prevBook, maxChapter)
				}

				else -> Toast.makeText(requireContext(), "첫 장입니다", Toast.LENGTH_SHORT).show()
			}
		}
	}

	override fun onNextChapterClicked() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter(primaryTranslation.code, currentBookId)
			when {
				currentChapter < maxChapter -> loadChapter(currentBookId, currentChapter + 1)
				currentBookId < 66 -> loadChapter(currentBookId + 1, 1)
				else -> Toast.makeText(requireContext(), "마지막 장입니다", Toast.LENGTH_SHORT).show()
			}
		}
	}

	override fun onSermonIconClicked() {
		ChapterSermonsBottomSheet(currentBookId, currentChapter)
			.show(parentFragmentManager, "chapter_sermons")
	}

	private fun loadChapter(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		currentBookId = bookId
		currentChapter = chapter

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val verses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
			val secondaryMap = secondaryTranslation?.let { sec ->
				db.bibleDao().getVerses(sec.code, bookId, chapter).associate { it.verse to it.text }
			}
			val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()
			isChapterRead = db.readingProgressDao().get(bookId, chapter) != null
			hasSermonForChapter =
				db.sermonDao().getByBookChapter(bookId, chapter).isNotEmpty() // 추가

			notifyTopBarChanged()

			// 이하 어댑터 생성 로직 기존과 동일
			adapter = VerseAdapter(
				verses = verses,
				secondaryTextByVerse = secondaryMap,
				bookmarks = bookmarkMap,
				fontSize = currentFontSize
			) { verseNum, current ->
				val verseText = verses.firstOrNull { it.verse == verseNum }?.text ?: ""
				val sheet = VerseActionBottomSheet(
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
				sheet.show(parentFragmentManager, "verse_action")
			}
			recyclerView.adapter = adapter

			scrollToVerse?.let { verseNum ->
				val index = verses.indexOfFirst { it.verse == verseNum }
				if (index >= 0) recyclerView.scrollToPosition(index)
			}
		}
	}

	private fun toggleField(verseNum: Int, current: BibleBookmark?, isHighlightToggle: Boolean) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
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

	private fun startAutoScroll() {
		autoScrollHandler.removeCallbacks(autoScrollRunnable)
		autoScrollHandler.post(autoScrollRunnable)
	}

	private fun stopAutoScroll() {
		autoScrollHandler.removeCallbacks(autoScrollRunnable)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		stopAutoScroll() // 화면 벗어나면 반드시 정지 (메모리 누수 방지)
	}

	private fun notifyTopBarChanged() {
		(activity as? TopBarConfigListener)?.onTopBarConfigChanged(getTopBarConfig())
	}

	override fun onAutoScrollButtonClicked() {
		isAutoScrolling = !isAutoScrolling
		if (isAutoScrolling) startAutoScroll() else stopAutoScroll()
		notifyTopBarChanged()
	}

	override fun onReadingPlanCheckClicked() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			if (isChapterRead) {
				db.readingProgressDao().delete(currentBookId, currentChapter)
			} else {
				db.readingProgressDao().upsert(
					com.chan.bnote.data.ReadingProgress(
						bookId = currentBookId,
						chapter = currentChapter
					)
				)
			}
			isChapterRead = !isChapterRead
			notifyTopBarChanged()
		}
	}
}