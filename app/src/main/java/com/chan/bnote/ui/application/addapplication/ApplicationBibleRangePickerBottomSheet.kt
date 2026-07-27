package com.chan.bnote.ui.application.addapplication

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
import com.chan.bnote.data.application.ApplicationBibleRef
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.DraggableBottomSheet
import com.chan.bnote.ui.bible.picker.PickerTab
import com.chan.bnote.ui.bible.picker.renderPickerTabs
import com.chan.bnote.ui.common.GridNumberAdapter
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.launch

/**
 * 적용에서 쓰는 성경 구절(범위) 선택 bottom sheet. 설교의 BibleRangePickerBottomSheet와 거의 같지만,
 * "장만 선택" 체크박스가 추가된다 — 켜면 절 없이 장 단위로만 고른다(통독 카테고리일 때 기본으로 켜둠).
 */
class ApplicationBibleRangePickerBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.75f

	var onRangeSelected: ((ApplicationBibleRef) -> Unit)? = null
	var existingRef: ApplicationBibleRef? = null
	var onDeleteRequested: (() -> Unit)? = null

	// 카테고리가 "통독"이면 기본으로 켜두기 위해 호출부에서 미리 세팅해준다.
	var defaultChapterOnly: Boolean = false

	private lateinit var recyclerView: RecyclerView
	private lateinit var scrollBookGrid: ScrollView
	private lateinit var bookGridContainer: LinearLayout
	private lateinit var titleView: TextView
	private lateinit var checkboxMulti: MaterialCheckBox
	private lateinit var checkboxCrossChapter: MaterialCheckBox
	private lateinit var checkboxChapterOnly: MaterialCheckBox
	private lateinit var textSelectedStart: TextView
	private lateinit var tabBarContainer: LinearLayout
	private lateinit var btnDelete: ImageView

	private var isMultiMode = true
	private var crossChapter = false
	private var chapterOnly = false
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
		return inflater.inflate(R.layout.bottom_sheet_application_bible_range, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_picker)
		scrollBookGrid = view.findViewById(R.id.scroll_book_grid)
		bookGridContainer = view.findViewById(R.id.container_book_grid)
		titleView = view.findViewById(R.id.text_sheet_title)
		checkboxMulti = view.findViewById(R.id.checkbox_multi_select)
		checkboxCrossChapter = view.findViewById(R.id.checkbox_cross_chapter)
		checkboxChapterOnly = view.findViewById(R.id.checkbox_chapter_only)
		textSelectedStart = view.findViewById(R.id.text_selected_start)
		tabBarContainer = view.findViewById(R.id.container_tab_bar)
		btnDelete = view.findViewById(R.id.btn_delete_ref)

		val existing = existingRef
		if (existing != null) {
			bookId = existing.startBookId
			startChapter = existing.startChapter
			startVerse = existing.startVerse
			chapterOnly = existing.isChapterOnly
			if (!chapterOnly && (existing.endChapter != existing.startChapter || existing.endVerse != existing.startVerse)) {
				crossChapter = existing.endChapter != existing.startChapter
			}
			btnDelete.visibility = View.VISIBLE
			btnDelete.setOnClickListener {
				onDeleteRequested?.invoke()
				dismiss()
			}
		} else {
			chapterOnly = defaultChapterOnly
		}

		checkboxMulti.isChecked = isMultiMode
		checkboxCrossChapter.isChecked = crossChapter
		checkboxChapterOnly.isChecked = chapterOnly

		checkboxMulti.setOnCheckedChangeListener { _, checked ->
			if (!checked && isMultiMode && !crossChapter && startVerse != -1 && !isSelectingEnd) {
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
			goToStep(if (chapterOnly) Step.CHAPTER else currentStep)
		}

		checkboxCrossChapter.setOnCheckedChangeListener { _, checked ->
			crossChapter = checked
			startVerse = -1
			isSelectingEnd = false
			goToStep(currentStep)
		}

		checkboxChapterOnly.setOnCheckedChangeListener { _, checked ->
			chapterOnly = checked
			startChapter = -1
			startVerse = -1
			isSelectingEnd = false
			goToStep(if (bookId != -1) Step.CHAPTER else Step.BOOK)
		}

		textSelectedStart.setOnClickListener {
			isSelectingEnd = false
			endChapter = -1
			goToStep(Step.VERSE)
		}

		goToStep(Step.BOOK)
	}

	private fun goToStep(step: Step) {
		currentStep = if (chapterOnly && step == Step.VERSE) Step.CHAPTER else step
		updateHeader()
		renderTabs()
		when (currentStep) {
			Step.BOOK -> showBookGrid()
			Step.CHAPTER -> showChapterGrid()
			Step.VERSE -> showVerseGrid()
		}
	}

	private fun updateHeader() {
		val showStartLabel = !chapterOnly && isMultiMode && crossChapter && isSelectingEnd
		checkboxMulti.visibility = if (showStartLabel) View.GONE else View.VISIBLE
		checkboxCrossChapter.visibility =
			if (showStartLabel || !isMultiMode || chapterOnly) View.GONE else View.VISIBLE
		textSelectedStart.visibility = if (showStartLabel) View.VISIBLE else View.GONE
		if (showStartLabel) {
			val unit = BibleBooks.chapterUnit(bookId)
			textSelectedStart.text =
				"${BibleBooks.nameOf(bookId)} ${startChapter}${unit} ${startVerse}절~"
		}

		titleView.text = if (bookId == -1) {
			"책 선택"
		} else if (chapterOnly) {
			val unit = BibleBooks.chapterUnit(bookId)
			"${BibleBooks.nameOf(bookId)} _${unit}"
		} else {
			val unit = BibleBooks.chapterUnit(bookId)
			val chapter = if (showStartLabel) endChapter else startChapter
			val chapterLabel = if (chapter != -1) "${chapter}${unit}" else "_${unit}"
			"${BibleBooks.nameOf(bookId)} ${chapterLabel} _절"
		}
	}

	private fun renderTabs() {
		val tabs = if (chapterOnly) {
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
				}
			)
		} else if (isMultiMode && crossChapter && isSelectingEnd) {
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

		val isCrossChapterEndSelection =
			!chapterOnly && isMultiMode && crossChapter && isSelectingEnd

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			var chapters = db.bibleDao().getChapters("NKRV", bookId)
			if (isCrossChapterEndSelection) chapters = chapters.filter { it >= startChapter }

			val preSelected = when {
				isCrossChapterEndSelection -> if (endChapter != -1) setOf(endChapter) else emptySet()
				chapterOnly && isMultiMode && startChapter != -1 -> setOf(startChapter)
				!chapterOnly -> if (startChapter != -1) setOf(startChapter) else emptySet()
				else -> emptySet()
			}

			recyclerView.adapter = GridNumberAdapter(chapters, preSelected) { position ->
				val tappedChapter = chapters[position]
				if (chapterOnly) {
					onChapterOnlyTapped(tappedChapter)
				} else if (isCrossChapterEndSelection) {
					endChapter = tappedChapter
					goToStep(Step.VERSE)
				} else {
					startChapter = tappedChapter
					startVerse = -1
					goToStep(Step.VERSE)
				}
			}
		}
	}

	/** 장만 선택 모드에서 장을 탭했을 때. 단일 모드면 바로 확정, 여러 구절 모드면 절 그리드와 같은
	 * 방식으로 시작/끝 장을 두 번 탭해서 고른다. */
	private fun onChapterOnlyTapped(tappedChapter: Int) {
		if (!isMultiMode) {
			finalizeChapterOnly(tappedChapter, tappedChapter)
			return
		}
		if (startChapter == -1) {
			startChapter = tappedChapter
			(recyclerView.adapter as? GridNumberAdapter)?.updateSelection(setOf(startChapter))
			updateHeader()
		} else {
			val finalStart = minOf(startChapter, tappedChapter)
			val finalEnd = maxOf(startChapter, tappedChapter)
			finalizeChapterOnly(finalStart, finalEnd)
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
				verses = verses.filter { it >= startVerse }
			}

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
				startVerse = selectedVerse
				isSelectingEnd = true
				goToStep(Step.CHAPTER)
			} else {
				finalizeRange(startChapter, startVerse, endChapter, selectedVerse)
			}
			return
		}

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
			ApplicationBibleRef(
				applicationId = 0,
				startBookId = bookId,
				startChapter = startChapter,
				startVerse = verse,
				endBookId = bookId,
				endChapter = startChapter,
				endVerse = verse,
				isChapterOnly = false
			)
		)
		dismiss()
	}

	private fun finalizeRange(sChapter: Int, sVerse: Int, eChapter: Int, eVerse: Int) {
		onRangeSelected?.invoke(
			ApplicationBibleRef(
				applicationId = 0,
				startBookId = bookId,
				startChapter = sChapter,
				startVerse = sVerse,
				endBookId = bookId,
				endChapter = eChapter,
				endVerse = eVerse,
				isChapterOnly = false
			)
		)
		dismiss()
	}

	private fun finalizeChapterOnly(sChapter: Int, eChapter: Int) {
		onRangeSelected?.invoke(
			ApplicationBibleRef(
				applicationId = 0,
				startBookId = bookId,
				startChapter = sChapter,
				startVerse = 1,
				endBookId = bookId,
				endChapter = eChapter,
				endVerse = 1,
				isChapterOnly = true
			)
		)
		dismiss()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}