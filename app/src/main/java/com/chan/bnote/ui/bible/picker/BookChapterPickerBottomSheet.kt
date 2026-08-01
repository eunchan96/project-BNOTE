package com.chan.bnote.ui.bible.picker

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
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.chan.bnote.ui.common.GridNumberAdapter
import kotlinx.coroutines.launch

class BookChapterPickerBottomSheet(
	private val translation: String,
	// 현재 읽고 있던 책이 있으면 미리 선택된 상태로 열어준다 (없으면 -1)
	private val initialBookId: Int = -1
) : FixedBottomSheetDialogFragment() {

	// bookId, chapter, verse 순서로 전달
	var onVerseSelected: ((bookId: Int, chapter: Int, verse: Int) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var tabBarContainer: LinearLayout

	private var selectedBookId: Int = -1
	private var selectedChapter: Int = -1

	private enum class Step { BOOK, CHAPTER, VERSE }

	private var currentStep = Step.BOOK

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
		tabBarContainer = view.findViewById(R.id.container_tab_bar)

		// 현재 보고 있던 책이 있으면 미리 선택된 상태로 시작한다 (장 탭 바로 이동 가능)
		selectedBookId = initialBookId

		goToStep(Step.BOOK)
	}

	/** 탭 이동(성경 → 장 → 절)과 화면 갱신을 함께 처리하는 진입점. */
	private fun goToStep(step: Step) {
		currentStep = step
		updateHeader()
		renderTabs()
		when (step) {
			Step.BOOK -> showBookGrid()
			Step.CHAPTER -> showChapterGrid()
			Step.VERSE -> showVerseGrid()
		}
	}

	private fun updateHeader() {
		titleView.text = if (selectedBookId == -1) {
			"책 선택"
		} else {
			val unit = BibleBooks.chapterUnit(selectedBookId)
			val chapterLabel =
				if (selectedChapter != -1) "${selectedChapter}${unit}" else "_${unit}"
			"${BibleBooks.nameOf(selectedBookId)} ${chapterLabel} _절"
		}
	}

	private fun renderTabs() {
		val tabs = listOf(
			PickerTab(label = "성경", enabled = true, selected = currentStep == Step.BOOK) {
				goToStep(Step.BOOK)
			},
			PickerTab(
				label = "장",
				enabled = selectedBookId != -1,
				selected = currentStep == Step.CHAPTER
			) {
				if (selectedBookId != -1) goToStep(Step.CHAPTER)
			},
			PickerTab(
				label = "절",
				enabled = selectedChapter != -1,
				selected = currentStep == Step.VERSE
			) {
				if (selectedChapter != -1) goToStep(Step.VERSE)
			}
		)
		renderPickerTabs(requireContext(), tabBarContainer, tabs)
	}

	private fun showBookGrid() {
		scrollBookGrid.visibility = View.VISIBLE
		recyclerView.visibility = View.GONE

		bookGridContainer.removeAllViews()
		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(8) }
			}
			for (bookId in group) {
				val isSelected = bookId == selectedBookId
				val button = TextView(requireContext()).apply {
					text = BibleBooks.gridDisplayName(bookId)
					gravity = Gravity.CENTER
					textSize = 13f
					maxLines = 2
					setPadding(dp(4), dp(10), dp(4), dp(10))
					background = androidx.core.content.ContextCompat.getDrawable(
						requireContext(),
						if (isSelected) R.drawable.bg_book_button_selected else R.drawable.bg_book_button
					)
					setTextColor(
						androidx.core.content.ContextCompat.getColor(
							requireContext(),
							if (isSelected) R.color.white else R.color.text_primary
						)
					)
					isClickable = true
					isFocusable = true
					// 대부분의 책 이름은 한 줄이라 칸이 작지만, "데살로니가전서"처럼 두 줄이 되는
					// 이름이 있는 줄(row)은 그 줄만 자연스럽게 커진다(전체 그리드가 다 같이 커지지
					// 않도록, MATCH_PARENT로 같은 줄의 제일 큰 칸에 맞춰지게 한다).
					layoutParams = LinearLayout.LayoutParams(
						0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
					).apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						selectedBookId = bookId
						selectedChapter = -1
						goToStep(Step.CHAPTER)
					}
				}
				row.addView(button)
			}
			// 행에 4개 미만이면(구약 마지막 줄 등) 남는 칸만큼 빈 스페이서를 넣어서 늘어나지 않게 한다.
			repeat(4 - group.size) {
				row.addView(View(requireContext()).apply {
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
							.apply {
								marginStart = dp(4)
								marginEnd = dp(4)
							}
				})
			}
			bookGridContainer.addView(row)
		}
	}

	private fun showChapterGrid() {
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val chapters = db.bibleDao().getChapters(translation, selectedBookId)

			recyclerView.adapter = GridNumberAdapter(
				chapters,
				if (selectedChapter != -1) setOf(selectedChapter) else emptySet()
			) { position ->
				selectedChapter = chapters[position]
				goToStep(Step.VERSE)
			}
		}
	}

	private fun showVerseGrid() {
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val verseNumbers =
				db.bibleDao().getVerses(translation, selectedBookId, selectedChapter)
					.map { it.verse }

			recyclerView.adapter = GridNumberAdapter(verseNumbers) { position ->
				onVerseSelected?.invoke(selectedBookId, selectedChapter, verseNumbers[position])
				dismiss()
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}