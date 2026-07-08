package com.chan.bnote.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBookGroups
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class BookChapterPickerBottomSheet : BottomSheetDialogFragment() {

	// bookId, chapter, verse 순서로 전달
	var onVerseSelected: ((bookId: Int, chapter: Int, verse: Int) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var backButton: TextView

	private var selectedBookId: Int = -1
	private var selectedChapter: Int = -1

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_book_chapter, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_picker)
		scrollBookGrid = view.findViewById(R.id.scroll_book_grid)
		bookGridContainer = view.findViewById(R.id.container_book_grid)
		titleView = view.findViewById(R.id.text_sheet_title)
		backButton = view.findViewById(R.id.btn_back)

		showBookList()
	}

	private fun showBookList() {
		titleView.text = "책 선택"
		backButton.visibility = View.GONE
		scrollBookGrid.visibility = View.VISIBLE
		recyclerView.visibility = View.GONE

		bookGridContainer.removeAllViews()
		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(4) }
			}
			for (bookId in group) {
				val button = TextView(requireContext()).apply {
					text = BibleBooks.nameOf(bookId)
					gravity = Gravity.CENTER
					textSize = 13f
					maxLines = 2
					setPadding(dp(4), dp(14), dp(4), dp(14))
					background = androidx.core.content.ContextCompat.getDrawable(
						requireContext(), R.drawable.bg_book_button
					)
					isClickable = true
					isFocusable = true
					layoutParams = LinearLayout.LayoutParams(
						0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
					).apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						selectedBookId = bookId
						showChapterList(BibleBooks.nameOf(bookId))
					}
				}
				row.addView(button)
			}
			bookGridContainer.addView(row)
		}
	}

	private fun showChapterList(bookName: String) {
		titleView.text = "$bookName - 장 선택"
		backButton.visibility = View.VISIBLE
		backButton.setOnClickListener { showBookList() }
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val chapters = db.bibleDao().getChapters(selectedBookId)

			recyclerView.adapter = GridNumberAdapter(chapters) { position ->
				selectedChapter = chapters[position]
				showVerseList(bookName, selectedChapter)
			}
		}
	}

	private fun showVerseList(bookName: String, chapter: Int) {
		titleView.text = "$bookName ${chapter}장 - 절 선택"
		backButton.visibility = View.VISIBLE
		backButton.setOnClickListener { showChapterList(bookName) }
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val verseNumbers = db.bibleDao().getVerses(selectedBookId, chapter).map { it.verse }

			recyclerView.adapter = GridNumberAdapter(verseNumbers) { position ->
				onVerseSelected?.invoke(selectedBookId, chapter, verseNumbers[position])
				dismiss()
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}