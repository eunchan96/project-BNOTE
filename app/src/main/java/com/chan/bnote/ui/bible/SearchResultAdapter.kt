package com.chan.bnote.ui.bible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleVerse

class SearchResultAdapter(
	private val results: List<BibleVerse>,
	private val fontSize: Int,
	private val onClick: (BibleVerse) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val ref: TextView = view.findViewById(R.id.text_result_ref)
		val content: TextView = view.findViewById(R.id.text_result_content)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verse = results[position]
		holder.ref.text = "${BibleBooks.shortNameOf(verse.bookId)} ${verse.chapter}:${verse.verse}"
		holder.content.text = verse.text
		holder.content.textSize = fontSize.toFloat()
		holder.itemView.setOnClickListener { onClick(verse) }
	}

	override fun getItemCount() = results.size
}