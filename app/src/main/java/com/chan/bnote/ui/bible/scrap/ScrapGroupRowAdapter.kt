package com.chan.bnote.ui.bible.scrap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.scrap.ScrapGroup

data class ScrapGroupRow(val group: ScrapGroup, val count: Int)

class ScrapGroupRowAdapter(
	private val rows: List<ScrapGroupRow>,
	private val onClick: (ScrapGroup) -> Unit
) : RecyclerView.Adapter<ScrapGroupRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_group_row_name)
		val count: TextView = view.findViewById(R.id.text_group_row_count)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_scrap_group_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.name.text = row.group.name
		holder.count.text = "${row.count}개"
		holder.itemView.setOnClickListener { onClick(row.group) }
	}

	override fun getItemCount() = rows.size
}