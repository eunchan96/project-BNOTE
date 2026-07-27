package com.chan.bnote.ui.application.category

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.application.ApplicationCategory

class ApplicationCategoryManageAdapter(
	initialCategories: List<ApplicationCategory>,
	private var isEditMode: Boolean,
	private val onEdit: (ApplicationCategory) -> Unit,
	private val onDelete: (ApplicationCategory) -> Unit,
	private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
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

	@SuppressLint("ClickableViewAccessibility")
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val category = categories[position]
		val context = holder.itemView.context

		val parsedColor = try {
			Color.parseColor(category.colorHex)
		} catch (e: Exception) {
			Color.GRAY
		}

		holder.name.text = category.name
		holder.count.visibility = View.GONE

		holder.editBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.dragHandle.visibility = if (isEditMode) View.VISIBLE else View.GONE

		// 평소엔 동그라미로 색을 보여주고, 관리 모드일 땐 동그라미 대신 ≡ 손잡이 자체를 그 색으로
		// 칠한다 — 동그라미까지 같이 있으면 이름이 옆으로 밀려서 답답해 보였다.
		if (isEditMode) {
			holder.dot.visibility = View.GONE
			holder.dragHandle.setTextColor(parsedColor)
		} else {
			holder.dragHandle.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
			holder.dot.visibility = View.VISIBLE
			val drawable = GradientDrawable()
			drawable.shape = GradientDrawable.OVAL
			drawable.setColor(parsedColor)
			holder.dot.background = drawable
		}

		holder.editBtn.setOnClickListener { onEdit(category) }
		holder.deleteBtn.setOnClickListener { onDelete(category) }

		holder.dragHandle.setOnTouchListener { _, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN && isEditMode) {
				onStartDrag(holder)
			}
			false
		}
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