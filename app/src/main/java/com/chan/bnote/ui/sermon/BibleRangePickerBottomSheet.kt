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
import com.chan.bnote.ui.bible.PickerTab
import com.chan.bnote.ui.bible.renderPickerTabs
import com.chan.bnote.ui.common.GridNumberAdapter
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.launch

class BibleRangePickerBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.75f

	// sermonId는 저장 시점에 채워지므로 0으로 임시 세팅
	var onRangeSelected: ((SermonBibleRef) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var checkboxMulti: MaterialCheckBox
	private lateinit var textSelectedStart: TextView
	private lateinit var tabBarContainer: LinearLayout

	// "여러 구절 선택하기" 체크 여부
	private var isMultiMode = false

	// 여러 구절 모드에서: false = 첫 번째(시작) 구절 선택 중, true = 두 번째(끝) 구절 선택 중
	private var isSelectingEnd = false

	private enum class Step { BOOK, CHAPTER, VERSE }

	private var currentStep = Step.BOOK

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
		checkboxMulti = view.findViewById(R.id.checkbox_multi_select)
		textSelectedStart = view.findViewById(R.id.text_selected_start)
		tabBarContainer = view.findViewById(R.id.container_tab_bar)

		checkboxMulti.setOnCheckedChangeListener { _, checked ->
			isMultiMode = checked
			// 체크박스는 첫 번째(시작) 선택 단계에서만 보이므로, 이미 고른 책/장/절은
			// 초기화하지 않고 그대로 유지한 채 현재 단계만 다시 그린다.
			goToStep(currentStep)
		}

		// 상단에 표시된 "OO 1장 1절~" 라벨을 누르면 첫 번째(시작) 선택을 다시 할 수 있다.
		textSelectedStart.setOnClickListener {
			isSelectingEnd = false
			endChapter = -1
			goToStep(Step.VERSE)
		}

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
		val showStartLabel = isMultiMode && isSelectingEnd
		checkboxMulti.visibility = if (showStartLabel) View.GONE else View.VISIBLE
		textSelectedStart.visibility = if (showStartLabel) View.VISIBLE else View.GONE
		if (showStartLabel) {
			val unit = BibleBooks.chapterUnit(bookId)
			textSelectedStart.text =
				"${BibleBooks.nameOf(bookId)} ${startChapter}${unit} ${startVerse}절~"
		}

		titleView.text = if (bookId == -1) {
			"책 선택"
		} else {
			val unit = BibleBooks.chapterUnit(bookId)
			val chapter = if (showStartLabel) endChapter else startChapter
			val chapterLabel = if (chapter != -1) "${chapter}${unit}" else "_${unit}"
			"${BibleBooks.nameOf(bookId)} ${chapterLabel} _절"
		}
	}

	private fun renderTabs() {
		val tabs = if (isMultiMode && isSelectingEnd) {
			// 두 번째(끝) 선택: 책은 고정이므로 장 | 절 두 탭만 보여준다.
			listOf(
				PickerTab(label = "장", enabled = true, selected = currentStep == Step.CHAPTER) {
					goToStep(Step.CHAPTER)
				},
				PickerTab(
					label = "절",
					enabled = endChapter != -1,
					selected = currentStep == Step.VERSE
				) {
					if (endChapter != -1) goToStep(Step.VERSE)
				}
			)
		} else {
			listOf(
				PickerTab(label = "성경", enabled = true, selected = currentStep == Step.BOOK) {
					goToStep(Step.BOOK)
				},
				PickerTab(
					label = "장",
					enabled = bookId != -1,
					selected = currentStep == Step.CHAPTER
				) {
					if (bookId != -1) goToStep(Step.CHAPTER)
				},
				PickerTab(
					label = "절",
					enabled = startChapter != -1,
					selected = currentStep == Step.VERSE
				) {
					if (startChapter != -1) goToStep(Step.VERSE)
				}
			)
		}
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
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(4) }
			}
			for (id in group) {
				val isSelected = id == bookId
				val button = TextView(requireContext()).apply {
					text = BibleBooks.nameOf(id)
					gravity = Gravity.CENTER
					textSize = 13f
					maxLines = 2
					setPadding(dp(4), dp(14), dp(4), dp(14))
					background = ContextCompat.getDrawable(
						requireContext(),
						if (isSelected) R.drawable.bg_book_button_selected else R.drawable.bg_book_button
					)
					setTextColor(
						ContextCompat.getColor(
							requireContext(),
							if (isSelected) R.color.white else R.color.text_primary
						)
					)
					isClickable = true
					isFocusable = true
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						bookId = id
						startChapter = -1
						startVerse = -1
						goToStep(Step.CHAPTER)
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

	private fun showChapterGrid() {
		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		val isEndSelection = isMultiMode && isSelectingEnd

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var chapters = db.bibleDao().getChapters("NKRV", bookId)
			if (isEndSelection) chapters = chapters.filter { it >= startChapter } // 끝 장은 시작 장 이상만

			recyclerView.adapter = GridNumberAdapter(chapters) { position ->
				if (isEndSelection) {
					endChapter = chapters[position]
				} else {
					startChapter = chapters[position]
					startVerse = -1
				}
				goToStep(Step.VERSE)
			}
		}
	}

	private fun showVerseGrid() {
		val isEndSelection = isMultiMode && isSelectingEnd
		val chapter = if (isEndSelection) endChapter else startChapter

		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var verses = db.bibleDao().getVerses("NKRV", bookId, chapter).map { it.verse }
			if (isEndSelection && endChapter == startChapter) {
				verses = verses.filter { it >= startVerse } // 같은 장이면 끝 절은 시작 절 이상만
			}

			recyclerView.adapter = GridNumberAdapter(verses) { position ->
				val selectedVerse = verses[position]
				if (!isMultiMode) {
					// 단일 구절 선택: 시작 = 끝
					onRangeSelected?.invoke(
						SermonBibleRef(
							sermonId = 0,
							startBookId = bookId,
							startChapter = startChapter,
							startVerse = selectedVerse,
							endBookId = bookId,
							endChapter = startChapter,
							endVerse = selectedVerse
						)
					)
					dismiss()
				} else if (!isSelectingEnd) {
					// 첫 번째(시작) 선택 완료 → 두 번째(끝) 선택은 장부터 바로 시작
					startVerse = selectedVerse
					isSelectingEnd = true
					goToStep(Step.CHAPTER)
				} else {
					// 두 번째(끝) 선택 완료
					onRangeSelected?.invoke(
						SermonBibleRef(
							sermonId = 0,
							startBookId = bookId,
							startChapter = startChapter,
							startVerse = startVerse,
							endBookId = bookId,
							endChapter = endChapter,
							endVerse = selectedVerse
						)
					)
					dismiss()
				}
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}