package com.chan.bnote.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleVerse

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private val secondaryTextByVerse: Map<Int, String>?,
	private var bookmarks: MutableMap<Int, BibleBookmark>,
	private var fontSize: Int,
	private var selectedVerses: Set<Int>,
	private val onVerseTap: (verse: Int) -> Unit
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val root: View = view.findViewById(R.id.item_root)
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val secondaryContent: TextView = view.findViewById(R.id.text_verse_content_secondary)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verseItem = verses[position]
		val bookmark = bookmarks[verseItem.verse]
		val isSelected = selectedVerses.contains(verseItem.verse)

		holder.number.text = verseItem.verse.toString()
		holder.content.text = verseItem.text
		holder.content.textSize = fontSize.toFloat()

		val secondaryText = secondaryTextByVerse?.get(verseItem.verse)
		if (secondaryText != null) {
			holder.secondaryContent.text = secondaryText
			holder.secondaryContent.textSize = (fontSize - 1).toFloat()
			holder.secondaryContent.visibility = View.VISIBLE
		} else {
			holder.secondaryContent.visibility = View.GONE
		}

		val context = holder.itemView.context
		holder.root.setBackgroundColor(
			when {
				isSelected -> ContextCompat.getColor(context, R.color.verse_selected_bg)
				bookmark?.isHighlighted == true -> ContextCompat.getColor(
					context,
					R.color.highlight_yellow
				)

				else -> Color.TRANSPARENT
			}
		)

		holder.root.setOnClickListener { onVerseTap(verseItem.verse) }
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
}