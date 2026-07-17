package com.chan.bnote.ui.sermon

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.DraggableBottomSheet
import com.chan.bnote.ui.common.GridNumberAdapter
import kotlinx.coroutines.launch

class BibleRangePickerBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.75f

	// sermonId는 저장 시점에 채워지므로 0으로 임시 세팅
	var onRangeSelected: ((SermonBibleRef) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var backButton: TextView

	private var bookId = -1
	private var startChapter = -1
	private var startVerse = -1
	private var endChapter = -1

	override fun onCreateView(
		inflater: android.view.LayoutInflater,
		container: android.view.ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_bible_range, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_picker)
		scrollBookGrid = view.findViewById(R.id.scroll_book_grid)
		bookGridContainer = view.findViewById(R.id.container_book_grid)
		titleView = view.findViewById(R.id.text_sheet_title)
		backButton = view.findViewById(R.id.btn_back)

		showBookStep()
	}

	private fun showBookStep() {
		titleView.text = "책 선택"
		backButton.visibility = View.GONE
		scrollBookGrid.visibility = View.VISIBLE
		recyclerView.visibility = View.GONE

		bookGridContainer.removeAllViews()
		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(4) }
			}
			for (id in group) {
				val button = TextView(requireContext()).apply {
					text = BibleBooks.nameOf(id)
					gravity = Gravity.CENTER
					textSize = 13f
					maxLines = 2
					setPadding(dp(4), dp(14), dp(4), dp(14))
					background =
						ContextCompat.getDrawable(requireContext(), R.drawable.bg_book_button)
					isClickable = true
					isFocusable = true
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						bookId = id
						showChapterStep(isStart = true)
					}
				}
				row.addView(button)
			}
			// 행에 4개 미만이면(구약 마지막 줄 등) 남는 칸만큼 빈 스페이서를 넣어서 늘어나지 않게 한다.
			repeat(4 - group.size) {
				row.addView(View(requireContext()).apply {
					layoutParams =
						LinearLayout.LayoutParams(0, 0, 1f).apply {
							marginStart = dp(4)
							marginEnd = dp(4)
						}
				})
			}
			bookGridContainer.addView(row)
		}
	}

	private fun showChapterStep(isStart: Boolean) {
		titleView.text = "${BibleBooks.nameOf(bookId)} - ${if (isStart) "시작" else "끝"} 장 선택"
		backButton.visibility = View.VISIBLE
		backButton.setOnClickListener {
			if (isStart) showBookStep() else showVerseStep(isStart = true)
		}
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var chapters = db.bibleDao().getChapters("NKRV", bookId)
			if (!isStart) chapters = chapters.filter { it >= startChapter } // 끝 장은 시작 장 이상만

			recyclerView.adapter = GridNumberAdapter(chapters) { position ->
				if (isStart) {
					startChapter = chapters[position]
					showVerseStep(isStart = true)
				} else {
					endChapter = chapters[position]
					showVerseStep(isStart = false)
				}
			}
		}
	}

	private fun showVerseStep(isStart: Boolean) {
		val chapter = if (isStart) startChapter else endChapter
		titleView.text =
			"${BibleBooks.nameOf(bookId)} ${chapter}${BibleBooks.chapterUnit(bookId)} - ${if (isStart) "시작" else "끝"} 절 선택"
		backButton.visibility = View.VISIBLE
		backButton.setOnClickListener {
			if (isStart) showChapterStep(isStart = true) else showChapterStep(isStart = false)
		}
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var verses = db.bibleDao().getVerses("NKRV", bookId, chapter).map { it.verse }
			if (!isStart && endChapter == startChapter) {
				verses = verses.filter { it >= startVerse } // 같은 장이면 끝 절은 시작 절 이상만
			}

			recyclerView.adapter = GridNumberAdapter(verses) { position ->
				if (isStart) {
					startVerse = verses[position]
					showChapterStep(isStart = false) // 시작 선택 끝났으면 바로 끝 장 선택으로
				} else {
					val endVerse = verses[position]
					onRangeSelected?.invoke(
						SermonBibleRef(
							sermonId = 0,
							startBookId = bookId,
							startChapter = startChapter,
							startVerse = startVerse,
							endBookId = bookId,
							endChapter = endChapter,
							endVerse = endVerse
						)
					)
					dismiss()
				}
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}