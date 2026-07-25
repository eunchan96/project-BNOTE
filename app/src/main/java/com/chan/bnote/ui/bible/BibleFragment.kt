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

/** 부분 하이라이트 대상 (segment: 0=본문, 1=절이 소제목으로 쪼개진 경우의 뒷부분). */
private data class HighlightSelection(
	val verse: Int,
	val start: Int,
	val end: Int,
	val segment: Int
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
	private lateinit var readingFooterAdapter: BibleReadingFooterAdapter
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
	private var currentSecondaryMap: Map<Int, com.chan.bnote.data.bible.SecondaryVerseText>? = null
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
		// 기본 아이템 애니메이터가 어댑터 내용이 바뀔 때마다 오래된 내용과 새 내용을 겹쳐서
		// 크로스페이드시키는데, 이게 스와이프 전환 중 "글자가 겹쳐 보이는" 원인이었다. 우리가
		// translationX로 직접 밀고 당기는 걸로 전환을 처리하니, 이 자동 애니메이션은 꺼둔다.
		recyclerView.itemAnimator = null
		previewNextRecyclerView = view.findViewById(R.id.recycler_verses_preview_next)
		previewNextRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		previewNextRecyclerView.isNestedScrollingEnabled = false
		previewNextRecyclerView.itemAnimator = null
		previewPrevRecyclerView = view.findViewById(R.id.recycler_verses_preview_prev)
		previewPrevRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		previewPrevRecyclerView.isNestedScrollingEnabled = false
		previewPrevRecyclerView.itemAnimator = null
		attachChapterSwipeGesture()

		selectionToolbar = view.findViewById(R.id.container_selection_toolbar)
		highlightColorToolbar = view.findViewById(R.id.scroll_highlight_toolbar)

		val bottomSpace = (resources.displayMetrics.heightPixels * 0.3f).toInt()
		readingFooterAdapter =
			BibleReadingFooterAdapter(bottomSpace) { onReadingPlanCheckClicked() }

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			BibleSeeder.seedIfEmpty(requireContext().applicationContext, db)

			val startBookId = arguments?.getInt(ARG_BOOK_ID)
				?: AppSettings.getLastReadBookId(requireContext())
			val startChapter = arguments?.getInt(ARG_CHAPTER)
				?: AppSettings.getLastReadChapter(requireContext())
			val startVerse = arguments?.takeIf { it.containsKey(ARG_VERSE) }?.getInt(ARG_VERSE)
			loadChapter(startBookId, startChapter, scrollToVerse = startVerse)
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

	private var swipeStartX = 0f
	private var swipeStartY = 0f
	private var isDraggingChapter = false
	private var pendingNextChapter: PrewarmedChapter? = null
	private var pendingPrevChapter: PrewarmedChapter? = null
	private lateinit var previewNextRecyclerView: RecyclerView
	private lateinit var previewPrevRecyclerView: RecyclerView

	/** 미리 통째로 로드해둔 장 하나(진짜 본문 + 하이라이트 + 메모 + 실제 인터랙티브 어댑터까지 전부).
	 * 스와이프가 완료되면 이걸 그대로 "현재 장"으로 승격시키므로, 미리보기와 실제 화면이 완전히 같다. */
	private inner class PrewarmedChapter(
		val bookId: Int,
		val chapter: Int,
		val verses: List<BibleVerse>,
		val secondaryMap: Map<Int, com.chan.bnote.data.bible.SecondaryVerseText>?,
		val bookmarkMap: MutableMap<Int, BibleBookmark>,
		val highlights: Map<Int, List<PartialHighlight>>,
		val verseMemos: Map<Int, VerseMemo>,
		val wordMemos: Map<Int, List<WordMemo>>,
		val isRead: Boolean,
		val hasSermon: Boolean,
		val adapter: VerseAdapter
	)

	/** 장이 다 로드된 직후(드래그 시작 시점이 아니라) 다음/이전 장을 통째로(본문·하이라이트·메모·실제
	 * 어댑터까지) 미리 만들어서 미리보기 RecyclerView에 얹어둔다. 실제 화면과 완전히 똑같은 걸 미리
	 * 만들어두는 거라, 스와이프가 끝나면 다시 불러올 필요 없이 그대로 "진짜 현재 장"으로 바꿔치기한다. */
	private fun prewarmAdjacentChapters() {
		pendingNextChapter = null
		pendingPrevChapter = null
		previewNextRecyclerView.visibility = View.INVISIBLE
		previewPrevRecyclerView.visibility = View.INVISIBLE

		val requestBookId = currentBookId
		val requestChapter = currentChapter

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter(primaryTranslation.code, requestBookId)

			val nextTarget = when {
				requestChapter < maxChapter -> requestBookId to (requestChapter + 1)
				requestBookId < 66 -> (requestBookId + 1) to 1
				else -> null
			}
			val prevTarget = when {
				requestChapter > 1 -> requestBookId to (requestChapter - 1)
				requestBookId > 1 -> {
					val prevBook = requestBookId - 1
					prevBook to db.bibleDao().getMaxChapter(primaryTranslation.code, prevBook)
				}

				else -> null
			}

			val next = nextTarget?.let { (b, c) -> buildPrewarmedChapter(db, b, c) }
			val prev = prevTarget?.let { (b, c) -> buildPrewarmedChapter(db, b, c) }

			// 그 사이에 사용자가 다른 장으로 넘어갔으면 이 결과는 버린다.
			if (requestBookId != currentBookId || requestChapter != currentChapter) return@launch

			pendingNextChapter = next
			pendingPrevChapter = prev

			val width = recyclerView.width.toFloat().takeIf { it > 0f } ?: 1080f
			if (next != null) {
				previewNextRecyclerView.adapter =
					androidx.recyclerview.widget.ConcatAdapter(next.adapter, previewFooterAdapter())
				previewNextRecyclerView.translationX = width
			}
			if (prev != null) {
				previewPrevRecyclerView.adapter =
					androidx.recyclerview.widget.ConcatAdapter(prev.adapter, previewFooterAdapter())
				previewPrevRecyclerView.translationX = -width
			}
		}
	}

	/** 미리보기용 footer는 그냥 빈 여백만 있으면 된다(읽음 버튼은 실제로 그 장이 될 때만 의미 있음). */
	private fun previewFooterAdapter(): BibleReadingFooterAdapter {
		val bottomSpace = (resources.displayMetrics.heightPixels * 0.3f).toInt()
		return BibleReadingFooterAdapter(bottomSpace) {}
	}

	private suspend fun buildPrewarmedChapter(
		db: BibleDatabase,
		bookId: Int,
		chapter: Int
	): PrewarmedChapter {
		val verses = db.bibleDao().getVerses(primaryTranslation.code, bookId, chapter)
		val secondaryMap = secondaryTranslation?.let { sec ->
			db.bibleDao().getVerses(sec.code, bookId, chapter)
				.associate {
					it.verse to com.chan.bnote.data.bible.SecondaryVerseText(
						it.text,
						it.text2
					)
				}
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

		val chapterAdapter = VerseAdapter(
			verses = verses,
			secondaryTextByVerse = secondaryMap,
			bookmarks = bookmarkMap,
			fontSize = currentFontSize,
			selectedVerses = emptySet(),
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

		return PrewarmedChapter(
			bookId, chapter, verses, secondaryMap, bookmarkMap, highlights,
			verseMemos, wordMemos, isRead, hasSermon, chapterAdapter
		)
	}

	/** 미리 만들어둔 장을 그대로 "진짜 현재 장"으로 바꿔치기한다 — 다시 불러오는 과정이 없어서
	 * 미리보기와 실제 화면이 완전히 똑같고, 전환도 순간적이다. */
	private fun promoteChapter(prewarmed: PrewarmedChapter) {
		currentBookId = prewarmed.bookId
		currentChapter = prewarmed.chapter
		currentVerses = prewarmed.verses
		currentSecondaryMap = prewarmed.secondaryMap
		currentHighlights = prewarmed.highlights
		currentVerseMemos = prewarmed.verseMemos
		currentWordMemos = prewarmed.wordMemos
		isChapterRead = prewarmed.isRead
		hasSermonForChapter = prewarmed.hasSermon
		clearSelection()

		adapter = prewarmed.adapter
		recyclerView.adapter =
			androidx.recyclerview.widget.ConcatAdapter(adapter, readingFooterAdapter)
		recyclerView.scrollToPosition(0)

		AppSettings.setLastRead(requireContext(), currentBookId, currentChapter)
		updateReadingCheckBottomButton()
		notifyTopBarChanged()

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			db.recentChapterViewDao().upsert(
				com.chan.bnote.data.mypage.RecentChapterView(
					bookId = currentBookId,
					chapter = currentChapter
				)
			)
		}

		if (AppSettings.isChapterSwipeEnabled(requireContext())) {
			recyclerView.post { prewarmAdjacentChapters() }
		}
	}

	/** 손가락을 따라 본문이 실시간으로 밀려나고, 그 자리로 다음/이전 장이 옆에서 따라 들어온다. */
	private fun updateChapterDrag(deltaX: Float) {
		val width = recyclerView.width.toFloat()
		if (width <= 0f) return

		recyclerView.translationX = deltaX

		val isNext = deltaX < 0
		val activePreview = if (isNext) previewNextRecyclerView else previewPrevRecyclerView
		val otherPreview = if (isNext) previewPrevRecyclerView else previewNextRecyclerView
		val hasPreview = if (isNext) pendingNextChapter != null else pendingPrevChapter != null

		otherPreview.visibility = View.INVISIBLE
		if (!hasPreview) {
			activePreview.visibility = View.INVISIBLE
			return
		}
		if (activePreview.visibility != View.VISIBLE) {
			// 처음 보이는 순간에만 하드웨어 레이어를 켠다(소프트웨어로 매 프레임 다시 그리면 버벅여서
			// "지지직"거리는 느낌이 난다 — GPU 레이어로 이동/합성만 하도록 바꿔서 부드럽게 만든다).
			recyclerView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
			activePreview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
		}
		activePreview.visibility = View.VISIBLE
		activePreview.translationX = deltaX + (if (isNext) width else -width)
	}

	/** 손을 뗐을 때: 충분히 많이 끌었으면 미리 만들어둔 장을 그대로 승격시키고, 아니면 제자리로 돌아온다. */
	private fun finishChapterDrag(deltaX: Float) {
		val width = recyclerView.width.toFloat()
		if (width <= 0f) {
			resetChapterDrag()
			return
		}

		val isNext = deltaX < 0
		val activePreview = if (isNext) previewNextRecyclerView else previewPrevRecyclerView
		val target = if (isNext) pendingNextChapter else pendingPrevChapter
		val threshold = width * 0.28f

		if (kotlin.math.abs(deltaX) > threshold && target != null) {
			recyclerView.animate()
				.translationX(if (isNext) -width else width)
				.setDuration(200)
				.withEndAction {
					promoteChapter(target)
					resetChapterDrag()
				}
				.start()
			activePreview.animate().translationX(0f).setDuration(200).start()
		} else {
			recyclerView.animate().translationX(0f).setDuration(200)
				.withEndAction { resetChapterDrag() }
				.start()
			activePreview.animate()
				.translationX(if (isNext) width else -width)
				.setDuration(200)
				.withEndAction { activePreview.visibility = View.INVISIBLE }
				.start()
		}
	}

	private fun resetChapterDrag() {
		recyclerView.translationX = 0f
		previewNextRecyclerView.visibility = View.INVISIBLE
		previewPrevRecyclerView.visibility = View.INVISIBLE
		// 하드웨어 레이어는 애니메이션 끝난 뒤엔 꺼둔다(계속 켜두면 오히려 메모리를 더 쓴다).
		recyclerView.setLayerType(View.LAYER_TYPE_NONE, null)
		previewNextRecyclerView.setLayerType(View.LAYER_TYPE_NONE, null)
		previewPrevRecyclerView.setLayerType(View.LAYER_TYPE_NONE, null)
	}

	/** 설정에서 "스와이프로 장 이동"을 켰을 때, 좌우로 드래그하면 손가락을 따라 실시간으로 넘어간다
	 * (인스타 스토리처럼 다음/이전 장이 옆에서 미리 보이면서 따라 들어옴). 절 선택이나 텍스트 드래그
	 * 선택 같은 평소 동작을 방해하지 않도록, 뚜렷한 가로 드래그로 판단될 때만 가로챈다. */
	private fun attachChapterSwipeGesture() {
		val touchSlop = android.view.ViewConfiguration.get(requireContext()).scaledTouchSlop

		recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
			override fun onInterceptTouchEvent(
				rv: RecyclerView,
				e: android.view.MotionEvent
			): Boolean {
				if (!AppSettings.isChapterSwipeEnabled(requireContext())) return false

				when (e.actionMasked) {
					android.view.MotionEvent.ACTION_DOWN -> {
						swipeStartX = e.x
						swipeStartY = e.y
						isDraggingChapter = false
					}

					android.view.MotionEvent.ACTION_MOVE -> {
						val deltaX = e.x - swipeStartX
						val deltaY = e.y - swipeStartY
						if (!isDraggingChapter &&
							kotlin.math.abs(deltaX) > touchSlop * 2 &&
							kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 2
						) {
							isDraggingChapter = true
						}
						if (isDraggingChapter) return true
					}
				}
				return false
			}

			override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {
				if (!isDraggingChapter) return
				when (e.actionMasked) {
					android.view.MotionEvent.ACTION_MOVE -> updateChapterDrag(e.x - swipeStartX)
					android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
						finishChapterDrag(e.x - swipeStartX)
						isDraggingChapter = false
					}
				}
			}

			override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
		})
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

		if (AppSettings.isChapterSwipeEnabled(requireContext()) && pendingNextChapter == null && pendingPrevChapter == null) {
			prewarmAdjacentChapters()
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
				db.bibleDao().getVerses(sec.code, bookId, chapter)
					.associate {
						it.verse to com.chan.bnote.data.bible.SecondaryVerseText(
							it.text,
							it.text2
						)
					}
			}
			currentVerses = verses
			currentSecondaryMap = secondaryMap

			val bookmarkMap = db.bookmarkDao().getBookmarksForChapter(bookId, chapter)
				.associateBy { it.verse }.toMutableMap()
			isChapterRead = db.readingProgressDao().get(bookId, chapter) != null
			updateReadingCheckBottomButton()
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
				onHighlightRequested = { verseNum, start, end, segment ->
					pendingHighlightRange = HighlightSelection(verseNum, start, end, segment)
					pendingHighlightVerses = null
					showHighlightColorToolbar()
				},
				onWordMemoCreate = { verseNum, start, end, segment ->
					showWordMemoEditDialog(
						verseNum,
						start,
						end,
						segment,
						null
					)
				},
				onWordMemoView = { verseNum, memo -> showWordMemoViewDialog(verseNum, memo) }
			)
			recyclerView.adapter =
				androidx.recyclerview.widget.ConcatAdapter(adapter, readingFooterAdapter)
			if (AppSettings.isChapterSwipeEnabled(requireContext())) {
				recyclerView.post { prewarmAdjacentChapters() }
			}

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
			updateReadingCheckBottomButton()
			notifyTopBarChanged()
		}
	}

	/** 설정에서 "하단 버튼으로 표시"를 켰을 때만 스크롤 맨 끝 여백에 이 버튼을 보여주고, 읽음 여부에
	 * 따라 문구를 바꾼다. 다시 누르면 읽음 표시를 취소할 수 있다(onReadingPlanCheckClicked가 토글이라
	 * 그대로 재사용). */
	private fun updateReadingCheckBottomButton() {
		if (!::readingFooterAdapter.isInitialized) return
		val shouldShow = isReadingPlanEnabled &&
				AppSettings.isReadingCheckBottomButtonMode(requireContext())
		readingFooterAdapter.update(shouldShow, isChapterRead)
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