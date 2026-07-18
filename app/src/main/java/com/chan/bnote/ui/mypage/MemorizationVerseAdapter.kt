package com.chan.bnote.ui.mypage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.mypage.MemorizationVerse

class MemorizationVerseAdapter(
	private val items: List<MemorizationVerse>,
	private val onClick: (MemorizationVerse) -> Unit
) : RecyclerView.Adapter<MemorizationVerseAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val ref: TextView = view.findViewById(R.id.text_verse_ref)
		val preview: TextView = view.findViewById(R.id.text_verse_preview)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_memorization_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = items[position]
		holder.ref.text = item.toDisplayLabel()
		holder.preview.text = item.verseText
		holder.itemView.setOnClickListener { onClick(item) }
	}

	override fun getItemCount() = items.size
}