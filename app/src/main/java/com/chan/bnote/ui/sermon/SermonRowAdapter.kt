package com.chan.bnote.ui.sermon

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.sermon.Sermon

data class SermonRowData(
	val sermon: Sermon,
	val colorHex: String?,
	val dateLabel: String,
	val bibleRefLabel: String
)

class SermonRowAdapter(
	private val rows: List<SermonRowData>,
	private val onClick: (Sermon) -> Unit
) : RecyclerView.Adapter<SermonRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val colorBar: View = view.findViewById(R.id.color_bar)
		val title: TextView = view.findViewById(R.id.text_sermon_title)
		val date: TextView = view.findViewById(R.id.text_sermon_date)
		val bibleRef: TextView = view.findViewById(R.id.text_sermon_bible_ref_short)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_sermon_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.title.text = row.sermon.title
		holder.date.text = row.dateLabel
		holder.bibleRef.text = row.bibleRefLabel
		holder.colorBar.setBackgroundColor(
			try {
				Color.parseColor(row.colorHex ?: "#E0E0E0")
			} catch (e: Exception) {
				Color.LTGRAY
			}
		)
		holder.itemView.setOnClickListener { onClick(row.sermon) }
	}

	override fun getItemCount() = rows.size
}