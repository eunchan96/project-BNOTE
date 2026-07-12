package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.ChapterMarker
import com.chan.bnote.data.Sermon
import kotlinx.coroutines.launch

class SermonByBookFragment : Fragment() {

	private lateinit var bookTitleText: TextView
	private lateinit var chapterGridRecycler: RecyclerView

	private var currentBookId = 1
	private var selectedChapter = 1

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_by_book, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bookTitleText = view.findViewById(R.id.text_current_book)
		chapterGridRecycler = view.findViewById(R.id.recycler_chapter_grid)
		chapterGridRecycler.layoutManager = GridLayoutManager(requireContext(), 5)

		bookTitleText.setOnClickListener {
			val picker = BookOnlyPickerBottomSheet()
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

		view.findViewById<TextView>(R.id.fab_add_sermon_by_book).setOnClickListener {
			val sheet = AddSermonBottomSheet()
			sheet.onSaved = { loadChapterGrid(); loadSermonsForSelectedChapter() }
			sheet.show(parentFragmentManager, "add_sermon")
		}

		loadChapterGrid()
	}

	private fun loadChapterGrid() {
		bookTitleText.text = BibleBooks.nameOf(currentBookId)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter("GAEYEOK", currentBookId)
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
		val result = mutableMapOf<Int, MutableList<String>>()
		for (marker in markers) {
			val color = marker.colorHex ?: continue
			for (chapter in marker.startChapter..marker.endChapter) {
				result.getOrPut(chapter) { mutableListOf() }.add(color)
			}
		}
		return result
	}

	private fun loadSermonsForSelectedChapter() {
		val label = view?.findViewById<TextView>(R.id.text_selected_chapter_label)
		label?.text = "${BibleBooks.nameOf(currentBookId)} ${selectedChapter}장"

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByBookChapter(currentBookId, selectedChapter)
			renderList(sermons)
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

		val labels = sermons.map { "${it.title}  (${it.preacher})" }
		recyclerView.adapter = SimpleListAdapter(labels) { position ->
			val detail = SermonDetailBottomSheet(sermons[position])
			detail.onChanged = { loadChapterGrid() }
			detail.show(parentFragmentManager, "sermon_detail")
		}
	}
}