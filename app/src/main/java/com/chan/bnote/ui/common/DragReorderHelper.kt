package com.chan.bnote.ui.common

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragReorderHelper(
	private val onMove: (from: Int, to: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
	ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		onMove(viewHolder.adapterPosition, target.adapterPosition)
		return true
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}