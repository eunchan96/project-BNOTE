package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.ReadingProgress
import com.chan.bnote.ui.DraggableBottomSheet
import kotlinx.coroutines.launch

class ReadingPlanBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.85f

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_reading_plan, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			// 책별 총 장수 계산
			val maxChapterByBook = (1..66).associateWith { bookId ->
				db.bibleDao().getMaxChapter("GAEYEOK", bookId)
			}
			val totalChapters = maxChapterByBook.values.sum()

			val readList = db.readingProgressDao().getAll()
			val readByBook = readList.groupBy { it.bookId }
			val totalRead = readList.size

			val overallText = view.findViewById<TextView>(R.id.text_overall_progress)
			val progressBar = view.findViewById<ProgressBar>(R.id.progress_overall)
			val percent = if (totalChapters > 0) (totalRead * 100 / totalChapters) else 0
			overallText.text = "전체 $totalRead / $totalChapters 장 읽음 ($percent%)"
			progressBar.progress = percent

			renderBookGrid(view, maxChapterByBook, readByBook)
		}
	}

	private fun renderBookGrid(
		rootView: View,
		maxChapterByBook: Map<Int, Int>,
		readByBook: Map<Int, List<ReadingProgress>>
	) {
		val gridContainer = rootView.findViewById<LinearLayout>(R.id.container_book_progress_grid)
		gridContainer.removeAllViews()

		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(4) }
			}
			for (bookId in group) {
				val maxChapter = maxChapterByBook[bookId] ?: 1
				val readCount = readByBook[bookId]?.size ?: 0

				val bgRes = when {
					readCount == 0 -> R.drawable.bg_book_progress_none
					readCount >= maxChapter -> R.drawable.bg_book_progress_done
					else -> R.drawable.bg_book_progress_partial
				}
				val textColorRes =
					if (readCount >= maxChapter && readCount > 0) R.color.white else R.color.book_progress_none_text
				val textColor = ContextCompat.getColor(requireContext(), textColorRes)

				val container = LinearLayout(requireContext()).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER
					setPadding(dp(4), dp(12), dp(4), dp(12))
					background = ContextCompat.getDrawable(requireContext(), bgRes)
					isClickable = true
					isFocusable = true
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						val sheet = ReadingPlanChapterBottomSheet(bookId)
						sheet.show(parentFragmentManager, "reading_plan_chapter")
					}
				}

				val nameView = TextView(requireContext()).apply {
					text = BibleBooks.nameOf(bookId)
					textSize = 13f
					maxLines = 2
					gravity = Gravity.CENTER
					setTextColor(textColor)
				}
				val countView = TextView(requireContext()).apply {
					text = "$readCount/$maxChapter"
					textSize = 10f
					gravity = Gravity.CENTER
					setTextColor(textColor)
					alpha = 0.8f
				}

				container.addView(nameView)
				container.addView(countView)
				row.addView(container)
			}
			gridContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}