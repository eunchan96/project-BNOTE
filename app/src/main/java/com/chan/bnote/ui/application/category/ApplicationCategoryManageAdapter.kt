package com.chan.bnote.ui.application.category

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.application.ApplicationCategory

class ApplicationCategoryManageAdapter(
	initialCategories: List<ApplicationCategory>,
	private var isEditMode: Boolean,
	private val onEdit: (ApplicationCategory) -> Unit,
	private val onDelete: (ApplicationCategory) -> Unit
) : RecyclerView.Adapter<ApplicationCategoryManageAdapter.ViewHolder>() {

	private val categories = initialCategories.toMutableList()

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dragHandle: TextView = view.findViewById(R.id.text_drag_handle)
		val dot: View = view.findViewById(R.id.color_dot)
		val name: TextView = view.findViewById(R.id.text_category_name)
		val count: TextView = view.findViewById(R.id.text_category_count)
		val editBtn: ImageView = view.findViewById(R.id.btn_edit_category)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_category)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val v = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_category_manage_row, parent, false)
		return ViewHolder(v)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val category = categories[position]

		val drawable = GradientDrawable()
		drawable.shape = GradientDrawable.OVAL
		drawable.setColor(
			try {
				Color.parseColor(category.colorHex)
			} catch (e: Exception) {
				Color.GRAY
			}
		)
		holder.dot.background = drawable
		holder.name.text = category.name
		holder.count.visibility = View.GONE

		holder.editBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.dragHandle.visibility = if (isEditMode) View.VISIBLE else View.GONE

		holder.editBtn.setOnClickListener { onEdit(category) }
		holder.deleteBtn.setOnClickListener { onDelete(category) }
	}

	override fun getItemCount() = categories.size

	fun setEditMode(editMode: Boolean) {
		isEditMode = editMode
		notifyDataSetChanged()
	}

	fun moveItem(from: Int, to: Int) {
		val item = categories.removeAt(from)
		categories.add(to, item)
		notifyItemMoved(from, to)
	}

	fun currentOrder(): List<ApplicationCategory> = categories.toList()
}