package com.chan.bnote.ui.common

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragReorderHelper(
	private val onMove: (from: Int, to: Int) -> Unit,
	private val onDragFinished: () -> Unit = {}
) : ItemTouchHelper.SimpleCallback(
	ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
	// 롱프레스로는 아무 데서나 드래그가 시작되지 않게 막고, ≡ 손잡이를 터치한 순간 바로
	// startDrag()를 직접 호출해서 드래그가 시작되게 한다(아래 dragHandle 참고).
	override fun isLongPressDragEnabled(): Boolean = false

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		onMove(viewHolder.adapterPosition, target.adapterPosition)
		return true
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

	// 손을 떼서 드래그가 끝난 시점. 여기서만 저장하면 드래그 중 매 칸 이동마다 저장하지 않아도 된다.
	override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
		super.clearView(recyclerView, viewHolder)
		onDragFinished()
	}
}