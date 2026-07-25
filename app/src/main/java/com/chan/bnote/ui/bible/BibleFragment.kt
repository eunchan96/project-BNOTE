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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleSeeder
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.SecondaryVerseText
import com.chan.bnote.data.bible.Translation
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.data.bible.scrap.Scrap
import com.chan.bnote.data.mypage.CopyFormatter
import com.chan.bnote.data.mypage.RecentChapterView
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

/** 부분 하이라이트 대상 (segment: 0=본문, 1=절이 소제목으로 쪼개진 경우의 뒷부분). */
private data class HighlightSelection(
	val verse: Int,
	val start: Int,
	val end: Int,
	val segment: Int
)

/** 한 페이지(장) 분량의 데이터 + 이미 다 만들어진 실제 인터랙티브 어댑터. */
class BiblePageData(
	val verses: List<BibleVerse>,
	val secondaryMap: Map<Int, SecondaryVerseText>?,
	val highlights: Map<Int, List<PartialHighlight>>,
	val verseMemos: Map<Int, VerseMemo>,
	val wordMemos: Map<Int, List<WordMemo>>,
	val isRead: Boolean,
	val hasSermon: Boolean,
	val adapter: VerseAdapter
)

class BibleFragment : Fragment(), TopBarActionHandler {

	companion object {
		private const val ARG_BOOK_ID = "bookId"
		private const val ARG_CHAPTER = "chapter"
		private const val ARG_VERSE = "verse"

		fun newInstance(bookId: Int, chapter: Int, verse: Int? = null): BibleFragment {
			val fragment = BibleFragment()
			fragment.arguments = android.os.Bundle().apply {
				putInt(ARG_BOOK_ID, bookId)
				putInt(ARG_CHAPTER, chapter)
				if (verse != null) putInt(ARG_VERSE, verse)
			}
			return fragment
		}
	}

	private lateinit var viewPager: ViewPager2
	private lateinit var pageAdapter: BiblePageAdapter

	// 지금 실제로 보이는 페이지(장)의 RecyclerView/VerseAdapter. ViewPager2가 페이지를 바꿀 때마다
	// onPageSelected에서 갱신된다 — 그 외 코드는 예전처럼 이 필드들만 보고 그대로 쓰면 된다.
	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: VerseAdapter

	private val pageFooters = mutableMapOf<Pair<Int, Int>, BibleReadingFooterAdapter>()
	private var pendingScrollBookId: Int? = null
	private var pendingScrollChapter: Int? = null
	private var pendingScrollVerse: Int? = null

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
	private var currentSecondaryMap: Map<Int, SecondaryVerseText>? = null
	private var currentHighlights: Map<Int, List<PartialHighlight>> = emptyMap()

