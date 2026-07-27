package com.chan.bnote.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

class GridNumberAdapter(
	private val items: List<Int>,
	// 선택된 값(위치가 아니라 실제 숫자)들을 갈색으로 표시한다. 여러 구절 시작/끝 절을 직접 탭해서
	// 고를 때(시작 절 하나만 눌렀을 때, 끝 절까지 두 개 다 눌렀을 때) 쓰기 위한 것.
	private var selectedItems: Set<Int> = emptySet(),
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
		val value = items[position]
		holder.textView.text = value.toString()

		val isSelected = value in selectedItems
		holder.textView.setBackgroundResource(
			if (isSelected) R.drawable.bg_book_button_selected else R.drawable.bg_book_button
		)
		holder.textView.setTextColor(
			ContextCompat.getColor(
				holder.itemView.context,
				if (isSelected) R.color.white else R.color.text_primary
			)
		)

		holder.itemView.setOnClickListener { onClick(position) }
	}

	override fun getItemCount(): Int = items.size

	fun updateSelection(newSelected: Set<Int>) {
		selectedItems = newSelected
		notifyDataSetChanged()
	}
}