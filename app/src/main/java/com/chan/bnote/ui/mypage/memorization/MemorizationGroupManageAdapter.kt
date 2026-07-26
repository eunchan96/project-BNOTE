package com.chan.bnote.ui.mypage.memorization

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.mypage.memorization.MemorizationGroup

class MemorizationGroupManageAdapter(
	private val groups: List<MemorizationGroup>,
	private val onEdit: (MemorizationGroup) -> Unit,
	private val onDelete: (MemorizationGroup) -> Unit
) : RecyclerView.Adapter<MemorizationGroupManageAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_group_name)
		val editBtn: ImageView = view.findViewById(R.id.btn_edit_group)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_group)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_memorization_group_manage_row, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val group = groups[position]
		holder.name.text = group.name
		holder.editBtn.setOnClickListener { onEdit(group) }
		holder.deleteBtn.setOnClickListener { onDelete(group) }
	}

	override fun getItemCount() = groups.size
}