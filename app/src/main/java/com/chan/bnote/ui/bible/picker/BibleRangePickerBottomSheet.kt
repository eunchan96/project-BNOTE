package com.chan.bnote.ui.bible.picker

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
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
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.chan.bnote.ui.common.GridNumberAdapter
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.launch

/**
 * 성경 구절(범위) 선택 bottom sheet.
 *
 * "여러 구절 선택하기"(기본 체크)가 켜져 있으면, 같은 장 안에서는 절 그리드에서 시작 절과 끝 절을
 * 직접 두 번 탭해서 범위를 고른다(예: 1~5절이면 1을 누르고 5를 누름 — 누른 절은 갈색으로 표시됨).
 * 다음 장까지 걸쳐야 하면 그 옆의 "다음 장까지 선택하기"를 켜면, 시작 절을 고른 뒤 예전처럼
 * 끝 장 → 끝 절을 따로 골라서 정한다.
 */
class BibleRangePickerBottomSheet : FixedBottomSheetDialogFragment() {

	// sermonId는 저장 시점에 채워지므로 0으로 임시 세팅
	var onRangeSelected: ((SermonBibleRef) -> Unit)? = null

	// 이미 추가된 본문을 다시 눌러서 열 때, 그 정보로 미리 채워서 보여준다.
	var existingRef: SermonBibleRef? = null
	var onDeleteRequested: (() -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var checkboxMulti: MaterialCheckBox
	private lateinit var checkboxCrossChapter: MaterialCheckBox
	private lateinit var textSelectedStart: TextView
	private lateinit var tabBarContainer: LinearLayout
	private lateinit var btnDelete: ImageView

	// "여러 구절 선택하기" 체크 여부 — 기본으로 켜둔다.
	private var isMultiMode = true

	// "다음 장까지 선택하기" 체크 여부 — 켜져 있을 때만 예전처럼 끝 장/절을 따로 고른다.
	private var crossChapter = false

	// 여러 구절 + 다음 장까지 모드에서: false = 시작 구절 선택 중, true = 끝 구절 선택 중
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
		checkboxCrossChapter = view.findViewById(R.id.checkbox_cross_chapter)
		textSelectedStart = view.findViewById(R.id.text_selected_start)
		tabBarContainer = view.findViewById(R.id.container_tab_bar)
		btnDelete = view.findViewById(R.id.btn_delete_ref)

		// 이미 추가된 본문을 다시 눌러서 연 경우: 그 정보로 미리 채워둔다. (여러 구절 범위였으면 끝 장까지만
		// 미리 채우고, 끝 절은 다시 골라야 한다 — 상태 변수 구조상 끝 절만 별도로 들고 있지 않아서다.)
		val existing = existingRef
		if (existing != null) {
			bookId = existing.startBookId
			startChapter = existing.startChapter
			startVerse = existing.startVerse
			if (existing.endChapter != existing.startChapter || existing.endVerse != existing.startVerse) {
				crossChapter = existing.endChapter != existing.startChapter
			}
			btnDelete.visibility = View.VISIBLE
			btnDelete.setOnClickListener {
				onDeleteRequested?.invoke()
				dismiss()
			}
		}

		checkboxMulti.isChecked = isMultiMode
		checkboxCrossChapter.isChecked = crossChapter
		checkboxMulti.setOnCheckedChangeListener { _, checked ->
			if (!checked && isMultiMode && !crossChapter && startVerse != -1 && !isSelectingEnd) {
				// 시작 절만 눌러놓은 채로("몇 절부터"만 정해진 상태) 체크를 풀면, 그 절 하나로 확정한다.
				isMultiMode = false
				finalizeSingle(startVerse)
				return@setOnCheckedChangeListener
			}
			isMultiMode = checked
			if (!checked) {
				crossChapter = false
				checkboxCrossChapter.isChecked = false
			}
			startVerse = -1
			isSelectingEnd = false
			goToStep(currentStep)
		}

		checkboxCrossChapter.setOnCheckedChangeListener { _, checked ->
			crossChapter = checked
			startVerse = -1
			isSelectingEnd = false
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
		// 체크박스는 "다음 장까지" 모드로 끝 구절을 고르는 중일 때만 숨기고 라벨로 바꾼다.
		// 같은 장 안에서 직접 두 번 탭하는 모드에서는 화면이 안 바뀌니 체크박스도 계속 보여준다
		// (그래야 시작 절만 찍은 상태에서 체크를 풀어 한 절로 확정하는 것도 할 수 있다).
		val showStartLabel = isMultiMode && crossChapter && isSelectingEnd
		checkboxMulti.visibility = if (showStartLabel) View.GONE else View.VISIBLE
		checkboxCrossChapter.visibility =
			if (showStartLabel || !isMultiMode) View.GONE else View.VISIBLE
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
		val tabs = if (isMultiMode && crossChapter && isSelectingEnd) {
			// 다음 장까지 모드로 끝 구절 선택 중: 책은 고정이므로 장 | 절 두 탭만 보여준다.
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

		val isCrossChapterEndSelection = isMultiMode && crossChapter && isSelectingEnd

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var chapters = db.bibleDao().getChapters("NKRV", bookId)
			if (isCrossChapterEndSelection) chapters =
				chapters.filter { it >= startChapter } // 끝 장은 시작 장 이상만

			recyclerView.adapter = GridNumberAdapter(
				chapters,
				(if (isCrossChapterEndSelection) endChapter else startChapter)
					.let { if (it != -1) setOf(it) else emptySet() }
			) { position ->
				if (isCrossChapterEndSelection) {
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
		val isCrossChapterEndSelection = isMultiMode && crossChapter && isSelectingEnd
		val chapter = if (isCrossChapterEndSelection) endChapter else startChapter

		scrollBookGrid.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var verses = db.bibleDao().getVerses("NKRV", bookId, chapter).map { it.verse }
			if (isCrossChapterEndSelection && endChapter == startChapter) {
				verses = verses.filter { it >= startVerse } // 같은 장이면 끝 절은 시작 절 이상만
			}

			// 같은 장 안에서 직접 두 번 탭하는 모드일 때만, 이미 찍은 시작 절을 갈색으로 표시해준다.
			val preSelected = if (isMultiMode && !crossChapter && startVerse != -1) {
				setOf(startVerse)
			} else {
				emptySet()
			}

			recyclerView.adapter = GridNumberAdapter(verses, preSelected) { position ->
				onVerseTapped(verses[position])
			}
		}
	}

	private fun onVerseTapped(selectedVerse: Int) {
		if (!isMultiMode) {
			finalizeSingle(selectedVerse)
			return
		}

		if (crossChapter) {
			if (!isSelectingEnd) {
				// 시작 구절 선택 완료 → 끝 구절은 장부터 다시 고른다(예전 방식 그대로).
				startVerse = selectedVerse
				isSelectingEnd = true
				goToStep(Step.CHAPTER)
			} else {
				finalizeRange(startChapter, startVerse, endChapter, selectedVerse)
			}
			return
		}

		// 같은 장 안에서 직접 두 번 탭: 첫 탭 = 시작, 두 번째 탭 = 끝(순서 상관없이 작은 쪽이 시작).
		if (startVerse == -1) {
			startVerse = selectedVerse
			(recyclerView.adapter as? GridNumberAdapter)?.updateSelection(setOf(startVerse))
			updateHeader()
		} else {
			val finalStart = minOf(startVerse, selectedVerse)
			val finalEnd = maxOf(startVerse, selectedVerse)
			finalizeRange(startChapter, finalStart, startChapter, finalEnd)
		}
	}

	private fun finalizeSingle(verse: Int) {
		onRangeSelected?.invoke(
			SermonBibleRef(
				sermonId = 0,
				startBookId = bookId,
				startChapter = startChapter,
				startVerse = verse,
				endBookId = bookId,
				endChapter = startChapter,
				endVerse = verse
			)
		)
		dismiss()
	}

	private fun finalizeRange(sChapter: Int, sVerse: Int, eChapter: Int, eVerse: Int) {
		onRangeSelected?.invoke(
			SermonBibleRef(
				sermonId = 0,
				startBookId = bookId,
				startChapter = sChapter,
				startVerse = sVerse,
				endBookId = bookId,
				endChapter = eChapter,
				endVerse = eVerse
			)
		)
		dismiss()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}