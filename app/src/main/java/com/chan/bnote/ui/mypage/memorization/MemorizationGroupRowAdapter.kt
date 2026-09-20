package com.chan.bnote.ui.mypage.memorization

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.mypage.memorization.MemorizationGroup

data class MemorizationGroupRow(val group: MemorizationGroup, val count: Int)

/**
 * 암송 구절 그룹 목록. 평소엔 그룹을 눌러서 그 안의 구절을 보고, "관리"를 누르면(설교 카테고리
 * 관리처럼) 같은 화면에서 그대로 수정/삭제 아이콘이 나타난다 — 별도 화면으로 안 넘어간다.
 */
class MemorizationGroupRowAdapter(
	private val rows: List<MemorizationGroupRow>,
	private var isEditMode: Boolean,
	private val onClick: (MemorizationGroup) -> Unit,
	private val onEdit: (MemorizationGroup) -> Unit,
	private val onDelete: (MemorizationGroup) -> Unit
) : RecyclerView.Adapter<MemorizationGroupRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_group_row_name)
		val count: TextView = view.findViewById(R.id.text_group_row_count)
		val editBtn: ImageView = view.findViewById(R.id.btn_edit_group)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_group)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_memorization_group_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.name.text = row.group.name
		holder.count.visibility = if (isEditMode) View.GONE else View.VISIBLE
		holder.count.text = "${row.count}개"

		holder.editBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE

		holder.itemView.setOnClickListener {
			if (!isEditMode) onClick(row.group)
		}
		holder.editBtn.setOnClickListener { onEdit(row.group) }
		holder.deleteBtn.setOnClickListener { onDelete(row.group) }
	}

	override fun getItemCount() = rows.size

	fun setEditMode(editMode: Boolean) {
		isEditMode = editMode
		notifyDataSetChanged()
	}
}