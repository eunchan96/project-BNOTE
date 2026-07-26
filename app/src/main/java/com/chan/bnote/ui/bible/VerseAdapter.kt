package com.chan.bnote.ui.bible

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.SecondaryVerseText
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.ui.common.HighlightColors

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private val secondaryTextByVerse: Map<Int, SecondaryVerseText>?,
	private var bookmarks: MutableMap<Int, BibleBookmark>,
	private var fontSize: Int,
	private var selectedVerses: Set<Int>,
	private var highlightsByVerse: Map<Int, List<PartialHighlight>>,
	private var verseMemos: Map<Int, VerseMemo>,
	private var wordMemosByVerse: Map<Int, List<WordMemo>>,
	private val onVerseTap: (verse: Int) -> Unit,
	private val onVerseMemoView: (verse: Int, memo: VerseMemo) -> Unit,
	private val onHighlightRequested: (verse: Int, start: Int, end: Int, segment: Int) -> Unit,
	private val onWordMemoCreate: (verse: Int, start: Int, end: Int, segment: Int) -> Unit,
	private val onWordMemoView: (verse: Int, memo: WordMemo) -> Unit,
	// ViewPager2 안에 있다 보니, 손가락을 가만히 대고 있는(롱프레스) 도중에 아주 살짝만 흔들려도
	// ViewPager2가 "장 넘기기 스와이프"로 착각해서 가로채 버리는 문제가 있었다. 그래서 롱프레스가
	// 될 것 같은 순간(딱히 안 움직이고 일정 시간 지남)엔 ViewPager2의 스와이프를 잠깐 꺼달라고
	// 요청하기 위한 콜백. null이면(성경 읽기 화면이 아니면) 그냥 안 쓰인다.
	private val onGestureHoldStart: (() -> Unit)? = null,
	private val onGestureHoldEnd: (() -> Unit)? = null
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	companion object {
		private const val ID_HIGHLIGHT = 9001
		private const val ID_MEMO = 9002

		// 시편(bookId=19)은 전통적으로 5권으로 나뉜다: 1권 1~41편, 2권 42~72편, 3권 73~89편, 4권 90~106편, 5권 107~150편.
		private fun psalmsBookPartLabel(bookId: Int, chapter: Int, verse: Int): String? {
			if (bookId != 19 || verse != 1) return null
			return when (chapter) {
				1 -> "제일권"
				42 -> "제이권"
				73 -> "제삼권"
				90 -> "제사권"
				107 -> "제오권"
				else -> null
			}
		}
	}

	// 장의 최대 절 번호 자릿수에 따라 절 번호 칸 너비를 정한다 (1~2자리 장에서 괜히 넓지 않게).
	private val numberColumnWidthDp: Int by lazy {
		val maxVerse = verses.maxOfOrNull { it.verse } ?: 1
		when {
			maxVerse >= 100 -> 26
			else -> 20
		}
	}

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val root: View = view.findViewById(R.id.item_root)
		val contentRow: View = view.findViewById(R.id.content_row)
		val contentRow2: View = view.findViewById(R.id.content_row2)
		val spacerNumber2: View = view.findViewById(R.id.spacer_verse_number2)
		val title: TextView = view.findViewById(R.id.text_verse_title)
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val midTitle: TextView = view.findViewById(R.id.text_verse_mid_title)
		val content2: TextView = view.findViewById(R.id.text_verse_content2)
		val secondaryContent: TextView = view.findViewById(R.id.text_verse_content_secondary)
		val secondaryContent2: TextView = view.findViewById(R.id.text_verse_content_secondary2)
		val bookPart: TextView = view.findViewById(R.id.text_verse_book_part)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verseItem = verses[position]
		val isSelected = selectedVerses.contains(verseItem.verse)
		val context = holder.itemView.context

		val bookPartLabel =
			psalmsBookPartLabel(verseItem.bookId, verseItem.chapter, verseItem.verse)
		if (bookPartLabel != null) {
			holder.bookPart.text = "[$bookPartLabel]"
			holder.bookPart.visibility = View.VISIBLE
		} else {
			holder.bookPart.visibility = View.GONE
		}

		val titleText = verseItem.title
		if (!titleText.isNullOrBlank()) {
			holder.title.text = "<$titleText>"
			holder.title.visibility = View.VISIBLE
		} else {
			holder.title.visibility = View.GONE
		}

		// 장이 소제목·권 표시 없이 1절부터 바로 시작하면 맨 위가 너무 붙어 보여서 여백을 더 준다.
		val extraTopSpacing = position == 0 && titleText.isNullOrBlank() && bookPartLabel == null
		val topPaddingDp = if (extraTopSpacing) 12 else 4
		val topPaddingPx = (topPaddingDp * context.resources.displayMetrics.density).toInt()
		holder.contentRow.setPadding(
			holder.contentRow.paddingLeft,
			topPaddingPx,
			holder.contentRow.paddingRight,
			holder.contentRow.paddingBottom
		)

		// 절 번호 칸 너비를 장의 최대 절 번호 자릿수에 맞춘다. (아래 소제목 뒷부분 줄의 빈 칸도 같이 맞춘다)
		val numberWidthPx = (numberColumnWidthDp * context.resources.displayMetrics.density).toInt()
		if (holder.number.layoutParams.width != numberWidthPx) {
			holder.number.layoutParams = holder.number.layoutParams.apply { width = numberWidthPx }
		}
		if (holder.spacerNumber2.layoutParams.width != numberWidthPx) {
			holder.spacerNumber2.layoutParams =
				holder.spacerNumber2.layoutParams.apply { width = numberWidthPx }
		}

		// 절 번호: 구절 메모 있으면 밑줄
		val verseMemo = verseMemos[verseItem.verse]
		if (verseMemo != null) {
			val numberSpan = SpannableString(verseItem.verse.toString())
			numberSpan.setSpan(
				UnderlineSpan(),
				0,
				numberSpan.length,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			holder.number.text = numberSpan
		} else {
			holder.number.text = verseItem.verse.toString()
		}

		holder.content.textSize = fontSize.toFloat()
		holder.content2.textSize = fontSize.toFloat()

		val highlightsSeg0 = highlightsByVerse[verseItem.verse].orEmpty().filter { it.segment == 0 }
		val highlightsSeg1 = highlightsByVerse[verseItem.verse].orEmpty().filter { it.segment == 1 }
		val wordMemosSeg0 = wordMemosByVerse[verseItem.verse].orEmpty().filter { it.segment == 0 }
		val wordMemosSeg1 = wordMemosByVerse[verseItem.verse].orEmpty().filter { it.segment == 1 }

		val title2 = verseItem.title2
		val text2 = verseItem.text2

		if (!title2.isNullOrBlank()) {
			// 실제로 소제목이 있는 절 → 소제목을 사이에 두고 두 블록으로 나눠서 보여준다.
			holder.content.text =
				buildAnnotatedSpannable(context, verseItem.text, highlightsSeg0, wordMemosSeg0)
			bindInteractiveText(
				holder,
				holder.content,
				verseItem,
				segment = 0,
				wordMemosOfSegment = wordMemosSeg0
			)

			holder.midTitle.text = "<$title2>"
			holder.midTitle.visibility = View.VISIBLE
			holder.contentRow2.visibility = View.VISIBLE
			holder.content2.text =
				buildAnnotatedSpannable(context, text2 ?: "", highlightsSeg1, wordMemosSeg1)
			bindInteractiveText(
				holder,
				holder.content2,
				verseItem,
				segment = 1,
				wordMemosOfSegment = wordMemosSeg1
			)
		} else {
			// 소제목이 없으면(그 번역본엔 이 절에 소제목이 없는 경우) 절대 나누지 않고 한 덩어리로 보여준다.
			holder.midTitle.visibility = View.GONE
			holder.contentRow2.visibility = View.GONE

			if (!text2.isNullOrBlank()) {
				// text2 데이터 자체는 있지만(다른 번역본엔 소제목이 있어서 나뉜 자리) 이 번역본엔 소제목이
				// 없으므로, 이어붙여서 하나의 문장처럼 보여준다. 하이라이트/메모는 segment별로 저장돼
				// 있으므로 이어붙인 위치에 맞게 오프셋만 옮겨서 같은 SpannableString에 함께 적용한다.
				val boundary = verseItem.text.length + 1 // 띄어쓰기 한 칸 포함
				val combinedText = "${verseItem.text} $text2"
				val spannable = SpannableString(combinedText)
				applyAnnotations(context, spannable, highlightsSeg0, wordMemosSeg0, offset = 0)
				applyAnnotations(
					context,
					spannable,
					highlightsSeg1,
					wordMemosSeg1,
					offset = boundary
				)
				holder.content.text = spannable
				bindInteractiveTextCombined(
					holder,
					holder.content,
					verseItem,
					boundary,
					wordMemosSeg0 + wordMemosSeg1
				)
			} else {
				holder.content.text =
					buildAnnotatedSpannable(context, verseItem.text, highlightsSeg0, wordMemosSeg0)
				bindInteractiveText(
					holder,
					holder.content,
					verseItem,
					segment = 0,
					wordMemosOfSegment = wordMemosSeg0
				)
			}
		}

		val secondaryText = secondaryTextByVerse?.get(verseItem.verse)
		if (secondaryText != null) {
			// 함께보기는 주성경이 실제로 나뉘어 있을 때만 같이 나눈다(주성경이 안 나뉘면 함께보기도
			// 한 문단으로). 소제목 자체는 함께보기에 절대 안 보여준다.
			if (!title2.isNullOrBlank()) {
				holder.secondaryContent.text = secondaryText.text
				holder.secondaryContent.textSize = (fontSize - 1).toFloat()
				holder.secondaryContent.visibility = View.VISIBLE

				if (!secondaryText.text2.isNullOrBlank()) {
					holder.secondaryContent2.text = secondaryText.text2
					holder.secondaryContent2.textSize = (fontSize - 1).toFloat()
					holder.secondaryContent2.visibility = View.VISIBLE
				} else {
					holder.secondaryContent2.visibility = View.GONE
				}
			} else {
				holder.secondaryContent.text = secondaryText.fullText
				holder.secondaryContent.textSize = (fontSize - 1).toFloat()
				holder.secondaryContent.visibility = View.VISIBLE
				holder.secondaryContent2.visibility = View.GONE
			}
		} else {
			holder.secondaryContent.visibility = View.GONE
			holder.secondaryContent2.visibility = View.GONE
		}

		holder.root.setBackgroundColor(
			if (isSelected) ContextCompat.getColor(
				context,
				R.color.verse_selected_bg
			) else Color.TRANSPARENT
		)

		val tapListener = View.OnClickListener { onVerseTap(verseItem.verse) }
		holder.root.setOnClickListener(tapListener)

		// 절 번호: 메모 있으면 메모 보기, 없으면 기존처럼 선택 토글
		holder.number.setOnClickListener {
			val memo = verseMemos[verseItem.verse]
			if (memo != null) {
				onVerseMemoView(verseItem.verse, memo)
			} else {
				onVerseTap(verseItem.verse)
			}
		}
	}

	/** 하이라이트 배경 + 단어 메모 밑줄까지 입힌 SpannableString을 만든다. */
	private fun buildAnnotatedSpannable(
		context: android.content.Context,
		text: String,
		highlights: List<PartialHighlight>,
		wordMemos: List<WordMemo>
	): SpannableString {
		val spannable = SpannableString(text)
		applyAnnotations(context, spannable, highlights, wordMemos, offset = 0)
		return spannable
	}

	/**
	 * 하이라이트/단어메모 스타일을 spannable에 입힌다. offset은 절이 이어붙여진 경우(소제목 없이 text+text2를
	 * 한 문장으로 보여줄 때) segment 1의 실제 글자가 시작하는 위치를 알려주기 위한 것.
	 */
	private fun applyAnnotations(
		context: android.content.Context,
		spannable: SpannableString,
		highlights: List<PartialHighlight>,
		wordMemos: List<WordMemo>,
		offset: Int
	) {
		for (h in highlights) {
			val start = (h.startOffset + offset).coerceIn(0, spannable.length)
			val end = (h.endOffset + offset).coerceIn(start, spannable.length)
			if (start < end) {
				val bgColor = try {
					Color.parseColor(h.colorHex)
				} catch (e: Exception) {
					Color.YELLOW
				}
				val fgColor = HighlightColors.contrastTextColor(h.colorHex)
				spannable.setSpan(
					BackgroundColorSpan(bgColor),
					start,
					end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				spannable.setSpan(
					ForegroundColorSpan(fgColor),
					start,
					end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
		}
		for (m in wordMemos) {
			val start = (m.startOffset + offset).coerceIn(0, spannable.length)
			val end = (m.endOffset + offset).coerceIn(start, spannable.length)
			if (start < end) {
				spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
		}
	}

	/** 탭으로 절 선택, 드래그로 부분 선택 후 하이라이트/메모 팝업까지 — content/content2 둘 다 동일하게 쓴다. */
	private fun bindInteractiveText(
		holder: ViewHolder,
		textView: TextView,
		verseItem: BibleVerse,
		segment: Int,
		wordMemosOfSegment: List<WordMemo>
	) {
		val context = textView.context
		var downTime = 0L
		var downX = 0f
		var downY = 0f
		val holdHandler = android.os.Handler(android.os.Looper.getMainLooper())
		var holdRunnable: Runnable? = null
		var holdStarted = false

		textView.setOnTouchListener { _, event ->
			when (event.actionMasked) {
				android.view.MotionEvent.ACTION_DOWN -> {
					downTime = System.currentTimeMillis()
					downX = event.x
					downY = event.y
					holdStarted = false
					// 150ms 동안 손가락이 거의 안 움직이면, 롱프레스로 이어질 가능성이 크다고 보고
					// 그 사이에 ViewPager2가 스와이프로 가로채지 못하게 미리 막아둔다.
					holdRunnable = Runnable {
						holdStarted = true
						onGestureHoldStart?.invoke()
					}
					holdHandler.postDelayed(holdRunnable!!, 150)
				}

				android.view.MotionEvent.ACTION_MOVE -> {
					val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
					val dx = kotlin.math.abs(event.x - downX)
					val dy = kotlin.math.abs(event.y - downY)
					if (dx > touchSlop || dy > touchSlop) {
						// 실제로 움직이기 시작했으면(스크롤/스와이프 의도) 롱프레스 예약을 취소한다.
						holdRunnable?.let { holdHandler.removeCallbacks(it) }
						if (holdStarted) {
							holdStarted = false
							onGestureHoldEnd?.invoke()
						}
					}
				}

				android.view.MotionEvent.ACTION_UP -> {
					holdRunnable?.let { holdHandler.removeCallbacks(it) }
					if (holdStarted) {
						holdStarted = false
						onGestureHoldEnd?.invoke()
					}
					val elapsed = System.currentTimeMillis() - downTime
					val dx = kotlin.math.abs(event.x - downX)
					val dy = kotlin.math.abs(event.y - downY)
					val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
					if (elapsed < 200 && dx < touchSlop && dy < touchSlop) {
						onVerseTap(verseItem.verse)
					}
				}

				android.view.MotionEvent.ACTION_CANCEL -> {
					holdRunnable?.let { holdHandler.removeCallbacks(it) }
					if (holdStarted) {
						holdStarted = false
						onGestureHoldEnd?.invoke()
					}
				}
			}
			false // 롱프레스로 텍스트 선택하는 기존 동작은 그대로 유지
		}

		textView.setTextIsSelectable(true)
		textView.customSelectionActionModeCallback = object : ActionMode.Callback {
			override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
				menu?.add(0, ID_HIGHLIGHT, 0, "하이라이트")
				menu?.add(0, ID_MEMO, 1, "메모")
				return true
			}

			override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

			override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
				val start = textView.selectionStart
				val end = textView.selectionEnd
				if (start !in 0 until end) return false

				when (item?.itemId) {
					ID_HIGHLIGHT -> {
						onHighlightRequested(verseItem.verse, start, end, segment)
						mode?.finish()
						return true
					}

					ID_MEMO -> {
						val overlapping = wordMemosOfSegment.firstOrNull { m ->
							!(end <= m.startOffset || start >= m.endOffset)
						}
						if (overlapping != null) {
							onWordMemoView(verseItem.verse, overlapping)
						} else {
							onWordMemoCreate(verseItem.verse, start, end, segment)
						}
						mode?.finish()
						return true
					}
				}
				return false
			}

			override fun onDestroyActionMode(mode: ActionMode?) {}
		}
	}

	/**
	 * bindInteractiveText와 거의 같지만, 화면엔 한 덩어리로 보여주면서 실제로는 text(segment 0)와
	 * text2(segment 1)가 이어붙어 있는 경우용. 선택한 범위가 이어붙인 경계(boundary) 앞이면 segment 0,
	 * 뒤면 segment 1로 판단해서 원래 글자 기준 오프셋으로 되돌려 저장한다.
	 */
	private fun bindInteractiveTextCombined(
		holder: ViewHolder,
		textView: TextView,
		verseItem: BibleVerse,
		boundary: Int,
		wordMemosOfBothSegments: List<WordMemo>
	) {
		val context = textView.context
		var downTime = 0L
		var downX = 0f
		var downY = 0f
		val holdHandler = android.os.Handler(android.os.Looper.getMainLooper())
		var holdRunnable: Runnable? = null
		var holdStarted = false

		textView.setOnTouchListener { _, event ->
			when (event.actionMasked) {
				android.view.MotionEvent.ACTION_DOWN -> {
					downTime = System.currentTimeMillis()
					downX = event.x
					downY = event.y
					holdStarted = false
					holdRunnable = Runnable {
						holdStarted = true
						onGestureHoldStart?.invoke()
					}
					holdHandler.postDelayed(holdRunnable!!, 150)
				}

				android.view.MotionEvent.ACTION_MOVE -> {
					val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
					val dx = kotlin.math.abs(event.x - downX)
					val dy = kotlin.math.abs(event.y - downY)
					if (dx > touchSlop || dy > touchSlop) {
						holdRunnable?.let { holdHandler.removeCallbacks(it) }
						if (holdStarted) {
							holdStarted = false
							onGestureHoldEnd?.invoke()
						}
					}
				}

				android.view.MotionEvent.ACTION_UP -> {
					holdRunnable?.let { holdHandler.removeCallbacks(it) }
					if (holdStarted) {
						holdStarted = false
						onGestureHoldEnd?.invoke()
					}
					val elapsed = System.currentTimeMillis() - downTime
					val dx = kotlin.math.abs(event.x - downX)
					val dy = kotlin.math.abs(event.y - downY)
					val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
					if (elapsed < 200 && dx < touchSlop && dy < touchSlop) {
						onVerseTap(verseItem.verse)
					}
				}

				android.view.MotionEvent.ACTION_CANCEL -> {
					holdRunnable?.let { holdHandler.removeCallbacks(it) }
					if (holdStarted) {
						holdStarted = false
						onGestureHoldEnd?.invoke()
					}
				}
			}
			false
		}

		textView.setTextIsSelectable(true)
		textView.customSelectionActionModeCallback = object : ActionMode.Callback {
			override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
				menu?.add(0, ID_HIGHLIGHT, 0, "하이라이트")
				menu?.add(0, ID_MEMO, 1, "메모")
				return true
			}

			override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

			override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
				val rawStart = textView.selectionStart
				val rawEnd = textView.selectionEnd
				if (rawStart !in 0 until rawEnd) return false

				// 경계를 넘나드는 선택은 거의 없겠지만, 넘으면 앞부분(segment 0) 기준으로 자른다.
				val segment: Int
				val start: Int
				val end: Int
				if (rawStart < boundary) {
					segment = 0
					start = rawStart
					end = minOf(rawEnd, boundary - 1)
				} else {
					segment = 1
					start = rawStart - boundary
					end = rawEnd - boundary
				}
				if (start !in 0 until end) return false

				when (item?.itemId) {
					ID_HIGHLIGHT -> {
						onHighlightRequested(verseItem.verse, start, end, segment)
						mode?.finish()
						return true
					}

					ID_MEMO -> {
						val overlapping = wordMemosOfBothSegments.firstOrNull { m ->
							m.segment == segment && !(end <= m.startOffset || start >= m.endOffset)
						}
						if (overlapping != null) {
							onWordMemoView(verseItem.verse, overlapping)
						} else {
							onWordMemoCreate(verseItem.verse, start, end, segment)
						}
						mode?.finish()
						return true
					}
				}
				return false
			}

			override fun onDestroyActionMode(mode: ActionMode?) {}
		}
	}

	override fun getItemCount() = verses.size

	fun updateBookmarks(newBookmarks: MutableMap<Int, BibleBookmark>) {
		bookmarks = newBookmarks
		notifyDataSetChanged()
	}

	fun updateFontSize(newSize: Int) {
		fontSize = newSize
		notifyDataSetChanged()
	}

	fun updateSelection(newSelection: Set<Int>) {
		selectedVerses = newSelection
		notifyDataSetChanged()
	}

	fun updateHighlights(newHighlights: Map<Int, List<PartialHighlight>>) {
		highlightsByVerse = newHighlights
		notifyDataSetChanged()
	}

	fun updateMemos(newVerseMemos: Map<Int, VerseMemo>, newWordMemos: Map<Int, List<WordMemo>>) {
		verseMemos = newVerseMemos
		wordMemosByVerse = newWordMemos
		notifyDataSetChanged()
	}
}