package com.chan.bnote.ui.scrap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.scrap.ScrapGroup

class ScrapGroupManageAdapter(
	private val groups: List<ScrapGroup>,
	private val onEdit: (ScrapGroup) -> Unit,
	private val onDelete: (ScrapGroup) -> Unit
) : RecyclerView.Adapter<ScrapGroupManageAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_group_name)
		val editBtn: TextView = view.findViewById(R.id.btn_edit_group)
		val deleteBtn: TextView = view.findViewById(R.id.btn_delete_group)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_scrap_group_header, parent, false)
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