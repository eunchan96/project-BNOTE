package com.chan.bnote.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleVerse

class VerseAdapter(private val verses: List<BibleVerse>) :
	RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

	class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verse = verses[position]
		holder.number.text = verse.verse.toString()
		holder.content.text = verse.text
	}

	override fun getItemCount(): Int = verses.size
}