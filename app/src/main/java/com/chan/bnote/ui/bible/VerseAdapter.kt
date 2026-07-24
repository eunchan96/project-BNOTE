package com.chan.bnote.ui.bible

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.ui.common.HighlightColors

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private val secondaryTextByVerse: Map<Int, String>?,
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
	private val onWordMemoView: (verse: Int, memo: WordMemo) -> Unit
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
		val title: TextView = view.findViewById(R.id.text_verse_title)
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val midTitle: TextView = view.findViewById(R.id.text_verse_mid_title)
		val content2: TextView = view.findViewById(R.id.text_verse_content2)
		val secondaryContent: TextView = view.findViewById(R.id.text_verse_content_secondary)
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

		// 절 번호 칸 너비를 장의 최대 절 번호 자릿수에 맞춘다.
		val numberWidthPx = (numberColumnWidthDp * context.resources.displayMetrics.density).toInt()
		if (holder.number.layoutParams.width != numberWidthPx) {
			holder.number.layoutParams = holder.number.layoutParams.apply { width = numberWidthPx }
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

		holder.content.text =
			buildAnnotatedSpannable(context, verseItem.text, highlightsSeg0, wordMemosSeg0)
		bindInteractiveText(
			holder,
			holder.content,
			verseItem,
			segment = 0,
			wordMemosOfSegment = wordMemosSeg0
		)

		// 아주 드물게, 절 본문이 소제목으로 둘로 쪼개지는 경우(예: 창 35:22)
		val title2 = verseItem.title2
		val text2 = verseItem.text2
		if (!title2.isNullOrBlank() && !text2.isNullOrBlank()) {
			holder.midTitle.text = "<$title2>"
			holder.midTitle.visibility = View.VISIBLE
			holder.content2.visibility = View.VISIBLE
			holder.content2.text =
				buildAnnotatedSpannable(context, text2, highlightsSeg1, wordMemosSeg1)
			bindInteractiveText(
				holder,
				holder.content2,
				verseItem,
				segment = 1,
				wordMemosOfSegment = wordMemosSeg1
			)
		} else {
			holder.midTitle.visibility = View.GONE
			holder.content2.visibility = View.GONE
		}

		val secondaryText = secondaryTextByVerse?.get(verseItem.verse)
		if (secondaryText != null) {
			holder.secondaryContent.text = secondaryText
			holder.secondaryContent.textSize = (fontSize - 1).toFloat()
			holder.secondaryContent.visibility = View.VISIBLE
		} else {
			holder.secondaryContent.visibility = View.GONE
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

	/** 하이라이트 배경 + 소제목 스타일 + 단어 메모 밑줄까지 입힌 SpannableString을 만든다. */
	private fun buildAnnotatedSpannable(
		context: android.content.Context,
		text: String,
		highlights: List<PartialHighlight>,
		wordMemos: List<WordMemo>
	): SpannableString {
		val spannable = SpannableString(text)

		// 아주 드물게 절 본문 중간에 <소제목>이 그대로 남아있는 경우(현재는 title2/text2로 분리해서
		// 안 쓰지만, 혹시 남아있을 수 있어 안전하게 계속 처리)를 위한 스타일링.
		for (match in Regex("<[^<>]+>").findAll(text)) {
			spannable.setSpan(
				StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			spannable.setSpan(
				ForegroundColorSpan(ContextCompat.getColor(context, R.color.brown_primary)),
				match.range.first, match.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}

		for (h in highlights) {
			val start = h.startOffset.coerceIn(0, spannable.length)
			val end = h.endOffset.coerceIn(start, spannable.length)
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
			val start = m.startOffset.coerceIn(0, spannable.length)
			val end = m.endOffset.coerceIn(start, spannable.length)
			if (start < end) {
				spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
		}
		return spannable
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
		textView.setOnTouchListener { _, event ->
			when (event.actionMasked) {
				android.view.MotionEvent.ACTION_DOWN -> {
					downTime = System.currentTimeMillis()
					downX = event.x
					downY = event.y
				}

				android.view.MotionEvent.ACTION_UP -> {
					val elapsed = System.currentTimeMillis() - downTime
					val dx = kotlin.math.abs(event.x - downX)
					val dy = kotlin.math.abs(event.y - downY)
					val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
					if (elapsed < 200 && dx < touchSlop && dy < touchSlop) {
						onVerseTap(verseItem.verse)
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