package com.chan.bnote.ui.sermon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.sermon.Preacher

data class PreacherRow(val preacher: Preacher, val count: Int)

class PreacherManageAdapter(
	initialRows: List<PreacherRow>,
	private var isEditMode: Boolean,
	private val onClick: (Preacher) -> Unit,
	private val onEdit: (Preacher) -> Unit,
	private val onDelete: (Preacher) -> Unit
) : RecyclerView.Adapter<PreacherManageAdapter.ViewHolder>() {

	// 드래그로 순서를 바꿀 때 어댑터를 통째로 새로 만들지 않고 이 리스트만 직접 움직인다
	// (재생성하면 드래그 도중 ItemTouchHelper 상태가 꼬여서 튕기는 문제가 있었음).
	private val rows = initialRows.toMutableList()

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_preacher_name)
		val count: TextView = view.findViewById(R.id.text_preacher_count)
		val editBtn: ImageView = view.findViewById(R.id.btn_edit_preacher)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_preacher)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_preacher_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.name.text = row.preacher.name
		holder.count.text = "${row.count}개"

		holder.editBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.editBtn.setOnClickListener { onEdit(row.preacher) }
		holder.deleteBtn.setOnClickListener { onDelete(row.preacher) }

		holder.itemView.setOnClickListener {
			if (!isEditMode) onClick(row.preacher)
		}
	}

	override fun getItemCount() = rows.size

	fun setEditMode(enabled: Boolean) {
		isEditMode = enabled
		notifyDataSetChanged()
	}

	/** 드래그 도중 호출됨. 리스트 순서만 바꾸고 애니메이션으로 이동만 알려준다 (전체 재로딩 없음). */
	fun moveItem(from: Int, to: Int) {
		if (from !in rows.indices || to !in rows.indices) return
		val item = rows.removeAt(from)
		rows.add(to, item)
		notifyItemMoved(from, to)
	}

	fun currentOrderIds(): List<Long> = rows.map { it.preacher.id }
}