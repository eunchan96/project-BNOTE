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
import com.chan.bnote.data.bookmark.BibleBookmark
import com.chan.bnote.data.memo.VerseMemo
import com.chan.bnote.data.memo.WordMemo
import com.chan.bnote.data.partialhighlight.PartialHighlight
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
	private val onHighlightRequested: (verse: Int, start: Int, end: Int) -> Unit,
	private val onWordMemoCreate: (verse: Int, start: Int, end: Int) -> Unit,
	private val onWordMemoView: (verse: Int, memo: WordMemo) -> Unit
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	companion object {
		private const val ID_HIGHLIGHT = 9001
		private const val ID_MEMO = 9002
	}

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val root: View = view.findViewById(R.id.item_root)
		val contentRow: View = view.findViewById(R.id.content_row)
		val title: TextView = view.findViewById(R.id.text_verse_title)
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val secondaryContent: TextView = view.findViewById(R.id.text_verse_content_secondary)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verseItem = verses[position]
		val isSelected = selectedVerses.contains(verseItem.verse)
		val context = holder.itemView.context

		val titleText = verseItem.title
		if (!titleText.isNullOrBlank()) {
			holder.title.text = "<$titleText>"
			holder.title.visibility = View.VISIBLE
		} else {
			holder.title.visibility = View.GONE
		}

		val extraTopSpacing = position == 0 && titleText.isNullOrBlank()
		val topPaddingDp = if (extraTopSpacing) 16 else 4
		val topPaddingPx = (topPaddingDp * context.resources.displayMetrics.density).toInt()
		holder.contentRow.setPadding(
			holder.contentRow.paddingLeft, topPaddingPx,
			holder.contentRow.paddingRight, holder.contentRow.paddingBottom
		)

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

		val spannable = SpannableString(verseItem.text)
		for (h in highlightsByVerse[verseItem.verse].orEmpty()) {
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
		for (m in wordMemosByVerse[verseItem.verse].orEmpty()) {
			val start = m.startOffset.coerceIn(0, spannable.length)
			val end = m.endOffset.coerceIn(start, spannable.length)
			if (start < end) {
				spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
		}
		holder.content.text = spannable

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
		holder.content.setOnClickListener(tapListener)

		// 절 번호: 메모 있으면 메모 보기, 없으면 기존처럼 선택 토글
		holder.number.setOnClickListener {
			val memo = verseMemos[verseItem.verse]
			if (memo != null) {
				onVerseMemoView(verseItem.verse, memo)
			} else {
				onVerseTap(verseItem.verse)
			}
		}

		holder.content.setTextIsSelectable(true)
		holder.content.customSelectionActionModeCallback = object : ActionMode.Callback {
			override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
				menu?.add(0, ID_HIGHLIGHT, 0, "하이라이트")
				menu?.add(0, ID_MEMO, 1, "메모")
				return true
			}

			override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

			override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
				val start = holder.content.selectionStart
				val end = holder.content.selectionEnd
				if (start !in 0 until end) return false

				when (item?.itemId) {
					ID_HIGHLIGHT -> {
						onHighlightRequested(verseItem.verse, start, end)
						mode?.finish()
						return true
					}

					ID_MEMO -> {
						val overlapping = wordMemosByVerse[verseItem.verse]?.firstOrNull { m ->
							!(end <= m.startOffset || start >= m.endOffset)
						}
						if (overlapping != null) {
							onWordMemoView(verseItem.verse, overlapping)
						} else {
							onWordMemoCreate(verseItem.verse, start, end)
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