package com.chan.bnote.ui.mypage.gratitude

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.gratitude.GratitudeNote

data class GratitudeRowData(val note: GratitudeNote, val previewText: CharSequence)

class GratitudeRowAdapter(
	private val rows: List<GratitudeRowData>,
	private val onClick: (GratitudeNote) -> Unit
) : RecyclerView.Adapter<GratitudeRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val date: TextView = view.findViewById(R.id.text_gratitude_date)
		val preview: TextView = view.findViewById(R.id.text_gratitude_preview)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_gratitude_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.date.text = DateUtils.formatDate(row.note.date)
		holder.preview.text = row.previewText
		holder.itemView.setOnClickListener { onClick(row.note) }
	}

	override fun getItemCount() = rows.size
}