	private lateinit var highlightColorToolbar: View
	private var pendingHighlightVerses: List<Int>? = null   // 절 탭 선택 → 전체 하이라이트용
	private var pendingHighlightRange: HighlightSelection? = null // 부분 하이라이트용

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
			val verse = data.getIntExtra(BookmarkListActivity.EXTRA_RESULT_VERSE, -1)
			if (bookId > 0 && chapter > 0) {
				loadChapter(bookId, chapter, scrollToVerse = if (verse > 0) verse else null)
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

		viewPager = view.findViewById(R.id.view_pager_bible)
		pageAdapter = BiblePageAdapter(this)
		viewPager.offscreenPageLimit = 1
		viewPager.isUserInputEnabled = AppSettings.isChapterSwipeEnabled(requireContext())
		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				onBiblePageSettled(position)
			}
		})

		selectionToolbar = view.findViewById(R.id.container_selection_toolbar)
		highlightColorToolbar = view.findViewById(R.id.scroll_highlight_toolbar)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			BibleSeeder.seedIfEmpty(requireContext().applicationContext, db)
			BibleChapterIndex.ensureLoaded(db, primaryTranslation.code)

			val startBookId = arguments?.getInt(ARG_BOOK_ID)
				?: AppSettings.getLastReadBookId(requireContext())
			val startChapter = arguments?.getInt(ARG_CHAPTER)
				?: AppSettings.getLastReadChapter(requireContext())
			val startVerse = arguments?.takeIf { it.containsKey(ARG_VERSE) }?.getInt(ARG_VERSE)

			currentBookId = startBookId
			currentChapter = startChapter
			pendingScrollBookId = startBookId
			pendingScrollChapter = startChapter
			pendingScrollVerse = startVerse

			viewPager.adapter = pageAdapter
			viewPager.setCurrentItem(BibleChapterIndex.positionOf(startBookId, startChapter), false)
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
		showReadingPlanCheck = isReadingPlanEnabled && !AppSettings.isReadingCheckBottomButtonMode(
			requireContext()
		),
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
		val sheet = TranslationPickerBottomSheet(primaryTranslation, secondaryTranslation)
		sheet.onTranslationsSelected = { primary, secondary ->
			primaryTranslation = primary
			secondaryTranslation = secondary
			AppSettings.setPrimaryTranslation(requireContext(), primary.code)
			AppSettings.setSecondaryTranslation(requireContext(), secondary?.code)

			// 번역본만 바뀌는 거라 지금 보고 있던 절 그대로 유지해야 하는데, 그냥 다시 그리면 맨 위(1절)로
			// 올라가 버린다. 그래서 지금 화면에 보이는 첫 절을 미리 기억해뒀다가 그 절로 다시 스크롤한다.
			if (::recyclerView.isInitialized) {
				val layoutManager =
					recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
				val firstVisiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: -1
				val visibleVerse = currentVerses.getOrNull(firstVisiblePosition)?.verse
				if (visibleVerse != null) {
					pendingScrollBookId = currentBookId
					pendingScrollChapter = currentChapter
					pendingScrollVerse = visibleVerse
				}
			}

			// 번역본이 바뀌면 모든 페이지의 본문이 다 바뀌어야 하니, 지금 페이지들을 전부 다시 그리게 한다.
			pageFooters.clear()
			pageAdapter.notifyDataSetChanged()
			onBiblePageSettled(viewPager.currentItem)
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

	/** ViewPager2가 새 페이지에 자리잡았을 때(스와이프든, setCurrentItem 호출이든) 불린다.
	 * 이 장을 "진짜 현재 장"으로 취급하도록 프래그먼트 레벨 상태를 전부 다시 맞춘다. */
	/** 페이지 하나의 데이터/어댑터가 다 준비됐을 때 BiblePageAdapter가 불러준다. onBiblePageSettled의
	 * 뷰홀더 조회가 타이밍상 아직 준비 안 된 페이지를 못 찾는 경우가 있어서, 그 보완으로 여기서도
	 * "지금 보이는 장이 맞으면" recyclerView/adapter를 채워준다. 이게 없으면 lateinit adapter가 아직
	 * 초기화되기 전에 절을 탭했을 때 앱이 튕길 수 있다. */
	fun onPageDataReady(
		bookId: Int,
		chapter: Int,
		pageRecyclerView: RecyclerView,
		pageAdapter: VerseAdapter
	) {
		if (bookId != currentBookId || chapter != currentChapter) return
		recyclerView = pageRecyclerView
		adapter = pageAdapter
	}

	private fun onBiblePageSettled(position: Int) {
		val (bookId, chapter) = BibleChapterIndex.chapterAt(position) ?: return
		val changed = bookId != currentBookId || chapter != currentChapter
		currentBookId = bookId
		currentChapter = chapter

		if (changed) {
			clearSelection()
			stopAutoScroll()
			isAutoScrolling = false
		}
		AppSettings.setLastRead(requireContext(), bookId, chapter)

		// ViewPager2 안쪽 RecyclerView에서 지금 페이지의 뷰홀더를 찾아 recyclerView/adapter를 갱신한다.
		val innerRecyclerView = viewPager.getChildAt(0) as? RecyclerView
		val holder =
			innerRecyclerView?.findViewHolderForAdapterPosition(position) as? BiblePageAdapter.PageViewHolder
		holder?.currentRecyclerView()?.let { rv ->
			recyclerView = rv
			(rv.adapter as? androidx.recyclerview.widget.ConcatAdapter)
				?.adapters
				?.filterIsInstance<VerseAdapter>()
				?.firstOrNull()
				?.let { adapter = it }
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			currentVerses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
			currentSecondaryMap = secondaryTranslation?.let { sec ->
				db.bibleDao().getVerses(sec.code, bookId, chapter)
					.associate { it.verse to SecondaryVerseText(it.text, it.text2) }
			}
			currentHighlights = db.partialHighlightDao()
				.getForChapter(primaryTranslation.code, bookId, chapter)
				.groupBy { it.verse }
			currentVerseMemos =
				db.verseMemoDao().getForChapter(bookId, chapter).associateBy { it.verse }
			currentWordMemos =
				db.wordMemoDao().getForChapter(primaryTranslation.code, bookId, chapter)
					.groupBy { it.verse }
			isChapterRead = db.readingProgressDao().get(bookId, chapter) != null
			hasSermonForChapter = db.sermonDao().getByBookChapter(bookId, chapter).isNotEmpty()
			db.recentChapterViewDao().upsert(RecentChapterView(bookId = bookId, chapter = chapter))

			notifyTopBarChanged()
			updateReadingCheckBottomButton()
		}
	}

	/** 한 페이지(장) 분량의 본문 · 하이라이트 · 북마크 · 메모를 전부 불러와서 실제 인터랙티브
	 * VerseAdapter까지 만들어서 반환한다. BiblePageAdapter가 페이지를 바인딩할 때 이걸 부른다.
	 * 콜백들은 currentBookId/currentChapter를 참조하는데, 이 값들은 onBiblePageSettled에서 항상
	 * "지금 보이는 페이지"에 맞게 갱신돼 있으므로 실제로 상호작용이 일어날 땐 항상 정확하다. */
	suspend fun loadPageData(bookId: Int, chapter: Int): BiblePageData {
		val db = BibleDatabase.getInstance(requireContext().applicationContext)
		val verses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
		val secondaryMap = secondaryTranslation?.let { sec ->
			db.bibleDao().getVerses(sec.code, bookId, chapter)
				.associate { it.verse to SecondaryVerseText(it.text, it.text2) }
		}
		val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
			.associateBy { it.verse }.toMutableMap()
		val isRead = db.readingProgressDao().get(bookId, chapter) != null
		val hasSermon = db.sermonDao().getByBookChapter(bookId, chapter).isNotEmpty()
		val highlights = db.partialHighlightDao()
			.getForChapter(primaryTranslation.code, bookId, chapter)
			.groupBy { it.verse }
		val verseMemos = db.verseMemoDao().getForChapter(bookId, chapter).associateBy { it.verse }
		val wordMemos = db.wordMemoDao().getForChapter(primaryTranslation.code, bookId, chapter)
			.groupBy { it.verse }

		val initialSelection = if (bookId == currentBookId && chapter == currentChapter) {
			selectedVerses.toSet()
		} else {
			emptySet()
		}

		val pageAdapterInstance = VerseAdapter(
			verses = verses,
			secondaryTextByVerse = secondaryMap,
			bookmarks = bookmarkMap,
			fontSize = currentFontSize,
			selectedVerses = initialSelection,
			highlightsByVerse = highlights,
			verseMemos = verseMemos,
			wordMemosByVerse = wordMemos,
			onVerseTap = { verseNum -> toggleVerseSelection(verseNum) },
			onVerseMemoView = { verseNum, memo -> showVerseMemoDialog(verseNum, memo) },
			onHighlightRequested = { verseNum, start, end, segment ->
				pendingHighlightRange = HighlightSelection(verseNum, start, end, segment)
				pendingHighlightVerses = null
				showHighlightColorToolbar()
			},
			onWordMemoCreate = { verseNum, start, end, segment ->
				showWordMemoEditDialog(verseNum, start, end, segment, null)
			},
			onWordMemoView = { verseNum, memo -> showWordMemoViewDialog(verseNum, memo) }
		)

		return BiblePageData(
			verses,
			secondaryMap,
			highlights,
			verseMemos,
			wordMemos,
			isRead,
			hasSermon,
			pageAdapterInstance
		)
	}

	/** 페이지마다 자기만의 "읽음 표시" 하단 버튼(footer)을 갖는다. */
	fun createFooterAdapterFor(
		bookId: Int,
		chapter: Int,
		isRead: Boolean
	): BibleReadingFooterAdapter {
		val bottomSpace = (resources.displayMetrics.heightPixels * 0.3f).toInt()
		val footer =
			BibleReadingFooterAdapter(bottomSpace) { onReadingCheckToggledForPage(bookId, chapter) }
		val shouldShow =
			isReadingPlanEnabled && AppSettings.isReadingCheckBottomButtonMode(requireContext())
		footer.update(shouldShow, isRead)
		return footer
	}

	fun registerPageFooter(bookId: Int, chapter: Int, footer: BibleReadingFooterAdapter) {
		pageFooters[bookId to chapter] = footer
	}

	/** loadChapter(bookId, chapter, scrollToVerse)로 요청해둔 "그 장에 도착하면 이 절로 스크롤"
	 * 요청을 페이지가 로드될 때 한 번만 꺼내 쓴다. */
	fun consumePendingScrollVerse(bookId: Int, chapter: Int): Int? {
		if (pendingScrollBookId == bookId && pendingScrollChapter == chapter) {
			val verse = pendingScrollVerse
			pendingScrollBookId = null
			pendingScrollChapter = null
			pendingScrollVerse = null
			return verse
		}
		return null
	}

	/** 상단 아이콘이든 하단 버튼이든, 읽음 표시를 누르면 결국 이걸 탄다. */
	private fun onReadingCheckToggledForPage(bookId: Int, chapter: Int) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val wasRead = db.readingProgressDao().get(bookId, chapter) != null
			if (wasRead) {
				db.readingProgressDao().delete(bookId, chapter)
			} else {
				db.readingProgressDao().upsert(ReadingProgress(bookId = bookId, chapter = chapter))
			}
			val nowRead = !wasRead

			val shouldShow =
				isReadingPlanEnabled && AppSettings.isReadingCheckBottomButtonMode(requireContext())
			pageFooters[bookId to chapter]?.update(shouldShow, nowRead)

			if (bookId == currentBookId && chapter == currentChapter) {
				isChapterRead = nowRead
				notifyTopBarChanged()
			}
		}
	}

	override fun onPrevChapterClicked() {
		val position = viewPager.currentItem
		if (position > 0) {
			viewPager.currentItem = position - 1
		} else {
			Toast.makeText(requireContext(), "첫 장입니다", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onNextChapterClicked() {
		val position = viewPager.currentItem
		if (position < pageAdapter.itemCount - 1) {
			viewPager.currentItem = position + 1
		} else {
			Toast.makeText(requireContext(), "마지막 장입니다", Toast.LENGTH_SHORT).show()
		}
	}

	/** 탭이 유지되는 프래그먼트라 onViewCreated는 다시 안 불리므로, 설정 화면에서 바뀐 값(글자 크기 등)을
	 * 여기서 다시 확인해서 반영한다. hide()/show() 방식의 탭 전환은 onResume이 아니라 이 콜백을 탄다. */
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) refreshOnReturnToTab()
	}

	override fun onResume() {
		super.onResume()
		refreshOnReturnToTab()
	}

	/** 탭 전환이나 설정 화면 등 다른 곳에서 돌아왔을 때, 그 사이에 바뀌었을 수 있는 것들을 다시 확인한다:
	 * 글자 크기, 성경읽기표 표시 방식/상태, 그리고 그 사이에 이 장에 설교노트가 추가/삭제됐을 수도 있으니
	 * 설교 아이콘 표시 여부까지. */
	private fun refreshOnReturnToTab() {
		if (!::adapter.isInitialized) return
		val newFontSize = AppSettings.getFontSize(requireContext())
		if (newFontSize != currentFontSize) {
			currentFontSize = newFontSize
			adapter.updateFontSize(currentFontSize)
		}
		scrollSpeed = AppSettings.getScrollSpeed(requireContext())
		updateReadingCheckBottomButton()
		if (::viewPager.isInitialized) {
			viewPager.isUserInputEnabled = AppSettings.isChapterSwipeEnabled(requireContext())
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			hasSermonForChapter =
				db.sermonDao().getByBookChapter(currentBookId, currentChapter).isNotEmpty()
			notifyTopBarChanged()
		}
	}

	override fun onSermonIconClicked() {
		ChapterSermonsActivity.start(requireContext(), currentBookId, currentChapter)
	}

	/** 탭을 새로 만들지 않고 기존 인스턴스에서 특정 장으로 이동할 때 (MainActivity에서 호출). */
	fun navigateTo(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		loadChapter(bookId, chapter, scrollToVerse)
	}

	/** 지정한 (책, 장)으로 이동한다. 이미 그 장을 보고 있으면 그냥 그 자리에서 절로만 스크롤하고,
	 * 다른 장이면 ViewPager2로 그 페이지까지 이동한다(애니메이션 없이 바로 점프). */
	private fun loadChapter(bookId: Int, chapter: Int, scrollToVerse: Int? = null) {
		if (!::viewPager.isInitialized) return

		if (!BibleChapterIndex.isReady) {
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(requireContext().applicationContext)
				BibleChapterIndex.ensureLoaded(db, primaryTranslation.code)
				loadChapter(bookId, chapter, scrollToVerse)
			}
			return
		}

		if (bookId == currentBookId && chapter == currentChapter) {
			scrollToVerse?.let { verseNum ->
				val index = currentVerses.indexOfFirst { it.verse == verseNum }
				if (index >= 0 && ::recyclerView.isInitialized) recyclerView.scrollToPosition(index)
			}
			return
		}

		pendingScrollBookId = bookId
		pendingScrollChapter = chapter
		pendingScrollVerse = scrollToVerse
		viewPager.setCurrentItem(BibleChapterIndex.positionOf(bookId, chapter), false)
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
			updateReadingCheckBottomButton()
			notifyTopBarChanged()
		}
	}

	/** 설정에서 "하단 버튼으로 표시"를 켰을 때만 스크롤 맨 끝 여백에 이 버튼을 보여주고, 읽음 여부에
	 * 따라 문구를 바꾼다. 다시 누르면 읽음 표시를 취소할 수 있다(onReadingCheckToggledForPage가
	 * 토글이라 그대로 재사용). */
	private fun updateReadingCheckBottomButton() {
		val shouldShow = isReadingPlanEnabled &&
				AppSettings.isReadingCheckBottomButtonMode(requireContext())
		pageFooters[currentBookId to currentChapter]?.update(shouldShow, isChapterRead)
	}

	private fun toggleVerseSelection(verseNum: Int) {
		if (selectedVerses.contains(verseNum)) {
			selectedVerses.remove(verseNum)
		} else {
			selectedVerses.add(verseNum)
		}
		if (::adapter.isInitialized) adapter.updateSelection(selectedVerses.toSet())
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
		val activeConfig = AppSettings.getActiveCopyFormat(requireContext())

		val text = CopyFormatter.format(
			bookId = currentBookId,
			chapter = currentChapter,
			verses = currentVerses,
			selectedVerseNumbers = selectedVerses,
			secondaryMap = if (includeSecondary) currentSecondaryMap else null,
			includeSecondary = includeSecondary,
			config = activeConfig
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
		pendingHighlightRange?.let { (verseNum, start, end, segment) ->
			val overlaps = currentHighlights[verseNum]?.any { h ->
				h.segment == segment && !(end <= h.startOffset || start >= h.endOffset)
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

			pendingHighlightRange?.let { (verseNum, start, end, segment) ->
				db.partialHighlightDao().insert(
					PartialHighlight(
						translation = primaryTranslation.code,
						bookId = currentBookId,
						chapter = currentChapter,
						verse = verseNum,
						startOffset = start,
						endOffset = end,
						segment = segment,
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
							segment = 0,
							colorHex = colorHex
						)
					)
					// 절이 소제목으로 둘로 나뉘는 극소수 예외 구절(예: 창 35:22)은 뒷부분(text2)도
					// 마저 하이라이트해야 "전체 하이라이트"가 진짜 전체를 덮는다.
					val text2 = verseData.text2
					if (!text2.isNullOrBlank()) {
						db.partialHighlightDao().insert(
							PartialHighlight(
								translation = primaryTranslation.code,
								bookId = currentBookId,
								chapter = currentChapter,
								verse = verseNum,
								startOffset = 0,
								endOffset = text2.length,
								segment = 1,
								colorHex = colorHex
							)
						)
					}
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
		segment: Int,
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
				endOffset = end,
				segment = segment
			)
		)
	}

	private fun showWordMemoViewDialog(verseNum: Int, memo: WordMemo) {
		showWordMemoEditDialog(verseNum, memo.startOffset, memo.endOffset, memo.segment, memo)
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