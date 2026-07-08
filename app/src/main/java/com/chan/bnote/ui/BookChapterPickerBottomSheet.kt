package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class BookChapterPickerBottomSheet : BottomSheetDialogFragment() {

	// MainActivity에서 show 하기 전에 설정해줌
	var onChapterSelected: ((bookId: Int, chapter: Int) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var titleView: TextView
	private lateinit var backButton: TextView

	private var bookIds: List<Int> = emptyList()
	private var selectedBookId: Int = -1

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_book_chapter, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_picker)
		titleView = view.findViewById(R.id.text_sheet_title)
		backButton = view.findViewById(R.id.btn_back)

		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		backButton.setOnClickListener { showBookList() }

		showBookList()
	}

	private fun showBookList() {
		titleView.text = "책 선택"
		backButton.visibility = View.GONE

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			bookIds = db.bibleDao().getBookIds()
			val names = bookIds.map { BibleBooks.nameOf(it) }

			recyclerView.adapter = SimpleListAdapter(names) { position ->
				selectedBookId = bookIds[position]
				showChapterList(names[position])
			}
		}
	}

	private fun showChapterList(bookName: String) {
		titleView.text = "$bookName - 장 선택"
		backButton.visibility = View.VISIBLE

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val chapters = db.bibleDao().getChapters(selectedBookId)
			val labels = chapters.map { "${it}장" }

			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				onChapterSelected?.invoke(selectedBookId, chapters[position])
				dismiss()
			}
		}
	}
}