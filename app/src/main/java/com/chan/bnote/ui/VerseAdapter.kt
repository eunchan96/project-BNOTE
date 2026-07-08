package com.chan.bnote.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBookmark
import com.chan.bnote.data.BibleVerse

class VerseAdapter(
	private val verses: List<BibleVerse>,
	private var bookmarks: MutableMap<Int, BibleBookmark>, // key: verse number
	private val onToggleHighlight: (verse: Int, current: BibleBookmark?) -> Unit,
	private val onToggleFavorite: (verse: Int, current: BibleBookmark?) -> Unit
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
		val root: android.view.View = view.findViewById(R.id.item_root)
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val star: TextView = view.findViewById(R.id.text_favorite_star)
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

		// 하이라이트 배경
		holder.root.setBackgroundColor(
			if (bookmark?.isHighlighted == true) Color.parseColor("#FFF9C4") else Color.TRANSPARENT
		)
		// 즐겨찾기 별 색
		holder.star.setTextColor(
			if (bookmark?.isFavorite == true) Color.parseColor("#FFC107") else Color.parseColor("#CCCCCC")
		)

		holder.root.setOnClickListener {
			onToggleHighlight(verseItem.verse, bookmarks[verseItem.verse])
		}
		holder.root.setOnLongClickListener {
			onToggleFavorite(verseItem.verse, bookmarks[verseItem.verse])
			true
		}
	}

	override fun getItemCount(): Int = verses.size

	fun updateBookmarks(newBookmarks: MutableMap<Int, BibleBookmark>) {
		bookmarks = newBookmarks
		notifyDataSetChanged()
	}
}