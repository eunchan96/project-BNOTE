package com.chan.bnote.ui

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
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
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleVerse
import com.chan.bnote.data.PartialHighlight

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private val secondaryTextByVerse: Map<Int, String>?,
	private var bookmarks: MutableMap<Int, BibleBookmark>,
	private var fontSize: Int,
	private var selectedVerses: Set<Int>,
	private var highlightsByVerse: Map<Int, List<PartialHighlight>>,
	private val onVerseTap: (verse: Int) -> Unit,
	private val onHighlightDefault: (verse: Int, start: Int, end: Int) -> Unit,
	private val onHighlightColorPick: (verse: Int, start: Int, end: Int) -> Unit
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	companion object {
		private const val ID_HIGHLIGHT = 9001
		private const val ID_HIGHLIGHT_COLOR = 9002
	}

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val root: View = view.findViewById(R.id.item_root)
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

		holder.number.text = verseItem.verse.toString()
		holder.content.textSize = fontSize.toFloat()

		// 부분 하이라이트 적용 (주성경 텍스트에만)
		val spannable = SpannableString(verseItem.text)
		for (h in highlightsByVerse[verseItem.verse].orEmpty()) {
			val start = h.startOffset.coerceIn(0, spannable.length)
			val end = h.endOffset.coerceIn(start, spannable.length)
			if (start < end) {
				val color = try {
					Color.parseColor(h.colorHex)
				} catch (e: Exception) {
					Color.YELLOW
				}
				spannable.setSpan(
					BackgroundColorSpan(color),
					start,
					end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
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

		// 탭 = 절 선택 모드 (번호/본문 어디를 눌러도 동작)
		val tapListener = View.OnClickListener { onVerseTap(verseItem.verse) }
		holder.root.setOnClickListener(tapListener)
		holder.number.setOnClickListener(tapListener)
		holder.content.setOnClickListener(tapListener)

		// 롱프레스 + 드래그 = 부분 텍스트 선택 (시스템 기본 복사 + 커스텀 하이라이트)
		holder.content.setTextIsSelectable(true)
		holder.content.customSelectionActionModeCallback = object : ActionMode.Callback {
			override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
				menu?.add(0, ID_HIGHLIGHT, 0, "하이라이트")
				menu?.add(0, ID_HIGHLIGHT_COLOR, 1, "하이라이트 색상")
				return true
			}

			override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

			override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
				val start = holder.content.selectionStart
				val end = holder.content.selectionEnd
				if (start !in 0 until end) return false

				when (item?.itemId) {
					ID_HIGHLIGHT -> {
						onHighlightDefault(verseItem.verse, start, end)
						mode?.finish()
						return true
					}

					ID_HIGHLIGHT_COLOR -> {
						onHighlightColorPick(verseItem.verse, start, end)
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
}