package com.chan.bnote.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleVerse

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private val secondaryTextByVerse: Map<Int, String>?,
	private var bookmarks: MutableMap<Int, BibleBookmark>,
	private var fontSize: Int,
	private val onLongPress: (verse: Int, current: BibleBookmark?) -> Unit
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

		holder.root.setBackgroundColor(
			if (bookmark?.isHighlighted == true)
				androidx.core.content.ContextCompat.getColor(
					holder.itemView.context,
					R.color.highlight_yellow
				)
			else android.graphics.Color.TRANSPARENT
		)

		holder.root.setOnLongClickListener {
			onLongPress(verseItem.verse, bookmarks[verseItem.verse])
			true
		}
	}

	override fun getItemCount(): Int = verses.size

	fun updateBookmarks(newBookmarks: MutableMap<Int, BibleBookmark>) {
		bookmarks = newBookmarks
		notifyDataSetChanged()
	}

	fun updateFontSize(newSize: Int) {
		fontSize = newSize
		notifyDataSetChanged()
	}
}