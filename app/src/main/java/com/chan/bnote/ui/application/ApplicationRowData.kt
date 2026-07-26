package com.chan.bnote.ui.application

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.application.Application

data class ApplicationRowData(
	val application: Application,
	val colorHex: String?,
	val rightTopLabel: String,
	// 추가한 본문이 있으면 그걸, 없고 연결한 설교가 있으면 그 설교 제목들을 보여준다.
	val rightBottomLabel: String
)

class ApplicationRowAdapter(
	private val rows: List<ApplicationRowData>,
	private val onClick: (Application) -> Unit
) : RecyclerView.Adapter<ApplicationRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val colorBar: View = view.findViewById(R.id.color_bar)
		val title: TextView = view.findViewById(R.id.text_application_title)
		val rightTop: TextView = view.findViewById(R.id.text_application_date)
		val rightBottom: TextView = view.findViewById(R.id.text_application_ref_short)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context)
				.inflate(R.layout.item_application_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.title.text = row.application.title
		holder.rightTop.text = row.rightTopLabel
		holder.rightBottom.text = row.rightBottomLabel
		holder.colorBar.setBackgroundColor(
			row.colorHex?.let {
				try {
					Color.parseColor(it)
				} catch (e: Exception) {
					null
				}
			} ?: androidx.core.content.ContextCompat.getColor(
				holder.itemView.context,
				R.color.category_none
			)
		)
		holder.itemView.setOnClickListener { onClick(row.application) }
	}

	override fun getItemCount() = rows.size
}