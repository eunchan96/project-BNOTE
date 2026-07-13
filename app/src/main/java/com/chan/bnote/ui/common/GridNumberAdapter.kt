package com.chan.bnote.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

class GridNumberAdapter(
	private val items: List<Int>,
	private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<GridNumberAdapter.ViewHolder>() {

	class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
		val textView: TextView = view.findViewById(R.id.text_item)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_grid_number, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.textView.text = items[position].toString()
		holder.itemView.setOnClickListener { onClick(position) }
	}

	override fun getItemCount(): Int = items.size
}