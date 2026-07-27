package com.chan.bnote.ui.sermon.category

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
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory

class CategoryManageAdapter(
	initialRows: List<CategoryManageActivity.CategoryRow>,
	private var isEditMode: Boolean,
	private val onClick: (SermonCategory?) -> Unit,
	private val onEdit: (SermonCategory) -> Unit,
	private val onDelete: (SermonCategory) -> Unit,
	private val onStartDrag: (RecyclerView.ViewHolder) -> Unit = {}
) : RecyclerView.Adapter<CategoryManageAdapter.ViewHolder>() {

	private val rows = initialRows.toMutableList()

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
		val row = rows[position]
		val context = holder.itemView.context

		val colorHex = row.category?.colorHex ?: String.format(
			"#%06X", 0xFFFFFF and ContextCompat.getColor(context, R.color.category_none)
		)
		val parsedColor = Color.parseColor(colorHex)

		holder.name.text = row.category?.name ?: "미분류"
		holder.count.visibility = if (isEditMode) View.GONE else View.VISIBLE
		holder.count.text = "${row.count}개"

		// 미분류는 실제 카테고리가 아니라서 수정/삭제/순서 이동 대상이 아니다.
		val canManage = isEditMode && row.category != null
		holder.editBtn.visibility = if (canManage) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (canManage) View.VISIBLE else View.GONE
		holder.dragHandle.visibility = if (canManage) View.VISIBLE else View.GONE

		// 평소엔 동그라미로 색을 보여주고, 관리 모드일 땐 동그라미 대신 ≡ 손잡이 자체를 그 색으로
		// 칠한다 — 동그라미까지 같이 있으면 이름이 옆으로 밀려서 답답해 보였다.
		if (canManage) {
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

		holder.itemView.setOnClickListener {
			if (isEditMode) return@setOnClickListener
			onClick(row.category)
		}
		holder.editBtn.setOnClickListener { row.category?.let(onEdit) }
		holder.deleteBtn.setOnClickListener { row.category?.let(onDelete) }

		// ≡ 손잡이를 누르는 순간 바로 드래그가 시작되게 한다(길게 누를 필요 없이).
		holder.dragHandle.setOnTouchListener { _, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN && canManage) {
				onStartDrag(holder)
			}
			false
		}
	}

	override fun getItemCount() = rows.size

	fun setEditMode(editMode: Boolean) {
		isEditMode = editMode
		notifyDataSetChanged()
	}

	/** 미분류(마지막 줄)는 순서 이동 대상이 아니라, 실제 카테고리끼리만 움직일 수 있게 막는다. */
	fun canMove(position: Int): Boolean = rows.getOrNull(position)?.category != null

	fun moveItem(from: Int, to: Int) {
		if (!canMove(from) || !canMove(to)) return
		val item = rows.removeAt(from)
		rows.add(to, item)
		notifyItemMoved(from, to)
	}

	/** 드래그가 끝난 뒤, 지금 순서 그대로 각 카테고리에 매길 sortOrder 값을 돌려준다(미분류 제외). */
	fun currentCategoryOrder(): List<SermonCategory> =
		rows.mapNotNull { it.category }
}