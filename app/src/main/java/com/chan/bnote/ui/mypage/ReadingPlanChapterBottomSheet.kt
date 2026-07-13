package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.BibleNavigationHost
import com.chan.bnote.ui.DraggableBottomSheet
import com.chan.bnote.ui.sermon.bybook.ChapterCell
import com.chan.bnote.ui.sermon.bybook.ChapterGridAdapter
import kotlinx.coroutines.launch

class ReadingPlanChapterBottomSheet(
	private val bookId: Int
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.7f

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_reading_plan_chapters, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_reading_plan_chapter_title).text =
			"${BibleBooks.nameOf(bookId)} 읽음 현황"

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_reading_plan_chapters)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val maxChapter = db.bibleDao().getMaxChapter("NKRV", bookId)
			val readChapters = db.readingProgressDao().getAll()
				.filter { it.bookId == bookId }
				.map { it.chapter }
				.toSet()

			val cells = (1..maxChapter).map { chapter ->
				ChapterCell(
					chapter,
					if (chapter in readChapters) listOf("#795548") else emptyList()
				)
			}

			recyclerView.adapter = ChapterGridAdapter(cells, selectedChapter = -1) { cell ->
				(activity as? BibleNavigationHost)?.navigateToBibleChapter(bookId, cell.chapter)
				dismissAllAndNavigate()
			}
		}
	}

	// 마이페이지 -> 성경읽기표 -> 책별 화면, 이렇게 시트가 2겹 열려있을 수 있어서 둘 다 닫아줌
	private fun dismissAllAndNavigate() {
		dismiss()
		(parentFragmentManager.findFragmentByTag("reading_plan") as? DraggableBottomSheet)?.dismiss()
	}
}