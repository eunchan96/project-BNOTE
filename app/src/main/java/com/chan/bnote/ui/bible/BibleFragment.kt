package com.chan.bnote.ui.bible

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleSeeder
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.Translation
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.data.bible.scrap.Scrap
import com.chan.bnote.data.mypage.CopyFormatter
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.TopBarConfigListener
import com.chan.bnote.ui.appendix.AppendixTextActivity
import com.chan.bnote.ui.appendix.AppendixTextType
import com.chan.bnote.ui.appendix.ResponsiveReadingListActivity
import com.chan.bnote.ui.appendix.TenCommandmentsActivity
import com.chan.bnote.ui.bible.hymn.HymnListActivity
import com.chan.bnote.ui.bible.memo.MemoListActivity
import com.chan.bnote.ui.bible.memo.VerseMemoEditorActivity
import com.chan.bnote.ui.bible.memo.WordMemoEditorActivity
import com.chan.bnote.ui.bible.picker.BookChapterPickerBottomSheet
import com.chan.bnote.ui.bible.picker.TranslationPickerBottomSheet
import com.chan.bnote.ui.bible.scrap.ScrapActivity
import com.chan.bnote.ui.bible.scrap.ScrapGroupPickerBottomSheet
import com.chan.bnote.ui.common.ColorPickerBottomSheet
import com.chan.bnote.ui.common.HighlightColors
import com.chan.bnote.ui.knowledge.BibleKnowledgeHubActivity
import kotlinx.coroutines.launch

class BibleFragment : Fragment(), TopBarActionHandler {

	companion object {
		private const val ARG_BOOK_ID = "bookId"
		private const val ARG_CHAPTER = "chapter"

		fun newInstance(bookId: Int, chapter: Int): BibleFragment {
			val fragment = BibleFragment()
			fragment.arguments = android.os.Bundle().apply {
				putInt(ARG_BOOK_ID, bookId)
				putInt(ARG_CHAPTER, chapter)
			}
			return fragment
		}
	}

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: VerseAdapter

	private var currentBookId = 1
	private var currentChapter = 1
	private var primaryTranslation: Translation = Translation.NKRV
	private var secondaryTranslation: Translation? = null
	private var currentFontSize: Int = 16
	private var scrollSpeed = 3

	private var isReadingPlanEnabled = false
	private var isChapterRead = false
	private var isAutoScrollEnabled = false
	private var isAutoScrolling = false
	private var hasSermonForChapter = false

	private val selectedVerses = mutableSetOf<Int>()
	private lateinit var selectionToolbar: View

