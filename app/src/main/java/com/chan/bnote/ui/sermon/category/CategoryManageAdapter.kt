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

		// 미분류는 실제 카테고리가 아니라서 수정/삭제 대상은 아니지만, 순서는 같이 옮길 수 있다.
		val canManage = isEditMode && row.category != null
		holder.editBtn.visibility = if (canManage) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (canManage) View.VISIBLE else View.GONE
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

		holder.itemView.setOnClickListener {
			if (isEditMode) return@setOnClickListener
			onClick(row.category)
		}
		holder.editBtn.setOnClickListener { row.category?.let(onEdit) }
		holder.deleteBtn.setOnClickListener { row.category?.let(onDelete) }

		// ≡ 손잡이를 누르는 순간 바로 드래그가 시작되게 한다(길게 누를 필요 없이).
		holder.dragHandle.setOnTouchListener { _, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN && isEditMode) {
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

	fun moveItem(from: Int, to: Int) {
		val item = rows.removeAt(from)
		rows.add(to, item)
		notifyItemMoved(from, to)
	}

	/** 드래그가 끝난 뒤, 지금 순서 그대로 각 카테고리에 매길 sortOrder 값을 돌려준다(미분류 제외 —
	 * 미분류는 실제 카테고리 테이블 행이 아니라서 별도로 위치만 기억해둔다). */
	fun currentCategoryOrder(): List<SermonCategory> =
		rows.mapNotNull { it.category }

	/** 지금 순서에서 미분류가 몇 번째에 있는지(0부터). 미분류 위치도 기억해뒀다가 다음에 열 때 그대로
	 * 보여주기 위한 것. */
	fun currentUncategorizedPosition(): Int = rows.indexOfFirst { it.category == null }
}