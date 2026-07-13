package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.DraggableBottomSheet
import com.chan.bnote.ui.sermon.SermonDetailBottomSheet
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowData
import kotlinx.coroutines.launch

class ChapterSermonsBottomSheet(
	private val bookId: Int,
	private val chapter: Int
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.5f

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_chapter_sermons, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_chapter_sermons_title).text =
			"${BibleBooks.nameOf(bookId)} ${chapter}장의 설교"

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_chapter_sermons)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByBookChapter(bookId, chapter)

			val rows = sermons.map { sermon ->
				val firstRef = db.sermonBibleRefDao().getFirstRef(sermon.id)
				val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
				SermonRowData(
					sermon = sermon,
					colorHex = category?.colorHex,
					dateLabel = DateUtils.formatDateShort(sermon.sermonDate),
					bibleRefLabel = firstRef?.toShortLabel() ?: ""
				)
			}

			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				SermonDetailBottomSheet(sermon).show(parentFragmentManager, "sermon_detail")
			}
		}
	}
}