	private val verseMemoEditorLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			lifecycleScope.launch { refreshMemos() }
		}
	}

	private val wordMemoEditorLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			lifecycleScope.launch { refreshMemos() }
		}
	}

	private var currentVerses: List<BibleVerse> = emptyList()
	private var currentSecondaryMap: Map<Int, String>? = null
	private var currentHighlights: Map<Int, List<PartialHighlight>> = emptyMap()

	private lateinit var highlightColorToolbar: View
	private var pendingHighlightVerses: List<Int>? = null   // 절 탭 선택 → 전체 하이라이트용
	private var pendingHighlightRange: Triple<Int, Int, Int>? =
		null // 부분 하이라이트용 (verse, start, end)

	private var currentVerseMemos: Map<Int, VerseMemo> = emptyMap()
	private var currentWordMemos: Map<Int, List<WordMemo>> = emptyMap()

	private val autoScrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
	private val autoScrollRunnable = object : Runnable {
		override fun run() {
			recyclerView.smoothScrollBy(0, 2 + scrollSpeed) // 속도 1~5 -> 3~7px씩
			autoScrollHandler.postDelayed(this, 60L - (scrollSpeed * 8)) // 속도 1~5 -> 52~20ms 간격
		}
	}

	private val scrapLauncher = registerForActivityResult(
		androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == android.app.Activity.RESULT_OK) {
			val bookId = result.data?.getIntExtra("bookId", -1) ?: return@registerForActivityResult
			val chapter =
				result.data?.getIntExtra("chapter", -1) ?: return@registerForActivityResult
			val verse = result.data?.getIntExtra("verse", -1) ?: return@registerForActivityResult
			if (bookId > 0 && chapter > 0) {
				loadChapter(bookId, chapter, scrollToVerse = if (verse > 0) verse else null)
			}
		}
	}

	private val bibleSearchLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val data = result.data ?: return@registerForActivityResult
			val bookId = data.getIntExtra(BibleSearchActivity.EXTRA_RESULT_BOOK_ID, -1)
			val chapter = data.getIntExtra(BibleSearchActivity.EXTRA_RESULT_CHAPTER, -1)
			val verse = data.getIntExtra(BibleSearchActivity.EXTRA_RESULT_VERSE, -1)
			if (bookId > 0 && chapter > 0) {
				loadChapter(bookId, chapter, scrollToVerse = if (verse > 0) verse else null)
			}
		}
	}

	private val bookmarkLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val data = result.data ?: return@registerForActivityResult
			val bookId = data.getIntExtra(BookmarkListActivity.EXTRA_RESULT_BOOK_ID, -1)
			val chapter = data.getIntExtra(BookmarkListActivity.EXTRA_RESULT_CHAPTER, -1)
			if (bookId > 0 && chapter > 0) {
				loadChapter(bookId, chapter)
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_bible, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val savedPrimaryCode = AppSettings.getPrimaryTranslation(requireContext())
		primaryTranslation =
			Translation.values().firstOrNull { it.code == savedPrimaryCode } ?: Translation.NKRV

		val savedSecondaryCode = AppSettings.getSecondaryTranslation(requireContext())
		secondaryTranslation = Translation.values().firstOrNull { it.code == savedSecondaryCode }

		currentFontSize = AppSettings.getFontSize(requireContext())
		isReadingPlanEnabled = AppSettings.isReadingPlanEnabled(requireContext())
		isAutoScrollEnabled = AppSettings.isAutoScrollEnabled(requireContext())
		scrollSpeed = AppSettings.getScrollSpeed(requireContext())

		recyclerView = view.findViewById(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		recyclerView.clipToPadding = false

		selectionToolbar = view.findViewById(R.id.container_selection_toolbar)
		highlightColorToolbar = view.findViewById(R.id.scroll_highlight_toolbar)

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

			val startBookId = arguments?.getInt(ARG_BOOK_ID)
				?: AppSettings.getLastReadBookId(requireContext())
			val startChapter = arguments?.getInt(ARG_CHAPTER)
				?: AppSettings.getLastReadChapter(requireContext())
			loadChapter(startBookId, startChapter)
		}

		view.findViewById<TextView>(R.id.btn_cancel_selection).setOnClickListener {
			clearSelection()
		}
		view.findViewById<TextView>(R.id.btn_toolbar_bookmark).setOnClickListener {
			onBookmarkButtonClicked()
		}
		view.findViewById<TextView>(R.id.btn_toolbar_scrap).setOnClickListener {
			onScrapButtonClicked()
		}
		view.findViewById<TextView>(R.id.btn_toolbar_highlight).setOnClickListener {
			pendingHighlightVerses = selectedVerses.toList()
			pendingHighlightRange = null
			showHighlightColorToolbar()
		}
		view.findViewById<TextView>(R.id.btn_toolbar_copy).setOnClickListener {
			onCopyButtonClicked()
		}
		view.findViewById<TextView>(R.id.btn_cancel_highlight_toolbar).setOnClickListener {
			hideHighlightColorToolbar()
		}
		view.findViewById<TextView>(R.id.btn_remove_highlight).setOnClickListener {
			removeHighlight()
		}
		view.findViewById<TextView>(R.id.btn_toolbar_memo).setOnClickListener {
			onMemoButtonClicked()
		}
	}

	override fun getTopBarConfig() = TopBarConfig(
		title = "${BibleBooks.nameOf(currentBookId)} ${currentChapter}${
			BibleBooks.chapterUnit(
				currentBookId
			)
		}",
		showTranslationButton = true,
		showSearch = true,
		showBookmarks = true,
		showMenu = true,
		showChapterNav = true,
		showReadingPlanCheck = isReadingPlanEnabled,
		isChapterRead = isChapterRead,
		showAutoScrollButton = isAutoScrollEnabled,
		isAutoScrolling = isAutoScrolling,
		showSermonIcon = hasSermonForChapter
	)

	override fun onLocationClicked() {
		val sheet = BookChapterPickerBottomSheet(primaryTranslation.code, currentBookId)
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
			AppSettings.setPrimaryTranslation(requireContext(), primary.code)
			AppSettings.setSecondaryTranslation(requireContext(), secondary?.code)
			loadChapter(currentBookId, currentChapter)
		}
		sheet.show(parentFragmentManager, "translation_picker")
	}

	override fun onSearchClicked() {
		bibleSearchLauncher.launch(
			BibleSearchActivity.createIntent(
				requireContext(),
				primaryTranslation.code
			)
		)
	}

	override fun onBookmarksClicked() {
		bookmarkLauncher.launch(BookmarkListActivity.createIntent(requireContext()))
	}

	override fun onMenuClicked() {
		val dialog = BibleMenuDialogFragment(
			isReadingPlanEnabled = isReadingPlanEnabled,
			isAutoScrollEnabled = isAutoScrollEnabled
		)
		dialog.onScrapClicked = {
			scrapLauncher.launch(
				android.content.Intent(
					requireContext(),
					ScrapActivity::class.java
				)
			)
		}
		dialog.onHymnClicked = {
			HymnListActivity.start(requireContext())
		}
		dialog.onHighlightClicked = {
			startActivity(Intent(requireContext(), HighlightListActivity::class.java))
		}
		dialog.onMemoClicked = {
			startActivity(MemoListActivity.verseMemoIntent(requireContext()))
		}
		dialog.onAppendixItemSelected = { itemName ->
			when (itemName) {
				"주기도문" -> AppendixTextActivity.start(
					requireContext(),
					AppendixTextType.LORDS_PRAYER
				)

				"사도신경" -> AppendixTextActivity.start(
					requireContext(),
					AppendixTextType.APOSTLES_CREED
				)

				"십계명" -> startActivity(
					Intent(
						requireContext(),
						TenCommandmentsActivity::class.java
					)
				)

				"교독문" -> startActivity(
					Intent(
						requireContext(),
						ResponsiveReadingListActivity::class.java
					)
				)
			}
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
		dialog.onBibleKnowledgeClicked = {
			startActivity(Intent(requireContext(), BibleKnowledgeHubActivity::class.java))
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

	/** 탭이 유지되는 프래그먼트라 onViewCreated는 다시 안 불리므로, 설정 화면에서 바뀐 값(글자 크기 등)을
	 * 여기서 다시 확인해서 반영한다. hide()/show() 방식의 탭 전환은 onResume이 아니라 이 콜백을 탄다. */
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) refreshFontSizeIfChanged()
	}

	override fun onResume() {
		super.onResume()
		refreshFontSizeIfChanged()
	}

	private fun refreshFontSizeIfChanged() {
		if (!::adapter.isInitialized) return
		val newFontSize = AppSettings.getFontSize(requireContext())
		if (newFontSize != currentFontSize) {
			currentFontSize = newFontSize
			adapter.updateFontSize(currentFontSize)
		}
	}

	override fun onSermonIconClicked() {
		ChapterSermonsActivity.start(requireContext(), currentBookId, currentChapter)
	}

	/** 탭을 새로 만들지 않고 기존 인스턴스에서 특정 장으로 이동할 때 (MainActivity에서 호출). */
	fun navigateTo(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		loadChapter(bookId, chapter, scrollToVerse)
	}

	private fun loadChapter(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		currentBookId = bookId
		currentChapter = chapter
		clearSelection()
		AppSettings.setLastRead(requireContext(), bookId, chapter)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			db.recentChapterViewDao().upsert(
				com.chan.bnote.data.mypage.RecentChapterView(bookId = bookId, chapter = chapter)
			)
			val verses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
			val secondaryMap = secondaryTranslation?.let { sec ->
				db.bibleDao().getVerses(sec.code, bookId, chapter).associate { it.verse to it.text }
			}
			currentVerses = verses
			currentSecondaryMap = secondaryMap

			val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()
			isChapterRead = db.readingProgressDao().get(bookId, chapter) != null
			hasSermonForChapter =
				db.sermonDao().getByBookChapter(bookId, chapter).isNotEmpty()

			val highlights = db.partialHighlightDao()
				.getForChapter(primaryTranslation.code, bookId, chapter)
				.groupBy { it.verse }
			currentHighlights = highlights

			val verseMemos =
				db.verseMemoDao().getForChapter(bookId, chapter).associateBy { it.verse }
			val wordMemos = db.wordMemoDao().getForChapter(primaryTranslation.code, bookId, chapter)
				.groupBy { it.verse }
			currentVerseMemos = verseMemos
			currentWordMemos = wordMemos

			notifyTopBarChanged()

			adapter = VerseAdapter(
				verses = verses,
				secondaryTextByVerse = secondaryMap,
				bookmarks = bookmarkMap,
				fontSize = currentFontSize,
				selectedVerses = selectedVerses,
				highlightsByVerse = currentHighlights,
				verseMemos = currentVerseMemos,
				wordMemosByVerse = currentWordMemos,
				onVerseTap = { verseNum -> toggleVerseSelection(verseNum) },
				onVerseMemoView = { verseNum, memo -> showVerseMemoDialog(verseNum, memo) },
				onHighlightRequested = { verseNum, start, end ->
					pendingHighlightRange = Triple(verseNum, start, end)
					pendingHighlightVerses = null
					showHighlightColorToolbar()
				},
				onWordMemoCreate = { verseNum, start, end ->
					showWordMemoEditDialog(
						verseNum,
						start,
						end,
						null
					)
				},
				onWordMemoView = { verseNum, memo -> showWordMemoViewDialog(verseNum, memo) }
			)
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
						isBookmarked = !(current?.isBookmarked ?: false),
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
		if (isHidden) return // 숨겨진(다른 탭이 보이는) 상태에서는 상단바를 건드리면 안 된다
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
					ReadingProgress(
						bookId = currentBookId,
						chapter = currentChapter
					)
				)
			}
			isChapterRead = !isChapterRead
			notifyTopBarChanged()
		}
	}

	private fun toggleVerseSelection(verseNum: Int) {
		if (selectedVerses.contains(verseNum)) {
			selectedVerses.remove(verseNum)
		} else {
			selectedVerses.add(verseNum)
		}
		adapter.updateSelection(selectedVerses.toSet())
		updateToolbarVisibility()
	}

	private fun clearSelection() {
		selectedVerses.clear()
		if (::adapter.isInitialized) adapter.updateSelection(emptySet())
		selectionToolbar.visibility = View.GONE
		highlightColorToolbar.visibility = View.GONE
		pendingHighlightVerses = null
		pendingHighlightRange = null
	}

	private fun updateToolbarVisibility() {
		if (selectedVerses.isEmpty()) {
			selectionToolbar.visibility = View.GONE
			highlightColorToolbar.visibility = View.GONE
			pendingHighlightVerses = null
			return
		}
		selectionToolbar.visibility = View.VISIBLE
		val singleSelected = selectedVerses.size == 1
		view?.findViewById<TextView>(R.id.btn_toolbar_bookmark)?.visibility =
			if (singleSelected) View.VISIBLE else View.GONE
		view?.findViewById<TextView>(R.id.btn_toolbar_memo)?.visibility =
			if (singleSelected) View.VISIBLE else View.GONE
	}

	private fun onBookmarkButtonClicked() {
		val verseNum = selectedVerses.firstOrNull() ?: return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val current = db.bookmarkDao().getBookmarksForChapter(currentBookId, currentChapter)
				.firstOrNull { it.verse == verseNum }

			val updated = (current ?: BibleBookmark(
				bookId = currentBookId,
				chapter = currentChapter,
				verse = verseNum
			))
				.copy(
					isBookmarked = !(current?.isBookmarked ?: false),
					updatedAt = System.currentTimeMillis()
				)
			db.bookmarkDao().upsert(updated)

			val refreshed = db.bookmarkDao().getBookmarksForChapter(currentBookId, currentChapter)
				.associateBy { it.verse }.toMutableMap()
			adapter.updateBookmarks(refreshed)

			val message = if (updated.isBookmarked) "북마크에 추가했어요" else "북마크를 해제했어요"
			Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
			clearSelection()
		}
	}

	private fun onCopyButtonClicked() {
		if (selectedVerses.isEmpty()) return

		val includeSecondary = AppSettings.isCopyIncludeSecondary(requireContext())
		val referenceStyle = AppSettings.getCopyReferenceStyle(requireContext())

		val text = CopyFormatter.format(
			bookId = currentBookId,
			chapter = currentChapter,
			verses = currentVerses,
			selectedVerseNumbers = selectedVerses,
			secondaryMap = if (includeSecondary) currentSecondaryMap else null,
			includeSecondary = includeSecondary,
			referenceStyle = referenceStyle
		)

		val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
				as android.content.ClipboardManager
		clipboard.setPrimaryClip(android.content.ClipData.newPlainText("bible_verses", text))

		Toast.makeText(requireContext(), "복사했어요", Toast.LENGTH_SHORT).show()
		clearSelection()
	}

	private fun onScrapButtonClicked() {
		if (selectedVerses.isEmpty()) return
		val sortedSelected = selectedVerses.sorted()
		val startVerse = sortedSelected.first()
		val endVerse = sortedSelected.last()
		val combinedText = currentVerses
			.filter { it.verse in selectedVerses }
			.sortedBy { it.verse }
			.joinToString("\n") { it.text }

		val picker = ScrapGroupPickerBottomSheet()
		picker.onGroupSelected = { group ->
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(requireContext().applicationContext)
				db.scrapDao().insertScrap(
					Scrap(
						groupId = group.id,
						bookId = currentBookId,
						chapter = currentChapter,
						startVerse = startVerse,
						endVerse = endVerse,
						verseText = combinedText
					)
				)
				Toast.makeText(requireContext(), "'${group.name}'에 스크랩했어요", Toast.LENGTH_SHORT)
					.show()
				clearSelection()
			}
		}
		picker.show(parentFragmentManager, "scrap_group_picker")
	}

	private fun openHighlightColorPicker(verseNum: Int, start: Int, end: Int) {
		val picker = ColorPickerBottomSheet(includeNoneOption = true)
		picker.onColorSelected = { colorHex ->
			if (colorHex.isEmpty()) {
				clearHighlight(verseNum, start, end)
			} else {
				saveHighlight(verseNum, start, end, colorHex)
			}
		}
		picker.show(parentFragmentManager, "highlight_color_picker")
	}

	private fun clearHighlight(verseNum: Int, start: Int, end: Int) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			db.partialHighlightDao().deleteOverlapping(
				translation = primaryTranslation.code,
				bookId = currentBookId,
				chapter = currentChapter,
				verse = verseNum,
				start = start,
				end = end
			)
			val refreshed = db.partialHighlightDao()
				.getForChapter(primaryTranslation.code, currentBookId, currentChapter)
				.groupBy { it.verse }
			currentHighlights = refreshed
			adapter.updateHighlights(refreshed)
		}
	}

	private fun saveHighlight(verseNum: Int, start: Int, end: Int, colorHex: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			db.partialHighlightDao().insert(
				PartialHighlight(
					translation = primaryTranslation.code,
					bookId = currentBookId,
					chapter = currentChapter,
					verse = verseNum,
					startOffset = start,
					endOffset = end,
					colorHex = colorHex
				)
			)
			val refreshed = db.partialHighlightDao()
				.getForChapter(primaryTranslation.code, currentBookId, currentChapter)
				.groupBy { it.verse }
			currentHighlights = refreshed
			adapter.updateHighlights(refreshed)
		}
	}

	private fun showHighlightColorToolbar() {
		selectionToolbar.visibility = View.GONE
		populateHighlightSwatches()
		view?.findViewById<TextView>(R.id.btn_remove_highlight)?.visibility =
			if (hasExistingHighlightForPending()) View.VISIBLE else View.GONE
		highlightColorToolbar.visibility = View.VISIBLE
	}

	private fun hasExistingHighlightForPending(): Boolean {
		pendingHighlightRange?.let { (verseNum, start, end) ->
			val overlaps = currentHighlights[verseNum]?.any { h ->
				!(end <= h.startOffset || start >= h.endOffset)
			} ?: false
			return overlaps
		}
		pendingHighlightVerses?.let { verseNums ->
			return verseNums.any { verseNum -> !currentHighlights[verseNum].isNullOrEmpty() }
		}
		return false
	}

	private fun hideHighlightColorToolbar() {
		highlightColorToolbar.visibility = View.GONE
		pendingHighlightVerses = null
		pendingHighlightRange = null
		updateToolbarVisibility() // 선택 중이던 절이 남아있으면 선택 툴바 복귀
	}

	private fun populateHighlightSwatches() {
		val container =
			view?.findViewById<android.widget.LinearLayout>(R.id.container_highlight_swatches)
				?: return
		container.removeAllViews()
		val size = (28 * resources.displayMetrics.density).toInt()
		val margin = (4 * resources.displayMetrics.density).toInt()

		for (colorHex in HighlightColors.palette) {
			val swatch = View(requireContext())
			val params = android.widget.LinearLayout.LayoutParams(size, size)
			params.setMargins(margin, margin, margin, margin)
			swatch.layoutParams = params

			val drawable = android.graphics.drawable.GradientDrawable()
			drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
			drawable.setColor(android.graphics.Color.parseColor(colorHex))
			drawable.setStroke(1, android.graphics.Color.parseColor("#55FFFFFF"))
			swatch.background = drawable

			swatch.setOnClickListener { applyHighlightColor(colorHex) }
			container.addView(swatch)
		}
	}

	private fun applyHighlightColor(colorHex: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			pendingHighlightRange?.let { (verseNum, start, end) ->
				db.partialHighlightDao().insert(
					PartialHighlight(
						translation = primaryTranslation.code,
						bookId = currentBookId,
						chapter = currentChapter,
						verse = verseNum,
						startOffset = start,
						endOffset = end,
						colorHex = colorHex
					)
				)
			}

			pendingHighlightVerses?.let { verseNums ->
				for (verseNum in verseNums) {
					db.partialHighlightDao().deleteAllForVerse(
						primaryTranslation.code,
						currentBookId,
						currentChapter,
						verseNum
					)
					val verseData = currentVerses.firstOrNull { it.verse == verseNum } ?: continue
					db.partialHighlightDao().insert(
						PartialHighlight(
							translation = primaryTranslation.code,
							bookId = currentBookId,
							chapter = currentChapter,
							verse = verseNum,
							startOffset = 0,
							endOffset = verseData.text.length,
							colorHex = colorHex
						)
					)
				}
			}

			refreshHighlights()
			hideHighlightColorToolbar()
			clearSelection()
		}
	}

	private fun removeHighlight() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			pendingHighlightRange?.let { (verseNum, _, _) ->
				db.partialHighlightDao().deleteAllForVerse(
					primaryTranslation.code,
					currentBookId,
					currentChapter,
					verseNum
				)
			}
			pendingHighlightVerses?.let { verseNums ->
				for (verseNum in verseNums) {
					db.partialHighlightDao().deleteAllForVerse(
						primaryTranslation.code,
						currentBookId,
						currentChapter,
						verseNum
					)
				}
			}

			refreshHighlights()
			hideHighlightColorToolbar()
			clearSelection()
		}
	}

	private suspend fun refreshHighlights() {
		val db = BibleDatabase.getInstance(requireContext().applicationContext)
		val refreshed = db.partialHighlightDao()
			.getForChapter(primaryTranslation.code, currentBookId, currentChapter)
			.groupBy { it.verse }
		currentHighlights = refreshed
		if (::adapter.isInitialized) adapter.updateHighlights(refreshed)
	}

	private fun onMemoButtonClicked() {
		val verseNum = selectedVerses.firstOrNull() ?: return
		showVerseMemoEditDialog(verseNum, currentVerseMemos[verseNum])
		clearSelection()
	}

	private fun showVerseMemoEditDialog(verseNum: Int, existing: VerseMemo?) {
		verseMemoEditorLauncher.launch(
			VerseMemoEditorActivity.createIntent(
				context = requireContext(),
				bookId = currentBookId,
				chapter = currentChapter,
				verse = verseNum
			)
		)
	}

	private fun showVerseMemoDialog(verseNum: Int, memo: VerseMemo) {
		showVerseMemoEditDialog(verseNum, memo)
	}

	private fun showWordMemoEditDialog(
		verseNum: Int,
		start: Int,
		end: Int,
		existing: WordMemo?
	) {
		wordMemoEditorLauncher.launch(
			WordMemoEditorActivity.createIntent(
				context = requireContext(),
				translation = primaryTranslation.code,
				bookId = currentBookId,
				chapter = currentChapter,
				verse = verseNum,
				startOffset = start,
				endOffset = end
			)
		)
	}

	private fun showWordMemoViewDialog(verseNum: Int, memo: WordMemo) {
		showWordMemoEditDialog(verseNum, memo.startOffset, memo.endOffset, memo)
	}

	private suspend fun refreshMemos() {
		val db = BibleDatabase.getInstance(requireContext().applicationContext)
		currentVerseMemos =
			db.verseMemoDao().getForChapter(currentBookId, currentChapter).associateBy { it.verse }
		currentWordMemos = db.wordMemoDao()
			.getForChapter(primaryTranslation.code, currentBookId, currentChapter)
			.groupBy { it.verse }
		if (::adapter.isInitialized) adapter.updateMemos(currentVerseMemos, currentWordMemos)
	}
}