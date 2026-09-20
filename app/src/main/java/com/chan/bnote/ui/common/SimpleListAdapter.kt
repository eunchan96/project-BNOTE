package com.chan.bnote.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

class SimpleListAdapter(
	private val items: List<String>,
	private val selectedIndex: Int = -1,
	private val onClick: (Int) -> Unit // 클릭된 position 전달
) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

	class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
		val textView: TextView = view.findViewById(R.id.text_item)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_simple_text, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.textView.text = items[position]
		val isSelected = position == selectedIndex
		holder.textView.setTextColor(
			androidx.core.content.ContextCompat.getColor(
				holder.itemView.context,
				if (isSelected) R.color.brown_primary else R.color.text_primary
			)
		)
		holder.textView.setTypeface(
			null,
			if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
		)
		holder.itemView.setOnClickListener { onClick(position) }
	}

	override fun getItemCount(): Int = items.size
}