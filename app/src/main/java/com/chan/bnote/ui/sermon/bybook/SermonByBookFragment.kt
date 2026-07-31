package com.chan.bnote.ui.sermon.bybook

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.sermon.ChapterMarker
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.FabAddHandler
import com.chan.bnote.ui.bible.picker.BookOnlyPickerBottomSheet
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowBuilder
import com.chan.bnote.ui.sermon.SermonSortableFragment
import com.chan.bnote.ui.sermon.SortButtonHelper
import com.chan.bnote.ui.sermon.addsermon.AddSermonActivity
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.launch

class SermonByBookFragment : Fragment(), SermonSortableFragment, FabAddHandler {

	private lateinit var bookTitleText: TextView
	private lateinit var chapterGridRecycler: RecyclerView

	private val addSermonLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadChapterGrid()
			loadSermonsForSelectedChapter()
		}
	}

	private val sermonDetailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadChapterGrid()
			loadSermonsForSelectedChapter()
		}
	}

	private var currentBookId = 1
	private var selectedChapter = 1
	private var sortMode = "ADDED"

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_by_book, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bookTitleText = view.findViewById(R.id.text_current_book)
		chapterGridRecycler = view.findViewById(R.id.recycler_chapter_grid)
		chapterGridRecycler.layoutManager = GridLayoutManager(requireContext(), 7)

		sortMode = AppSettings.getByBookSortMode(requireContext())

		bookTitleText.setOnClickListener {
			val picker = BookOnlyPickerBottomSheet()
			picker.selectedBookId = currentBookId
			picker.onBookSelected = { bookId ->
				currentBookId = bookId
				selectedChapter = 1
				loadChapterGrid()
			}
			picker.show(parentFragmentManager, "book_only_picker")
		}

		view.findViewById<TextView>(R.id.btn_book_prev).setOnClickListener {
			if (currentBookId > 1) {
				currentBookId -= 1
				selectedChapter = 1
				loadChapterGrid()
			}
		}
		view.findViewById<TextView>(R.id.btn_book_next).setOnClickListener {
			if (currentBookId < 66) {
				currentBookId += 1
				selectedChapter = 1
				loadChapterGrid()
			}
		}

		SortButtonHelper.setup(view.findViewById(R.id.btn_by_book_sort), this)

		loadChapterGrid()
	}

	override fun onFabAddClicked() {
		addSermonLauncher.launch(AddSermonActivity.createIntent(requireContext()))
	}

	private fun loadChapterGrid() {
		bookTitleText.text = BibleBooks.nameOf(currentBookId)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter("NKRV", currentBookId)
			val markers = db.sermonDao().getChapterMarkersForBook(currentBookId)

			val colorsByChapter = buildColorsByChapter(markers)

			val cells = (1..maxChapter).map { chapter ->
				ChapterCell(chapter, colorsByChapter[chapter].orEmpty())
			}
			chapterGridRecycler.adapter = ChapterGridAdapter(cells, selectedChapter) { cell ->
				selectedChapter = cell.chapter
				loadChapterGrid() // 선택 표시 갱신을 위해 그리드 다시 그림
			}

			loadSermonsForSelectedChapter()
		}
	}

	// 구간(예: 1~3장)에 걸친 마커를 장 단위로 펼쳐서, 걸쳐있는 모든 장에 같은 색이 표시되게 함
	private fun buildColorsByChapter(markers: List<ChapterMarker>): Map<Int, List<String>> {
		val fallbackColorHex = String.format(
			"#%06X", 0xFFFFFF and androidx.core.content.ContextCompat.getColor(
				requireContext(), R.color.category_none
			)
		)
		val result = mutableMapOf<Int, MutableList<String>>()
		for (marker in markers) {
			val color = marker.colorHex ?: fallbackColorHex
			for (chapter in marker.startChapter..marker.endChapter) {
				result.getOrPut(chapter) { mutableListOf() }.add(color)
			}
		}
		return result
	}

	override fun getSortOptions() = listOf(
		"BIBLE" to "성경순(시작절)",
		"DATE" to "날짜순",
		"CATEGORY" to "카테고리순",
		"ADDED" to "추가순"
	)

	override fun getCurrentSortMode() = sortMode

	override fun setSortMode(mode: String) {
		sortMode = mode
		AppSettings.setByBookSortMode(requireContext(), mode)
		loadSermonsForSelectedChapter()
	}

	private fun loadSermonsForSelectedChapter() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByBookChapter(currentBookId, selectedChapter)
			val sorted = when (sortMode) {
				"BIBLE" -> {
					val firstRefs =
						com.chan.bnote.ui.sermon.SermonSortUtils.loadFirstRefs(db, sermons)
					sermons.sortedWith(
						com.chan.bnote.ui.sermon.SermonSortUtils.byBibleOrder(
							firstRefs
						)
					)
				}

				"DATE" -> sermons.sortedWith(com.chan.bnote.ui.sermon.SermonSortUtils.byDateDesc())
				"CATEGORY" -> {
					val categoryOrder =
						com.chan.bnote.ui.sermon.SermonSortUtils.loadCategoryOrderMap(db)
					sermons.sortedWith(
						com.chan.bnote.ui.sermon.SermonSortUtils.byCategoryOrder(
							categoryOrder
						)
					)
				}

				else -> sermons.sortedWith(com.chan.bnote.ui.sermon.SermonSortUtils.byAddedOrder())
			}
			renderList(sorted)
		}
	}

	private fun renderList(sermons: List<Sermon>) {
		val recyclerView =
			view?.findViewById<RecyclerView>(R.id.recycler_sermons_by_chapter) ?: return
		val emptyText = view?.findViewById<TextView>(R.id.text_empty_by_chapter) ?: return

		if (sermons.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val rows = SermonRowBuilder.build(db, sermons)
			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				sermonDetailLauncher.launch(
					SermonDetailActivity.createIntent(requireContext(), sermon.id)
				)
			}
		}
	}
}